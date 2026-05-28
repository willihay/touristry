package org.bensam.touristry.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.bensam.touristry.ModBlocks;
import org.bensam.touristry.ModMenus;
import org.bensam.touristry.block.entity.TouristBeaconBlockEntity;
import org.jspecify.annotations.NonNull;

public class TouristBeaconMenu extends AbstractContainerMenu {
    public static final int BUTTON_TOGGLE_OPEN_FOR_BUSINESS = 0;

    // Slot layout
    private static final int BEACON_SLOT_COUNT = TouristBeaconBlockEntity.TOTAL_INVENTORY_SIZE;
    private static final int BEACON_PAYMENT_SLOT_COUNT = TouristBeaconBlockEntity.PAYMENT_SLOT_SIZE;
    private static final int BEACON_PAYMENT_SLOT_START_X = 116;
    private static final int BEACON_PAYMENT_SLOT_START_Y = 14;
    private static final int BEACON_KEY_SLOT = TouristBeaconBlockEntity.BEACON_KEY_SLOT_INDEX;
    private static final int BEACON_KEY_SLOT_X = 80;
    private static final int BEACON_KEY_SLOT_Y = 32;
    private static final int SLOT_SIDE_LENGTH = 18;

    // Player inventory layout
    private static final int PLAYER_INVENTORY_ROW_X = 8;
    private static final int PLAYER_INVENTORY_ROW_Y = 84;
    private static final int PLAYER_SLOT_START = BEACON_SLOT_COUNT;

    private final Container beaconInventory;
    private final ContainerData beaconContainerData;
    private final ContainerLevelAccess containerLevelAccess;

    // Client-side constructor:
    // Uses dummy containers so the menu can be constructed on the client
    // Real state is synced from the server through ContainerData and slot containers.
    public TouristBeaconMenu(int containerId, Inventory playerInventory) {
        this(
                containerId,
                playerInventory,
                new SimpleContainer(BEACON_SLOT_COUNT),
                new SimpleContainerData(TouristBeaconBlockEntity.DATA_COUNT),
                ContainerLevelAccess.NULL
        );
    }

    // Server-side constructor:
    public TouristBeaconMenu(int containerId, Inventory playerInventory, Container beaconInventory, ContainerData data, ContainerLevelAccess access) {
        super(ModMenus.TOURIST_BEACON_MENU.get(), containerId);
        this.beaconInventory = beaconInventory;
        this.beaconContainerData = data;
        this.containerLevelAccess = access;

        // Add beacon payment slots.
        add3x3GridSlots(beaconInventory, BEACON_PAYMENT_SLOT_START_X, BEACON_PAYMENT_SLOT_START_Y);

        // Add beacon key slot.
        this.addSlot(new Slot(beaconInventory, BEACON_KEY_SLOT, BEACON_KEY_SLOT_X, BEACON_KEY_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) { return false; }

            @Override
            public void onTake(Player player, ItemStack stack) {
                TouristBeaconMenu.this.onKeyTake(player, stack);
            }
        });

        // Add the player inventory slots.
        this.addStandardInventorySlots(playerInventory, PLAYER_INVENTORY_ROW_X, PLAYER_INVENTORY_ROW_Y);

        // Add data slots for data sync.
        this.addDataSlots(this.beaconContainerData);
    }

    protected void add3x3GridSlots(Container container, int firstSlotX, int firstSlotY) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slotIndex = col + row * 3;
                this.addSlot(new Slot(container, slotIndex, firstSlotX + col * SLOT_SIDE_LENGTH, firstSlotY + row * SLOT_SIDE_LENGTH));
            }
        }
    }

    public double getReputation() {
        return (double) this.beaconContainerData.get(TouristBeaconBlockEntity.DATA_REPUTATION) / 100;
    }

    public boolean isOpenForBusiness() {
        return this.beaconContainerData.get(TouristBeaconBlockEntity.DATA_OPEN_FOR_BUSINESS) != 0;
    }

    private void toggleOpenForBusiness() {
        this.containerLevelAccess.execute((level, blockPos) -> {
            if (level.getBlockEntity(blockPos) instanceof TouristBeaconBlockEntity beaconBlockEntity) {
                beaconBlockEntity.setOpenForBusiness(!beaconBlockEntity.isOpenForBusiness());
                level.updateNeighbourForOutputSignal(blockPos, level.getBlockState(blockPos).getBlock());
            }
        });
    }

    @Override
    public boolean clickMenuButton(@NonNull Player player, int buttonId) {
        if (buttonId != BUTTON_TOGGLE_OPEN_FOR_BUSINESS) {
            return false;
        }

        if (!player.level().isClientSide()) {
            this.toggleOpenForBusiness();
        }

        return true;
    }

    protected void onKeyTake(Player player, ItemStack itemStack) {
        if (this.beaconInventory instanceof TouristBeaconBlockEntity touristBeaconBlockEntity) {
            Slot slot = this.slots.get(BEACON_KEY_SLOT);
            slot.set(touristBeaconBlockEntity.getBeaconKey());
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

        // The beacon key slot is intentionally infinite. Returning the copied
        // key here would make Minecraft's quick-move loop keep pulling keys
        // while the refilled slot still matches the returned stack.
        if (slotIndex == BEACON_KEY_SLOT) {
            ItemStack keyToMove = sourceStack.copy();
            keyToMove.setCount(1);

            if (!this.moveItemStackTo(keyToMove, PLAYER_SLOT_START, inventorySize, false)) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, sourceStack);
            return ItemStack.EMPTY;
        }

        // Beacon slot -> player inventory
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

        return returnStack;
    }

    @Override
    public boolean stillValid(@NonNull Player player) {
        return stillValid(this.containerLevelAccess, player, ModBlocks.TOURIST_BEACON.get());
    }
}
