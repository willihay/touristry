package org.bensam.touristry.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.bensam.touristry.ModBlockEntities;
import org.bensam.touristry.Touristry;
import org.bensam.touristry.menu.TouristBeaconMenu;
import org.bensam.touristry.tourism.TourismManager;
import org.jspecify.annotations.NonNull;

public class TouristBeaconBlockEntity extends BaseContainerBlockEntity {
    private static final int INVENTORY_SIZE = 9;
    private NonNullList<ItemStack> paymentItems = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);

    public TouristBeaconBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(ModBlockEntities.TOURIST_BEACON.get(), blockPos, blockState);
    }

    @Override
    public void clearContent() {
        super.clearContent();
        this.setChanged();
    }

    @Override
    protected @NonNull AbstractContainerMenu createMenu(int i, Inventory inventory) {
        return new TouristBeaconMenu(i, inventory, this);
    }

    @Override
    public int getContainerSize() {
        return this.paymentItems.size();
    }

    @Override
    protected @NonNull Component getDefaultName() {
        return Component.translatable("block." + Touristry.MOD_ID + ".tourist_beacon");
    }

    @Override
    protected @NonNull NonNullList<ItemStack> getItems() {
        return this.paymentItems;
    }

    @Override
    protected void setItems(@NonNull NonNullList<ItemStack> nonNullList) {
        this.paymentItems = nonNullList;
    }

    public boolean tryDepositItem(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return true;
        }

        ItemStack depositStack = itemStack.copy();

        for (int i = 0; i < this.paymentItems.size(); i++) {
            ItemStack slotStack = this.getItem(i);
            if (!slotStack.isEmpty() && (!ItemStack.isSameItemSameComponents(slotStack, depositStack)
                    || slotStack.getCount() >= slotStack.getMaxStackSize())) {
                continue;
            }

            if (slotStack.isEmpty()) {
                this.setItem(i, depositStack);
                return true;
            } else {
                int depositCount = depositStack.getCount();
                int slotCount = slotStack.getCount();
                int totalCount = slotCount + depositCount;
                int overMax = totalCount > slotStack.getMaxStackSize() ? totalCount - slotStack.getMaxStackSize() : 0;
                if (overMax == 0) {
                    slotStack.setCount(totalCount);
                    return true;
                }
                slotStack.setCount(getMaxStackSize());
                depositStack.setCount(overMax);
            }
        }

        return false;
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        // Register this block entity for tourism when block entity is attached back into a chunk/world.
        this.syncTourismRegistration();
    }

    @Override
    public void setRemoved() {
        // Unregister this block entity from tourism when block entity is being removed, replaced, or unloaded from active use.
        TourismManager.unregisterTouristBeacon(this);
        super.setRemoved();
    }

    private void syncTourismRegistration() {
        if (this.level instanceof ServerLevel && !this.isRemoved()) {
            TourismManager.registerTouristBeacon(this);
        } else {
            TourismManager.unregisterTouristBeacon(this);
        }
    }

    //region Persistence Methods
    @Override
    protected void loadAdditional(@NonNull ValueInput valueInput) {
        super.loadAdditional(valueInput);
        this.paymentItems = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(valueInput, this.paymentItems);
    }

    @Override
    protected void saveAdditional(@NonNull ValueOutput valueOutput) {
        super.saveAdditional(valueOutput);
        ContainerHelper.saveAllItems(valueOutput, this.paymentItems);
    }

    @Override
    protected void applyImplicitComponents(@NonNull DataComponentGetter dataComponentGetter) {
        super.applyImplicitComponents(dataComponentGetter);

        // Restore additional components when BlockItem is placed as a Block/Block Entity.
        // ...
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.@NonNull Builder builder) {
        super.collectImplicitComponents(builder);

        // Collect additional components to save in components container in BlockItem when block breaks.
        // ...
    }

    @Override
    public void removeComponentsFromTag(@NonNull ValueOutput valueOutput) {
        super.removeComponentsFromTag(valueOutput);

        // Discard additional component tags when representing this block entity as components.
        // ...
    }

    @Override
    public void preRemoveSideEffects(@NonNull BlockPos blockPos, @NonNull BlockState blockState) {
        // Overriding with an empty method prevents spilling contents on block break.
    }
    //endregion
}
