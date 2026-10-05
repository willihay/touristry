package org.bensam.touristry.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.bensam.touristry.ModBlockEntities;
import org.bensam.touristry.ModComponents;
import org.bensam.touristry.entity.TouristEntity;
import org.bensam.touristry.entity.TouristItemInterest;
import org.bensam.touristry.entity.goal.DiningExperienceGoal;
import org.bensam.touristry.menu.DiningExperienceMenu;
import org.bensam.touristry.tourism.experience.ExperienceTarget;
import org.bensam.touristry.tourism.experience.ExperienceVisit;
import org.bensam.touristry.tourism.experience.ItemPrice;
import org.bensam.touristry.tourism.experience.TouristExperience;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.*;

public class DiningExperienceBlockEntity extends AbstractExperienceBlockEntity {
    public static final int IDEAL_TARGET_APPROACH_DISTANCE = 1; // Tourist should try to stand this far away for dining spot targets
    public static final int MAX_APPROACH_DISTANCE = 4; // Skip target if tourist can't get closer than this distance
    public static final int MAX_RANGE_TO_TARGET = 100;
    public static final int MIN_TICKS_CHOOSING_FOOD_AT_COUNTER = 80;
    public static final int MAX_TICKS_CHOOSING_FOOD_AT_COUNTER = 200;
    public static final int MIN_WAIT_AFTER_ARRIVAL_TICKS = 0;
    public static final int MAX_WAIT_AFTER_ARRIVAL_TICKS = 0;
    public static final int TICKS_AT_BLOCK_WHEN_PAYING = 60;
    public static final int PAYMENT_SLOT_SIZE = 9;

    private ItemStack defaultCost = ItemStack.EMPTY;
    private LinkedHashMap<ItemStackKey, ItemPrice> menuPrices;

    public DiningExperienceBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(ModBlockEntities.DINING_EXPERIENCE.get(), blockPos, blockState, PAYMENT_SLOT_SIZE);

        this.menuPrices = new LinkedHashMap<>();

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
        int ticksAtBlock = 0;
        boolean isOrderingHere = tourist.getShoppingBag().isEmpty();
        boolean isPayingHere = target.pos().equals(this.getBlockPos());

        // Determine how long the tourist will visibly pause at the target.
        if (isOrderingHere) {
            ticksAtBlock = tourist.getRandom().nextIntBetweenInclusive(MIN_TICKS_CHOOSING_FOOD_AT_COUNTER, MAX_TICKS_CHOOSING_FOOD_AT_COUNTER);
        } else if (isPayingHere) {
            ticksAtBlock = TICKS_AT_BLOCK_WHEN_PAYING;
        }

        return new DiningExperienceGoal(
                tourist,
                this.getBlockPos(),
                target.pos(),
                tourist.getTicksAtCurrentTarget(),
                ticksAtBlock,
                isOrderingHere,
                isPayingHere
        );
    }

    @Override
    protected AbstractContainerMenu createMenu(int i, Inventory inventory) {
        return new DiningExperienceMenu(i, inventory, this, this.data, ContainerLevelAccess.create(this.level, this.getBlockPos()));
    }

    public ItemStack getDefaultCost() {
        return this.defaultCost.copy();
    }

    @Override
    public Component getDefaultName() {
        return Component.translatable("block.touristry.dining_experience");
    }

    @Override
    public int getIdealApproachDistance() {
        return IDEAL_TARGET_APPROACH_DISTANCE;
    }

    public @Nullable ItemPrice getMenuPrice(int index) {
        List<ItemPrice> menuPriceList = this.getMenuPrices();

        if (index >= 0 && index < menuPriceList.size()) {
            return menuPriceList.get(index);
        }
        return null;
    }

    public @NonNull ItemPrice getMenuPrice(ItemStack itemStack) {
        ItemPrice menuPrice = this.lookupItemPriceFor(itemStack);
        if (menuPrice != null && menuPrice.cost() != null) {
            return menuPrice;
        }

        return new ItemPrice(itemStack.copyWithCount(1), this.getDefaultCost());
    }

    public List<ItemPrice> getMenuPrices() {
        return this.menuPrices.values().stream()
                .sorted(ItemPrice.DISPLAY_ORDER)
                .toList();
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
        // If the dining experience has seating available, most tourists will elect to sit.

        // Tourists finish their dining experience at the block entity to pay for their food.
        return List.of(new ExperienceTarget(
                    this.getBlockPos(),
                    this.getApproachDirection(),
                    null,
                    serverLevel.getDayTime()
        ));
    }

    public int importItemsFromTargets(ServerLevel serverLevel) {
        int numAdded = 0;

        this.pruneInvalidTargets(serverLevel);

        for (ExperienceTarget target : this.targets) {
            if (target.isBlock()) {
                Container container = AbstractExperienceBlockEntity.getTargetContainer(serverLevel, target.pos());
                if (container != null) {
                    if (container.iterator() instanceof ContainerIterator it) {
                        while (it.hasNext()) {
                            ItemStack itemInContainer = it.next();
                            if (!itemInContainer.isEmpty() && TouristItemInterest.FOOD.isAMatch(itemInContainer, serverLevel)) {
                                ItemStack copyOfItem = itemInContainer.copyWithCount(1);
                                if (copyOfItem.isDamageableItem()) {
                                    copyOfItem.setDamageValue(0);
                                }
                                ItemPrice itemPrice = new ItemPrice(copyOfItem, this.getDefaultCost());
                                if (this.menuPrices.putIfAbsent(new ItemStackKey(copyOfItem), itemPrice) == null) {
                                    numAdded++;
                                }
                            }
                        }
                    }
                }
            }
        }

        if (numAdded > 0) {
            this.setChanged();
        }

        return numAdded;
    }

    @Override
    protected boolean isTargetValid(ServerLevel serverLevel, ExperienceTarget target) {
        // Check entity targets.
        if (target.isEntity()) {
            // There are currently no valid pantry entities.
            return false;
        }

        // Check if block still exists and is valid as a food pantry.
        BlockEntity blockEntity = serverLevel.getBlockEntity(target.pos());
        return blockEntity instanceof Container && !(blockEntity instanceof TouristExperience);
    }

    public @Nullable ItemPrice lookupItemPriceFor(ItemStack itemStack) {
        return this.menuPrices.get(new ItemStackKey(itemStack));
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

    public boolean removeMenuPrice(@NonNull ItemPrice menuPrice) {
        boolean removed = this.menuPrices.remove(new ItemStackKey(menuPrice.itemForSale())) != null;
        if (removed) {
            this.setChanged();
        }
        return removed;
    }

    public void removeAllMenuPrices() {
        if (!this.menuPrices.isEmpty()) {
            this.menuPrices.clear();
            this.setChanged();
        }
    }

    public void removeDefaultMenuPrices() {
        ItemStack defaultCost = this.getDefaultCost();
        Iterator<Map.Entry<ItemStackKey, ItemPrice>> iterator = this.menuPrices.entrySet().iterator();
        boolean removed = false;

        while (iterator.hasNext()) {
            Map.Entry<ItemStackKey, ItemPrice> entry = iterator.next();
            ItemPrice price = entry.getValue();

            if (price.itemForSale().getCount() > 1) {
                continue;
            }

            if (price.cost() == null || ItemStack.matches(price.cost(), defaultCost)) {
                iterator.remove();
                removed = true;
            }
        }

        if (removed) {
            this.setChanged();
        }
    }

    public void resetDefaultCost() {
        this.defaultCost = new ItemStack(Items.EMERALD);
        this.setChanged();
    }

    public void setDefaultCost(ItemStack itemStack) {
        this.defaultCost = itemStack.copy();
        this.setChanged();
    }

    public void updateMenuPrice(@NonNull ItemPrice menuPrice) {
        this.menuPrices.put(new ItemStackKey(menuPrice.itemForSale()), menuPrice);
        this.setChanged();
    }

    //region Persistence Methods
    @Override
    protected void loadAdditional(ValueInput valueInput) {
        super.loadAdditional(valueInput);

        this.defaultCost = valueInput.read("DefaultCost", ItemStack.OPTIONAL_CODEC).orElse(this.defaultCost);
        this.menuPrices = valueInput.read("MenuPrices", ItemPrice.MAP_CODEC).orElse(new LinkedHashMap<>());
    }

    @Override
    protected void saveAdditional(ValueOutput valueOutput) {
        super.saveAdditional(valueOutput);

        valueOutput.store("DefaultCost", ItemStack.OPTIONAL_CODEC, this.defaultCost);
        valueOutput.store("MenuPrices", ItemPrice.MAP_CODEC, this.menuPrices);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter dataComponentGetter) {
        super.applyImplicitComponents(dataComponentGetter);

        // Restore additional components when BlockItem is placed as a Block/Block Entity.
        this.defaultCost = dataComponentGetter.getOrDefault(ModComponents.TOURIST_EXPERIENCE_ITEM_DEFAULT_COST, this.defaultCost);
        this.menuPrices = dataComponentGetter.getOrDefault(ModComponents.TOURIST_EXPERIENCE_ITEM_PRICES, new LinkedHashMap<>());
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);

        // Collect additional components to save in components container in BlockItem when block breaks.
        builder.set(ModComponents.TOURIST_EXPERIENCE_ITEM_DEFAULT_COST, this.defaultCost.copy());

        if (!this.menuPrices.isEmpty()) {
            builder.set(ModComponents.TOURIST_EXPERIENCE_ITEM_PRICES, this.menuPrices);
        }
    }

    @Override
    public void removeComponentsFromTag(ValueOutput valueOutput) {
        super.removeComponentsFromTag(valueOutput);

        // Remove raw tag entries for data that is carried by custom components in the block item form.
        valueOutput.discard("DefaultCost");
        valueOutput.discard("MenuPrices");
    }
    //endregion
}
