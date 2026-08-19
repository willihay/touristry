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
import org.bensam.touristry.Touristry;
import org.bensam.touristry.entity.TouristEntity;
import org.bensam.touristry.entity.goal.LookAtTargetPosGoal;
import org.bensam.touristry.menu.ShoppingExperienceMenu;
import org.bensam.touristry.tourism.experience.ExperienceTarget;
import org.bensam.touristry.tourism.experience.ItemPrice;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.TreeSet;

public class ShoppingExperienceBlockEntity extends AbstractExperienceBlockEntity {
    public static final int IDEAL_APPROACH_DISTANCE = 1; // Tourist should try to stand this far away for shopping targets
    public static final int MAX_APPROACH_DISTANCE = 4; // Skip target if tourist can't get closer than this distance
    public static final int MAX_RANGE_TO_TARGET = 100;
    public static final int PAYMENT_SLOT_SIZE = 9;
    public static final int TARGET_KEY_INDEX = PAYMENT_SLOT_SIZE;
    public static final int ENTRY_FEE_INDEX = TARGET_KEY_INDEX + 1;
    public static final int DEFAULT_COST_INDEX = ENTRY_FEE_INDEX + 1;
    public static final int TOTAL_INVENTORY_SIZE = PAYMENT_SLOT_SIZE + 3;

    private ItemStack defaultCost = ItemStack.EMPTY;
    protected TreeSet<ItemPrice> itemPrices;

    public ShoppingExperienceBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(ModBlockEntities.SHOPPING_EXPERIENCE.get(), blockPos, blockState, TOTAL_INVENTORY_SIZE);

        this.itemPrices = new TreeSet<>();

        if (this.defaultCost.isEmpty()) {
            this.defaultCost = new ItemStack(Items.EMERALD);
        }
        this.inventory.set(DEFAULT_COST_INDEX, this.defaultCost.copy());
    }

    // Lifecycle
    @Override
    public void onTouristArrival(TouristEntity tourist, ServerLevel serverLevel) {

    }

    @Override
    public boolean tickAtTarget(TouristEntity tourist, ServerLevel serverLevel, ExperienceTarget target) {
        if (target == null) {
            return true;
        }

        return tourist.getTicksAtCurrentTarget() >= 100; // minimum 5 game seconds at a shopping target
    }

    @Override
    public void onTouristDeparture(TouristEntity tourist, ServerLevel serverLevel, boolean completed) {

    }

    // Helpers
    @Override
    public boolean canSpendBudgetHere() {
        return true;
    }

    @Override
    public @Nullable Goal createGoalForTarget(TouristEntity tourist, ExperienceTarget target) {
        if (target.isChildExperience()) {
            return null; // just navigate to sub-experience
        }

        return new LookAtTargetPosGoal(tourist, target.pos());
    }

    @Override
    protected AbstractContainerMenu createMenu(int i, Inventory inventory) {
        return new ShoppingExperienceMenu(i, inventory, this, this.data, ContainerLevelAccess.create(this.level, this.getBlockPos()));
    }

    public @Nullable ItemPrice findItemPriceFor(ItemStack itemStack) {
        for (ItemPrice itemPrice : this.itemPrices) {
            if (ItemStack.isSameItemSameComponents(itemPrice.itemForSale(), itemStack)) {
                return itemPrice;
            }
        }
        return null;
    }

    public ItemStack getDefaultCost() {
        return this.defaultCost.copy();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block." + Touristry.MOD_ID + ".shopping_experience");
    }

    @Override
    public ItemStack getEntryFee() {
        return this.inventory.get(ENTRY_FEE_INDEX).copy();
    }

    @Override
    public int getIdealApproachDistance() {
        return IDEAL_APPROACH_DISTANCE;
    }

    public @Nullable ItemPrice getItemPrice(int index) {
        List<ItemPrice> itemPriceList = this.getItemPrices();

        if (index >= 0 && index < itemPriceList.size()) {
            return itemPriceList.get(index);
        }
        return null;
    }

    public List<ItemPrice> getItemPrices() {
        return this.itemPrices.stream().toList();
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
    public int getPaymentSlotSize() {
        return PAYMENT_SLOT_SIZE;
    }

    @Override
    protected int getTargetKeySlotIndex() {
        return PAYMENT_SLOT_SIZE;
    }

    @Override
    public boolean hasBeds() {
        return false;
    }

    @Override
    public boolean hasEntryFee() {
        return !this.inventory.get(ENTRY_FEE_INDEX).isEmpty();
    }

    public int importItemsFromTargets(ServerLevel serverLevel) {
        int numAdded = 0;

        this.pruneInvalidTargets(serverLevel);

        for (ExperienceTarget target : this.targets) {
            if (target.isBlock()) {
                BlockEntity blockEntity = serverLevel.getBlockEntity(target.pos());
                if (blockEntity instanceof Container container) {
                    if (container.iterator() instanceof ContainerIterator it) {
                        while (it.hasNext()) {
                            ItemStack itemInContainer = it.next();
                            if (!itemInContainer.isEmpty()) {
                                ItemStack copyOfItem = itemInContainer.copyWithCount(1);
                                if (copyOfItem.isDamageableItem()) {
                                    copyOfItem.setDamageValue(0);
                                }
                                ItemPrice itemPrice = new ItemPrice(copyOfItem, this.defaultCost.copy());
                                if (this.itemPrices.add(itemPrice)) {
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
        // Check child experiences.
        if (target.isChildExperience()) {
            return this.isTargetChildExperienceValid(target.childExperienceUUID());
        }

        // Check entity targets.
        if (target.isEntity()) {
            // There are currently no valid shopping entities.
            return false;
        }

        // Check if block still exists and is valid for shopping.
        BlockEntity blockEntity = serverLevel.getBlockEntity(target.pos());
        return blockEntity instanceof Container;
    }

    public boolean removeItemPrice(@NonNull ItemPrice itemPrice) {
        boolean removed = this.itemPrices.remove(itemPrice);
        if (removed) {
            this.setChanged();
        }
        return removed;
    }

    public void removeAllItemPrices() {
        this.itemPrices.clear();
        this.setChanged();
    }

    public void resetDefaultCost() {
        this.defaultCost = new ItemStack(Items.EMERALD);
        this.setChanged();
    }

    public void setDefaultCost(ItemStack itemStack) {
        this.defaultCost = itemStack;
        this.setChanged();
    }

    public void updateItemPrice(@NonNull ItemPrice itemPrice) {
        this.itemPrices.remove(itemPrice);
        this.itemPrices.add(itemPrice);
        this.setChanged();
    }

    //region Persistence Methods
    @Override
    protected void loadAdditional(ValueInput valueInput) {
        super.loadAdditional(valueInput);

        this.defaultCost = valueInput.read("DefaultCost", ItemStack.OPTIONAL_CODEC).orElse(this.defaultCost);
        this.inventory.set(DEFAULT_COST_INDEX, this.defaultCost.copy());

        this.itemPrices = valueInput.read("ItemPrices", ItemPrice.TREESET_CODEC).orElse(new TreeSet<>());
    }

    @Override
    protected void saveAdditional(ValueOutput valueOutput) {
        super.saveAdditional(valueOutput);

        valueOutput.store("DefaultCost", ItemStack.OPTIONAL_CODEC, this.defaultCost);
        valueOutput.store("ItemPrices", ItemPrice.TREESET_CODEC, this.itemPrices);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter dataComponentGetter) {
        super.applyImplicitComponents(dataComponentGetter);

        // Restore additional components when BlockItem is placed as a Block/Block Entity.
        this.defaultCost = dataComponentGetter.getOrDefault(ModComponents.SHOPPING_EXPERIENCE_DEFAULT_COST, this.defaultCost);
        this.inventory.set(DEFAULT_COST_INDEX, this.defaultCost.copy());

        this.itemPrices = new TreeSet<>();
        this.itemPrices.addAll(dataComponentGetter.getOrDefault(ModComponents.SHOPPING_EXPERIENCE_ITEM_PRICES, new TreeSet<>()));
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);

        // Collect additional components to save in components container in BlockItem when block breaks.
        builder.set(ModComponents.SHOPPING_EXPERIENCE_DEFAULT_COST, this.defaultCost.copy());

        if (!this.itemPrices.isEmpty()) {
            builder.set(ModComponents.SHOPPING_EXPERIENCE_ITEM_PRICES, this.itemPrices);
        }
    }

    @Override
    public void removeComponentsFromTag(ValueOutput valueOutput) {
        super.removeComponentsFromTag(valueOutput);

        // Remove raw tag entries for data that is carried by custom components in the block item form.
        valueOutput.discard("DefaultCost");
        valueOutput.discard("ItemPrices");
    }
    //endregion
}
