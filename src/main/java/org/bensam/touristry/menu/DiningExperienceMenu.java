package org.bensam.touristry.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import org.bensam.touristry.block.entity.AbstractExperienceBlockEntity;
import org.bensam.touristry.block.entity.DiningExperienceBlockEntity;
import org.bensam.touristry.tourism.experience.ItemPrice;

import java.util.List;

public class DiningExperienceMenu extends AbstractExperienceMenu<DiningExperienceMenu.Tab> {
    // Slot layout
    private static final int CONFIGURATION_SLOT_COUNT = 3; // target key + entry fee + menu item default cost
    private static final int EXPERIENCE_PAYMENT_SLOT_COUNT = DiningExperienceBlockEntity.PAYMENT_SLOT_SIZE;
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
    private static final int EXPERIENCE_SLOT_COUNT = EXPERIENCE_PAYMENT_SLOT_COUNT + CONFIGURATION_SLOT_COUNT + ItemPricingContainer.ITEM_PRICING_SLOTS;

    // Player inventory layout
    private static final int PLAYER_INVENTORY_ROW_X = 108;
    private static final int PLAYER_INVENTORY_ROW_Y = 84;
    private static final int PLAYER_SLOT_START = EXPERIENCE_SLOT_COUNT;

    // Tab layout
    public enum Tab {
        STATUS,
        TARGETS,
        PRICING
    }

    private final ContainerLevelAccess containerLevelAccess;

    // Client-side snapshot fields:
    private int syncedItemPricesRevision;
    private List<ItemPrice> syncedItemPrices = List.of();

    // Client-side constructor:
    // Uses dummy containers so the menu can be constructed on the client
    // Real state is synced from the server through ContainerData and slot containers.
    /*
    public DiningExperienceMenu(int containerId, Inventory playerInventory) {
        this(
                containerId,
                playerInventory,
                new SimpleContainer(EXPERIENCE_SLOT_COUNT),
                new SimpleContainerData(AbstractExperienceBlockEntity.DATA_COUNT),
                ContainerLevelAccess.NULL
        );
    }
     */

    protected DiningExperienceMenu(MenuType<?> menuType, int containerId, Inventory playerInventory, Container experienceInventory, ContainerData data, ContainerLevelAccess access) {
        super(menuType, containerId, playerInventory, experienceInventory, new SimpleContainer(CONFIGURATION_SLOT_COUNT), data, access);
        this.containerLevelAccess = access;
        //ItemPricingContainer itemPricingContainer = new ItemPricingContainer(this);
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
