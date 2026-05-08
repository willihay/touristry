package org.bensam.touristry.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.bensam.touristry.ModBlockEntities;
import org.bensam.touristry.tourism.TourismManager;
import org.jspecify.annotations.NonNull;

public class TouristBeaconBlockEntity extends BlockEntity implements Container {
    // temporary: payment storage
    private static final int PAYMENT_SLOT = 0;
    private final NonNullList<ItemStack> paymentItems = NonNullList.withSize(1, ItemStack.EMPTY);

    public TouristBeaconBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(ModBlockEntities.TOURIST_BEACON.get(), blockPos, blockState);
    }

    //region Item Helpers
    @Override
    public void clearContent() {
        this.paymentItems.clear();
        this.paymentItems.add(ItemStack.EMPTY);
        this.recomputeState();
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public @NonNull ItemStack getItem(int slotIndex) {
        return this.paymentItems.get(slotIndex);
    }

    @Override
    public boolean isEmpty() {
        return this.paymentItems.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public @NonNull ItemStack removeItem(int slotIndex, int amount) {
        ItemStack result = ContainerHelper.removeItem(this.paymentItems, slotIndex, amount);
        if (!result.isEmpty()) {
            this.recomputeState();
        }
        return result;
    }

    @Override
    public @NonNull ItemStack removeItemNoUpdate(int slotIndex) {
        ItemStack result = ContainerHelper.takeItem(this.paymentItems, slotIndex);
        return result;
    }

    @Override
    public void setItem(int slotIndex, @NonNull ItemStack itemStack) {
        this.paymentItems.set(slotIndex, itemStack);

        if (itemStack.getCount() > this.getMaxStackSize()) {
            itemStack.setCount(this.getMaxStackSize());
        }

        this.recomputeState();
    }

    public boolean tryDepositItem(ItemStack depositStack) {
        if (depositStack.isEmpty()) {
            return false;
        }

        ItemStack itemInventory = this.getItem(PAYMENT_SLOT);
        if (!itemInventory.isEmpty() && (!ItemStack.isSameItemSameComponents(itemInventory, depositStack)
                || itemInventory.getCount() >= itemInventory.getMaxStackSize())) {
            return false;
        }

        if (itemInventory.isEmpty()) {
            this.setItem(PAYMENT_SLOT, depositStack);
        } else {
            itemInventory.grow(1); // TODO: deposit up to full amount in depositStack
        }

        this.recomputeState();
        return true;
    }
    //endregion

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        // Register this block entity for tourism when block entity is attached back into a chunk/world.
        this.syncTourismRegistration();
    }

    public void recomputeState() {
        if (!(this.level instanceof ServerLevel)) {
            return;
        }
        setChanged();
    }

    @Override
    public void setRemoved() {
        // Unregister this block entity from tourism when block entity is being removed, replaced, or unloaded from active use.
        TourismManager.unregisterTouristBeacon(this);
        super.setRemoved();
    }

    @Override
    public boolean stillValid(@NonNull Player player) {
        return Container.stillValidBlockEntity(this, player);
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
        this.paymentItems.set(0, ItemStack.EMPTY);
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
        // Restore input slot items when BlockItem is placed as a Block/Block Entity.
        dataComponentGetter.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(this.paymentItems);
        this.recomputeState();
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.@NonNull Builder builder) {
        super.collectImplicitComponents(builder);
        // Collect items in input slots to save in components container in BlockItem when block breaks.
        builder.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(this.paymentItems));
    }

    @SuppressWarnings("deprecation")
    @Override
    public void removeComponentsFromTag(@NonNull ValueOutput valueOutput) {
        super.removeComponentsFromTag(valueOutput);
        valueOutput.discard("Items"); // no need for Items tag when representing this block entity as components
    }

    @Override
    public void preRemoveSideEffects(@NonNull BlockPos blockPos, @NonNull BlockState blockState) {
        // Overriding with an empty method prevents spilling contents on block break.
    }
    //endregion
}
