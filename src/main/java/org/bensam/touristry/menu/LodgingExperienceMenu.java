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
import org.bensam.touristry.block.entity.LodgingExperienceBlockEntity;
import org.jspecify.annotations.NonNull;

public class LodgingExperienceMenu extends AbstractExperienceMenu<LodgingExperienceMenu.Tab> {
    // Slot layout
    private static final int CONFIGURATION_SLOT_COUNT = 2; // target key + default cost
    private static final int EXPERIENCE_PAYMENT_SLOT_COUNT = LodgingExperienceBlockEntity.PAYMENT_SLOT_SIZE;
    private static final int EXPERIENCE_PAYMENT_SLOT_START_X = 216;
    private static final int EXPERIENCE_PAYMENT_SLOT_START_Y = 17;
    private static final int EXPERIENCE_TARGET_KEY_SLOT = EXPERIENCE_PAYMENT_SLOT_COUNT;
    private static final int CONFIGURATION_TARGET_KEY_SLOT = 0;
    private static final int EXPERIENCE_TARGET_KEY_SLOT_X = 180;
    private static final int EXPERIENCE_TARGET_KEY_SLOT_Y = 35;
    private static final int BED_DEFAULT_COST_SLOT = EXPERIENCE_TARGET_KEY_SLOT + 1;
    private static final int CONFIGURATION_DEFAULT_COST_SLOT = 1;
    private static final int BED_DEFAULT_COST_SLOT_X = 180;
    private static final int BED_DEFAULT_COST_SLOT_Y = 53;
    private static final int EXPERIENCE_SLOT_COUNT = EXPERIENCE_PAYMENT_SLOT_COUNT + CONFIGURATION_SLOT_COUNT;

    // Player inventory layout
    private static final int PLAYER_SLOT_START = EXPERIENCE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_ROW_X = 108;
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
    public LodgingExperienceMenu(int containerId, Inventory playerInventory) {
        this(
                containerId,
                playerInventory,
                new SimpleContainer(EXPERIENCE_SLOT_COUNT),
                new SimpleContainerData(AbstractExperienceBlockEntity.DATA_COUNT),
                ContainerLevelAccess.NULL
        );
    }

    public LodgingExperienceMenu(int containerId, Inventory playerInventory, Container experienceContainer, ContainerData data, ContainerLevelAccess access) {
        super(ModMenus.LODGING_EXPERIENCE_MENU.get(), containerId, playerInventory, experienceContainer, createConfigurationContainer(experienceContainer), data, access);
        this.containerLevelAccess = access;

        // Add payment slots.
        this.add3x3PaymentSlots(Tab.STATUS, EXPERIENCE_PAYMENT_SLOT_START_X, EXPERIENCE_PAYMENT_SLOT_START_Y);

        // Add target key slot.
        this.addTargetKeySlot(Tab.STATUS, CONFIGURATION_TARGET_KEY_SLOT, EXPERIENCE_TARGET_KEY_SLOT_X, EXPERIENCE_TARGET_KEY_SLOT_Y);

        // Add default cost slot.
        this.addSlot(new CloneSlot<>(
                this,
                this.getConfigurationContainer(),
                CONFIGURATION_DEFAULT_COST_SLOT,
                BED_DEFAULT_COST_SLOT_X,
                BED_DEFAULT_COST_SLOT_Y,
                menu -> menu.isSelectedTab(Tab.STATUS)
        ) {
            @Override
            public void setChanged() {
                super.setChanged();
                if (LodgingExperienceMenu.this.getExperienceContainer() instanceof LodgingExperienceBlockEntity lodgingExperienceBlockEntity) {
                    lodgingExperienceBlockEntity.setDefaultCost(this.getItem());
                }
            }
        });

        // Add the player inventory slots.
        this.addPlayerInventorySlots(
                menu -> menu.isSelectedTab(Tab.STATUS),
                PLAYER_INVENTORY_ROW_X,
                PLAYER_INVENTORY_ROW_Y);

        // Add data slots for data sync.
        this.addDataSlots(data);
    }

    private static Container createConfigurationContainer(Container experienceContainer) {
        SimpleContainer configurationContainer = new SimpleContainer(CONFIGURATION_SLOT_COUNT);
        if (experienceContainer instanceof LodgingExperienceBlockEntity lodgingExperienceBlockEntity) {
            configurationContainer.setItem(CONFIGURATION_TARGET_KEY_SLOT, lodgingExperienceBlockEntity.createTargetKey());
            configurationContainer.setItem(CONFIGURATION_DEFAULT_COST_SLOT, lodgingExperienceBlockEntity.getDefaultCost());
        }
        return configurationContainer;
    }

    @Override
    public boolean clickMenuButton(@NonNull Player player, int buttonId) {
        if (buttonId == Tab.STATUS.ordinal()) {
            this.setSelectedTab(Tab.STATUS);
            return true;
        }

        if (buttonId == Tab.TARGETS.ordinal()) {
            this.setSelectedTab(Tab.TARGETS);
            return true;
        }

        return super.clickMenuButton(player, buttonId);
    }

    public ItemStack getDefaultCost() {
        return this.getSlot(BED_DEFAULT_COST_SLOT).getItem();
    }

    public Slot getDefaultCostSlot() {
        return this.getSlot(BED_DEFAULT_COST_SLOT);
    }

    @Override
    protected Tab getDefaultTab() {
        return null;
    }

    public boolean isDefaultCostFree() {
        return !this.getSlot(BED_DEFAULT_COST_SLOT).hasItem();
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
            if (this.isSelectedTab(Tab.STATUS)) {
                if (!this.moveItemStackTo(sourceStack, 0, EXPERIENCE_PAYMENT_SLOT_COUNT, false)) {
                    return ItemStack.EMPTY;
                }
            }
        }

        return super.quickMoveStack(player, slotId);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.containerLevelAccess, player, ModBlocks.LODGING_EXPERIENCE.get());
    }
}
