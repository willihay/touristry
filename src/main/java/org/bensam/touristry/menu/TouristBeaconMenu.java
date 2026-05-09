package org.bensam.touristry.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.bensam.touristry.ModMenus;
import org.jspecify.annotations.NonNull;

public class TouristBeaconMenu extends AbstractContainerMenu {
    // Slot layout
    private static final int BEACON_SLOT_COUNT = 9;
    private static final int BEACON_SLOT_START_X = 62;
    private static final int BEACON_SLOT_START_Y = 17;
    private static final int SLOT_SIDE_LENGTH = 18;

    // Player inventory layout
    private static final int PLAYER_INVENTORY_ROW_X = 8;
    private static final int PLAYER_INVENTORY_ROW_Y = 84;
    private static final int PLAYER_SLOT_START = BEACON_SLOT_COUNT;

    private final Container beaconInventory;

    // Client-side constructor:
    // Uses dummy containers so the menu can be constructed on the client
    // Real state is synced from the server through ContainerData and slot containers.
    public TouristBeaconMenu(int containerId, Inventory playerInventory) {
        this(
                containerId,
                playerInventory,
                new SimpleContainer(BEACON_SLOT_COUNT)
        );
    }

    // Server-side constructor:
    public TouristBeaconMenu(int containerId, Inventory playerInventory, Container beaconInventory) {
        super(ModMenus.TOURIST_BEACON_MENU.get(), containerId);
        this.beaconInventory = beaconInventory;

        // Add beacon inventory slots.
        add3x3GridSlots(beaconInventory, BEACON_SLOT_START_X, BEACON_SLOT_START_Y);

        // Add the player inventory slots.
        this.addStandardInventorySlots(playerInventory, PLAYER_INVENTORY_ROW_X, PLAYER_INVENTORY_ROW_Y);
    }

    protected void add3x3GridSlots(Container container, int firstSlotX, int firstSlotY) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slotIndex = col + row * 3;
                this.addSlot(new Slot(container, slotIndex, firstSlotX + col * SLOT_SIDE_LENGTH, firstSlotY + row * SLOT_SIDE_LENGTH));
            }
        }
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
        if (slotIndex < BEACON_SLOT_COUNT) {
            if (!this.moveItemStackTo(sourceStack, PLAYER_SLOT_START, inventorySize, false)) {
                return ItemStack.EMPTY;
            }
        }
        // Player inventory -> payment slot
        else {
            if (!this.moveItemStackTo(sourceStack, 0, BEACON_SLOT_COUNT, false)) {
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

        slot.onTake(player, sourceStack);
        return returnStack;
    }

    @Override
    public boolean stillValid(@NonNull Player player) {
        return this.beaconInventory.stillValid(player);
    }
}
