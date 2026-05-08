package org.bensam.touristry.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.bensam.touristry.ModBlocks;
import org.bensam.touristry.ModMenus;
import org.jspecify.annotations.NonNull;

public class TouristBeaconMenu extends AbstractContainerMenu {
    // Slot layout
    private static final int PAYMENT_SLOT = 0;
    public static final int PAYMENT_SLOT_X = 62;
    public static final int PAYMENT_SLOT_Y = 17;
    private static final int BEACON_SLOT_COUNT = 1;

    // Player inventory layout
    private static final int PLAYER_INVENTORY_ROW_X = 8;
    private static final int PLAYER_INVENTORY_ROW_Y = 84;
    private static final int FIRST_PLAYER_SLOT = BEACON_SLOT_COUNT;
    private static final int FIRST_HOTBAR_SLOT = FIRST_PLAYER_SLOT + 27;

    private final Container blockInventory;
    private final ContainerLevelAccess access;

    // Client-side constructor:
    // Uses dummy containers so the menu can be constructed on the client
    // Real state is synced from the server through ContainerData and slot containers.
    public TouristBeaconMenu(int containerId, Inventory playerInventory) {
        this(
                containerId,
                playerInventory,
                new SimpleContainer(BEACON_SLOT_COUNT),
                ContainerLevelAccess.NULL
        );
    }

    // Server-side constructor:
    public TouristBeaconMenu(int containerId, Inventory playerInventory, Container blockInventory, ContainerLevelAccess access) {
        super(ModMenus.TOURIST_BEACON_MENU.get(), containerId);
        this.blockInventory = blockInventory;
        this.access = access;

        // Add block entity slots.
        // Slot 0: Payment
        this.addSlot(new Slot(blockInventory, PAYMENT_SLOT, PAYMENT_SLOT_X, PAYMENT_SLOT_Y));

        // Add the player inventory slots.
        this.addStandardInventorySlots(playerInventory, PLAYER_INVENTORY_ROW_X, PLAYER_INVENTORY_ROW_Y);
    }

    protected boolean isValidBlock(BlockState blockState) {
        return blockState.is(ModBlocks.TOURIST_BEACON.get());
    }

    @Override
    public @NonNull ItemStack quickMoveStack(@NonNull Player player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);

        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        int inventorySize = this.slots.size();
        ItemStack sourceStack = slot.getItem();
        ItemStack returnStack = sourceStack.copy();

        // Payment slot -> player inventory
        if (slotIndex == PAYMENT_SLOT) {
            if (!this.moveItemStackTo(sourceStack, FIRST_PLAYER_SLOT, inventorySize, false)) {
                return ItemStack.EMPTY;
            }
        }
        // Player inventory -> payment slot
        else {
            if (!this.moveItemStackTo(sourceStack, PAYMENT_SLOT, BEACON_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (sourceStack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        if (sourceStack.getCount() == returnStack.getCount()) {
            return ItemStack.EMPTY;
        }

        return returnStack;
    }

    @Override
    public boolean stillValid(@NonNull Player player) {
        return this.blockInventory.stillValid(player);
    }
}
