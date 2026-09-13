package org.bensam.touristry.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.bensam.touristry.ModBlocks;
import org.bensam.touristry.ModMenus;
import org.bensam.touristry.block.entity.AbstractExperienceBlockEntity;
import org.bensam.touristry.block.entity.SightseeingExperienceBlockEntity;
import org.jspecify.annotations.NonNull;

public class SightseeingExperienceMenu extends AbstractExperienceMenu<SightseeingExperienceMenu.Tab> {
    // Slot layout
    private static final int CONFIGURATION_SLOT_COUNT = 2; // target key + entry fee
    private static final int EXPERIENCE_PAYMENT_SLOT_COUNT = SightseeingExperienceBlockEntity.PAYMENT_SLOT_SIZE;
    private static final int EXPERIENCE_PAYMENT_SLOT_START_X = 116;
    private static final int EXPERIENCE_PAYMENT_SLOT_START_Y = 17;
    private static final int CONFIGURATION_TARGET_KEY_SLOT = 0;
    private static final int EXPERIENCE_TARGET_KEY_SLOT_X = 80;
    private static final int EXPERIENCE_TARGET_KEY_SLOT_Y = 35;
    private static final int CONFIGURATION_ENTRY_FEE_SLOT = 1;
    private static final int EXPERIENCE_ENTRY_FEE_SLOT_X = 80;
    private static final int EXPERIENCE_ENTRY_FEE_SLOT_Y = 53;
    private static final int EXPERIENCE_SLOT_COUNT = EXPERIENCE_PAYMENT_SLOT_COUNT + CONFIGURATION_SLOT_COUNT;

    // Player inventory layout
    private static final int PLAYER_SLOT_START = EXPERIENCE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_ROW_X = 8;
    private static final int PLAYER_INVENTORY_ROW_Y = 84;

    // Tab layout
    public enum Tab {
        STATUS,
        TARGETS
    }

    private final ContainerLevelAccess containerLevelAccess;

    // Client-side constructor:
    // Uses dummy containers so the menu can be constructed on the client
    // Real state is synced from the server through ContainerData and slot containers.
    public SightseeingExperienceMenu(int containerId, Inventory playerInventory) {
        this(
                containerId,
                playerInventory,
                new SimpleContainer(EXPERIENCE_SLOT_COUNT),
                new SimpleContainerData(AbstractExperienceBlockEntity.DATA_COUNT),
                ContainerLevelAccess.NULL
        );
    }

    // Server-side constructor:
    public SightseeingExperienceMenu(int containerId, Inventory playerInventory, Container experienceContainer, ContainerData data, ContainerLevelAccess access) {
        super(ModMenus.SIGHTSEEING_EXPERIENCE_MENU.get(), containerId, playerInventory, experienceContainer, createConfigurationContainer(experienceContainer), data, access);
        this.containerLevelAccess = access;

        // Add payment slots.
        this.add3x3PaymentSlots(SightseeingExperienceMenu.Tab.STATUS, EXPERIENCE_PAYMENT_SLOT_START_X, EXPERIENCE_PAYMENT_SLOT_START_Y);

        // Add target key slot.
        this.addTargetKeySlot(SightseeingExperienceMenu.Tab.STATUS, CONFIGURATION_TARGET_KEY_SLOT, EXPERIENCE_TARGET_KEY_SLOT_X, EXPERIENCE_TARGET_KEY_SLOT_Y);

        // Add entry fee slot.
        this.addEntryFeeSlot(SightseeingExperienceMenu.Tab.STATUS, CONFIGURATION_ENTRY_FEE_SLOT, EXPERIENCE_ENTRY_FEE_SLOT_X, EXPERIENCE_ENTRY_FEE_SLOT_Y);

        // Add the player inventory slots.
        this.addPlayerInventorySlots(
                menu -> menu.isSelectedTab(SightseeingExperienceMenu.Tab.STATUS),
                PLAYER_INVENTORY_ROW_X,
                PLAYER_INVENTORY_ROW_Y);

        // Add data slots for data sync.
        this.addDataSlots(data);
    }

    private static Container createConfigurationContainer(Container experienceContainer) {
        SimpleContainer configurationContainer = new SimpleContainer(CONFIGURATION_SLOT_COUNT);
        if (experienceContainer instanceof SightseeingExperienceBlockEntity sightseeingExperienceBlockEntity) {
            configurationContainer.setItem(CONFIGURATION_TARGET_KEY_SLOT, sightseeingExperienceBlockEntity.createTargetKey());
            configurationContainer.setItem(CONFIGURATION_ENTRY_FEE_SLOT, sightseeingExperienceBlockEntity.getEntryFee());
        }
        return configurationContainer;
    }

    @Override
    public boolean clickMenuButton(@NonNull Player player, int buttonId) {
        if (buttonId == SightseeingExperienceMenu.Tab.STATUS.ordinal()) {
            this.setSelectedTab(SightseeingExperienceMenu.Tab.STATUS);
            return true;
        }

        if (buttonId == SightseeingExperienceMenu.Tab.TARGETS.ordinal()) {
            this.setSelectedTab(SightseeingExperienceMenu.Tab.TARGETS);
            return true;
        }

        return super.clickMenuButton(player, buttonId);
    }

    @Override
    protected SightseeingExperienceMenu.Tab getDefaultTab() {
        return SightseeingExperienceMenu.Tab.STATUS;
    }

    @Override
    public @NonNull ItemStack quickMoveStack(@NonNull Player player, int slotId) {
        Slot slot = this.slots.get(slotId);

        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack sourceStack = slot.getItem();

        // Player inventory -> experience slot
        if (slotId >= PLAYER_SLOT_START) {
            if (this.isSelectedTab(SightseeingExperienceMenu.Tab.STATUS)) {
                if (!this.moveItemStackTo(sourceStack, 0, EXPERIENCE_PAYMENT_SLOT_COUNT, false)) {
                    return ItemStack.EMPTY;
                }
            }
        }

        return super.quickMoveStack(player, slotId);
    }

    @Override
    public boolean stillValid(@NonNull Player player) {
        return stillValid(this.containerLevelAccess, player, ModBlocks.SIGHTSEEING_EXPERIENCE.get());
    }
}
