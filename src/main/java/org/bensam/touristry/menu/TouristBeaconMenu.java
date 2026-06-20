package org.bensam.touristry.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import org.bensam.touristry.ModBlocks;
import org.bensam.touristry.ModMenus;
import org.bensam.touristry.block.entity.TouristBeaconBlockEntity;
import org.jspecify.annotations.NonNull;

public class TouristBeaconMenu extends AbstractContainerMenu implements TourismStatusMenu {
    public static final int BUTTON_TOGGLE_OPEN_FOR_BUSINESS = 0;

    // Player inventory layout
    private static final int PLAYER_INVENTORY_ROW_X = 8;
    private static final int PLAYER_INVENTORY_ROW_Y = 84;

    private final ContainerData beaconContainerData;
    private final ContainerLevelAccess containerLevelAccess;

    // Client-side constructor:
    // Uses dummy containers so the menu can be constructed on the client
    // Real state is synced from the server through ContainerData and slot containers.
    public TouristBeaconMenu(int containerId, Inventory playerInventory) {
        this(
                containerId,
                playerInventory,
                new SimpleContainerData(TouristBeaconBlockEntity.DATA_COUNT),
                ContainerLevelAccess.NULL
        );
    }

    // Server-side constructor:
    public TouristBeaconMenu(int containerId, Inventory playerInventory, ContainerData data, ContainerLevelAccess access) {
        super(ModMenus.TOURIST_BEACON_MENU.get(), containerId);
        this.beaconContainerData = data;
        this.containerLevelAccess = access;

        // Add the player inventory slots.
        this.addStandardInventorySlots(playerInventory, PLAYER_INVENTORY_ROW_X, PLAYER_INVENTORY_ROW_Y);

        // Add data slots for data sync.
        this.addDataSlots(this.beaconContainerData);
    }

    public int getContainerId() {
        return this.containerId;
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

    @Override
    public @NonNull ItemStack quickMoveStack(@NonNull Player player, int slotIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(@NonNull Player player) {
        return stillValid(this.containerLevelAccess, player, ModBlocks.TOURIST_BEACON.get());
    }
}
