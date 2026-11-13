package org.bensam.touristry.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BedBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.bensam.touristry.ModBlockEntities;
import org.bensam.touristry.ModComponents;
import org.bensam.touristry.block.AbstractExperienceBlock;
import org.bensam.touristry.entity.TouristEntity;
import org.bensam.touristry.entity.goal.GoalType;
import org.bensam.touristry.entity.goal.LodgingExperienceGoal;
import org.bensam.touristry.menu.LodgingExperienceMenu;
import org.bensam.touristry.tourism.experience.ExperienceTarget;
import org.bensam.touristry.tourism.experience.ExperienceVisit;
import org.jspecify.annotations.Nullable;

import java.util.*;

public class LodgingExperienceBlockEntity extends AbstractExperienceBlockEntity {
    public static final int IDEAL_TARGET_APPROACH_DISTANCE = 1; // Tourist should try to stand this far away for bed targets
    public static final int MAX_APPROACH_DISTANCE = 3; // Skip target if tourist can't get closer than this distance
    public static final int MAX_RANGE_TO_TARGET = 100;
    public static final int MIN_WAIT_AFTER_ARRIVAL_TICKS = 100;
    public static final int MAX_WAIT_AFTER_ARRIVAL_TICKS = 200;
    public static final int TICKS_AT_BLOCK_WHEN_PAYING = 100;
    public static final int PAYMENT_SLOT_SIZE = 9;

    private ItemStack defaultCost = ItemStack.EMPTY;

    public LodgingExperienceBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(ModBlockEntities.LODGING_EXPERIENCE.get(), blockPos, blockState, PAYMENT_SLOT_SIZE);

        this.orderedTargets = false;

        if (this.defaultCost.isEmpty()) {
            this.defaultCost = new ItemStack(Items.EMERALD);
        }
    }

    public void cancelReservation(TouristEntity touristEntity) {
        this.clearTargetReservations(touristEntity.getUUID());
    }

    @Override
    public boolean canSpendBudgetHere() {
        return true;
    }

    @Override
    public @Nullable Goal createGoalForTarget(TouristEntity tourist, ServerLevel serverLevel, ExperienceTarget target) {
        int ticksAtBlock = 0;
        boolean isPayingHere = target.pos().equals(this.getBlockPos());

        // Determine how long the tourist will visibly pause at the target.
        if (isPayingHere) {
            ticksAtBlock = TICKS_AT_BLOCK_WHEN_PAYING;
        } else {
            ticksAtBlock = tourist.getRandom().nextIntBetweenInclusive(20, 100);
        }

        return new LodgingExperienceGoal(
                tourist,
                isPayingHere ? GoalType.PAY : GoalType.SLEEP,
                target.pos(),
                tourist.getTicksAtCurrentTarget(),
                ticksAtBlock
        );
    }

    @Override
    protected AbstractContainerMenu createMenu(int i, Inventory inventory) {
        return new LodgingExperienceMenu(i, inventory, this, this.data, ContainerLevelAccess.create(this.level, this.getBlockPos()));
    }

    @Override
    public Direction getApproachFromDirection() {
        BlockState blockState = this.getBlockState();
        if (blockState.hasProperty(AbstractExperienceBlock.FACING)) {
            return blockState.getValue(AbstractExperienceBlock.FACING).getOpposite();
        }
        return Direction.NORTH;
    }

    private int getAvailableBed(RandomSource random) {
        List<Integer> availableBeds = new ArrayList<>();

        for (int i = 0; i < this.targets.size(); i++) {
            if (!this.targets.get(i).hasReservation()) {
                availableBeds.add(i);
            }
        }

        if (availableBeds.isEmpty()) {
            return -1;
        }

        if (this.orderedTargets) {
            return availableBeds.getFirst();
        }
        return availableBeds.get(random.nextInt(availableBeds.size()));
    }

    public ItemStack getDefaultCost() {
        return this.defaultCost.copy();
    }

    @Override
    public Component getDefaultName() {
        return Component.translatable("block.touristry.lodging_experience");
    }

    @Override
    public int getIdealApproachDistance() {
        return IDEAL_TARGET_APPROACH_DISTANCE;
    }

    @Override
    public int getMaxApproachDistance() {
        return MAX_APPROACH_DISTANCE;
    }

    @Override
    public int getMaxRangeToTarget() {
        return MAX_RANGE_TO_TARGET;
    }

    @Override
    public int getMinWaitAfterArrivalTicks() {
        return MIN_WAIT_AFTER_ARRIVAL_TICKS;
    }

    @Override
    public int getMaxWaitAfterArrivalTicks() {
        return MAX_WAIT_AFTER_ARRIVAL_TICKS;
    }

    @Override
    public int getPaymentSlotSize() {
        return PAYMENT_SLOT_SIZE;
    }

    public @Nullable ExperienceTarget getReservation(TouristEntity tourist) {
        UUID touristUUID = tourist.getUUID();
        return this.targets.stream()
                .filter(target -> target.isReservedBy(touristUUID))
                .findFirst().orElse(null);
    }

    @Override
    public List<ExperienceTarget> getTargetsForVisit(TouristEntity tourist, ServerLevel serverLevel) {
        this.pruneInvalidTargets(serverLevel);

        // Check if tourist already has reservation here.
        ExperienceTarget reservation = this.getReservation(tourist);

        // If not, then try to reserve a bed.
        if (reservation == null) {
            // Get an open bed.
            int targetIndex = this.getAvailableBed(tourist.getRandom());
            if (targetIndex == -1) {
                return Collections.emptyList();
            }

            // Reserve the bed for the tourist.
            if (!this.reserveBed(targetIndex, tourist.getUUID())) {
                return Collections.emptyList();
            }

            reservation = this.targets.get(targetIndex);
        }

        // Add the bed as the first target for this visit.
        List<ExperienceTarget> targetList = new ArrayList<>();
        targetList.add(reservation);

        // Tourists finish their lodging experience at the block entity to pay for their stay.
        targetList.add(new ExperienceTarget(
                this.getBlockPos(),
                this.getApproachFromDirection(),
                null,
                serverLevel.getDayTime(),
                "",
                null
        ));

        return targetList;
    }

    @Override
    public boolean hasAvailableBeds() {
        for (ExperienceTarget target : this.targets) {
            if (!target.hasReservation()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isLodgingExperience() {
        return true;
    }

    @Override
    protected boolean isTargetValid(ServerLevel serverLevel, ExperienceTarget target) {
        // Check entity targets.
        if (target.isEntity()) {
            // There are currently no valid bed entities.
            return false;
        }

        // Check if block still exists and is valid as a bed.
        BlockEntity blockEntity = serverLevel.getBlockEntity(target.pos());
        BlockState blockState = serverLevel.getBlockState(target.pos());
        return blockEntity instanceof BedBlockEntity || blockState.is(BlockTags.BEDS);
    }

    @Override
    public ExperienceVisit prepareToLeaveEarly(ExperienceVisit visit) {
        if (visit.remainingTargets().isEmpty()) {
            return visit;
        }

        List<ExperienceTarget> updatedTargets = new ArrayList<>();
        boolean haveAddedCurrentTarget = false;
        for (ExperienceTarget target : visit.remainingTargets()) {
            if (!haveAddedCurrentTarget || target.pos().equals(this.getBlockPos())) {
                updatedTargets.add(target);
                haveAddedCurrentTarget = true;
            }
        }

        return visit.withTargets(updatedTargets);
    }

    public boolean reserveBed(int targetIndex, UUID touristUUID) {
        if (targetIndex < 0 || targetIndex >= this.targets.size()) {
            return false;
        }

        ExperienceTarget target = this.targets.get(targetIndex);
        if (target.hasReservation()) {
            return target.isReservedBy(touristUUID);
        }

        this.targets.set(targetIndex, target.withReservation(touristUUID));
        this.setChanged();
        return true;
    }

    public void resetDefaultCost() {
        this.defaultCost = new ItemStack(Items.EMERALD);
        this.setChanged();
    }

    public void setDefaultCost(ItemStack itemStack) {
        this.defaultCost = itemStack.copy();
        this.setChanged();
    }

    //region Persistence Methods
    @Override
    protected void loadAdditional(ValueInput valueInput) {
        super.loadAdditional(valueInput);

        this.defaultCost = valueInput.read("DefaultCost", ItemStack.OPTIONAL_CODEC).orElse(this.defaultCost);
    }

    @Override
    protected void saveAdditional(ValueOutput valueOutput) {
        super.saveAdditional(valueOutput);

        valueOutput.store("DefaultCost", ItemStack.OPTIONAL_CODEC, this.defaultCost);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter dataComponentGetter) {
        super.applyImplicitComponents(dataComponentGetter);

        // Restore additional components when BlockItem is placed as a Block/Block Entity.
        this.defaultCost = dataComponentGetter.getOrDefault(ModComponents.TOURIST_EXPERIENCE_ITEM_DEFAULT_COST, this.defaultCost);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);

        // Collect additional components to save in components container in BlockItem when block breaks.
        builder.set(ModComponents.TOURIST_EXPERIENCE_ITEM_DEFAULT_COST, this.defaultCost.copy());
    }

    @Override
    public void removeComponentsFromTag(ValueOutput valueOutput) {
        super.removeComponentsFromTag(valueOutput);

        // Remove raw tag entries for data that is carried by custom components in the block item form.
        valueOutput.discard("DefaultCost");
    }
    //endregion
}
