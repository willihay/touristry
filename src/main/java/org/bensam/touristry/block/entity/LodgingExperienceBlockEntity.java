package org.bensam.touristry.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
import org.bensam.touristry.menu.LodgingExperienceMenu;
import org.bensam.touristry.tourism.experience.ExperienceTarget;
import org.bensam.touristry.tourism.experience.ExperienceVisit;
import org.bensam.touristry.tourism.experience.LodgingBedInfo;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class LodgingExperienceBlockEntity extends AbstractExperienceBlockEntity {
    public static final int IDEAL_TARGET_APPROACH_DISTANCE = 1; // Tourist should try to stand this far away for bed targets
    public static final int MAX_APPROACH_DISTANCE = 3; // Skip target if tourist can't get closer than this distance
    public static final int MAX_RANGE_TO_TARGET = 100;
    public static final int MIN_WAIT_AFTER_ARRIVAL_TICKS = 100;
    public static final int MAX_WAIT_AFTER_ARRIVAL_TICKS = 200;
    public static final int TICKS_AT_BLOCK_WHEN_PAYING = 100;
    public static final int PAYMENT_SLOT_SIZE = 9;

    private ItemStack defaultCost = ItemStack.EMPTY;
    private LinkedHashMap<BlockPos, LodgingBedInfo> bedInfo;

    public LodgingExperienceBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(ModBlockEntities.LODGING_EXPERIENCE.get(), blockPos, blockState, PAYMENT_SLOT_SIZE);

        this.bedInfo = new LinkedHashMap<>();

        if (this.defaultCost.isEmpty()) {
            this.defaultCost = new ItemStack(Items.EMERALD);
        }
    }

    @Override
    public boolean canSpendBudgetHere() {
        return true;
    }

    @Override
    public @Nullable Goal createGoalForTarget(TouristEntity tourist, ServerLevel serverLevel, ExperienceTarget target) {
        return null;
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

    @Override
    public List<ExperienceTarget> getTargetsForVisit(ServerLevel serverLevel) {

        // Tourists finish their lodging experience at the block entity to pay for their stay.
        return List.of(new ExperienceTarget(
                this.getBlockPos(),
                this.getApproachFromDirection(),
                null,
                serverLevel.getDayTime()
        ));
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
        return blockEntity instanceof BedBlockEntity;
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

        return new ExperienceVisit(
                visit.experienceUUID(),
                visit.budgetRemaining(),
                updatedTargets,
                visit.targetsCompleted(),
                visit.totalTargets(),
                visit.result(),
                visit.hasReviewed()
        );
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
        this.bedInfo = valueInput.read("BedInfo", LodgingBedInfo.BED_INFO_CODEC).orElse(new LinkedHashMap<>());
    }

    @Override
    protected void saveAdditional(ValueOutput valueOutput) {
        super.saveAdditional(valueOutput);

        valueOutput.store("DefaultCost", ItemStack.OPTIONAL_CODEC, this.defaultCost);
        valueOutput.store("BedInfo", LodgingBedInfo.BED_INFO_CODEC, this.bedInfo);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter dataComponentGetter) {
        super.applyImplicitComponents(dataComponentGetter);

        // Restore additional components when BlockItem is placed as a Block/Block Entity.
        this.defaultCost = dataComponentGetter.getOrDefault(ModComponents.TOURIST_EXPERIENCE_ITEM_DEFAULT_COST, this.defaultCost);
        this.bedInfo = dataComponentGetter.getOrDefault(ModComponents.TOURIST_EXPERIENCE_BED_INFO, new LinkedHashMap<>());
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);

        // Collect additional components to save in components container in BlockItem when block breaks.
        builder.set(ModComponents.TOURIST_EXPERIENCE_ITEM_DEFAULT_COST, this.defaultCost.copy());

        if (!this.bedInfo.isEmpty()) {
            builder.set(ModComponents.TOURIST_EXPERIENCE_BED_INFO, this.bedInfo);
        }
    }

    @Override
    public void removeComponentsFromTag(ValueOutput valueOutput) {
        super.removeComponentsFromTag(valueOutput);

        // Remove raw tag entries for data that is carried by custom components in the block item form.
        valueOutput.discard("DefaultCost");
        valueOutput.discard("BedInfo");
    }
    //endregion
}
