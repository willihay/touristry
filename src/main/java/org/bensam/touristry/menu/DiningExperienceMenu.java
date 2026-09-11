package org.bensam.touristry.menu;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import org.bensam.touristry.block.entity.DiningExperienceBlockEntity;

public class DiningExperienceMenu extends AbstractExperienceMenu<DiningExperienceMenu.Tab> {
    // Slot layout
    private static final int EXPERIENCE_PAYMENT_SLOT_COUNT = DiningExperienceBlockEntity.PAYMENT_SLOT_SIZE;
    private static final int EXPERIENCE_SLOT_COUNT = DiningExperienceBlockEntity.TOTAL_INVENTORY_SIZE + ItemPricingContainer.ITEM_PRICING_SLOTS;
    private static final int EXPERIENCE_PAYMENT_SLOT_START_X = 216;
    private static final int EXPERIENCE_PAYMENT_SLOT_START_Y = 17;
    private static final int EXPERIENCE_TARGET_KEY_SLOT_X = 180;
    private static final int EXPERIENCE_TARGET_KEY_SLOT_Y = 35;
    private static final int EXPERIENCE_ENTRY_FEE_SLOT_X = 180;
    private static final int EXPERIENCE_ENTRY_FEE_SLOT_Y = 53;
    public static final int MENU_DEFAULT_COST_SLOT_X = 220;
    public static final int MENU_DEFAULT_COST_SLOT_Y = 19;
    private static final int MENU_ITEM_FOR_SALE_SLOT_X = 162;
    private static final int MENU_ITEM_FOR_SALE_SLOT_Y = 51;
    public static final int MENU_ITEM_COST_SLOT_X = 220;
    public static final int MENU_ITEM_COST_SLOT_Y = 51;
    private static final int SLOT_SIDE_LENGTH = 18;

    // Player inventory layout
    private static final int PLAYER_INVENTORY_ROW_X = 108;
    private static final int PLAYER_INVENTORY_ROW_Y = 84;
    private static final int PLAYER_SLOT_START = EXPERIENCE_SLOT_COUNT;

    public enum Tab {
        STATUS,
        TARGETS,
        PRICING
    }

    protected DiningExperienceMenu(MenuType<?> menuType, int containerId, Inventory playerInventory, Container experienceInventory, ContainerData data, ContainerLevelAccess access) {
        super(menuType, containerId, playerInventory, experienceInventory, data, access);
    }

    @Override
    protected Tab getDefaultTab() {
        return Tab.STATUS;
    }

    @Override
    public boolean stillValid(Player player) {
        return false;
    }
}
