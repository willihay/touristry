package org.bensam.touristry.menu;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.bensam.touristry.ModBlocks;
import org.bensam.touristry.ModMenus;
import org.bensam.touristry.block.entity.AbstractExperienceBlockEntity;
import org.bensam.touristry.block.entity.DiningExperienceBlockEntity;
import org.bensam.touristry.network.ExperienceScreenActionC2SPayload;
import org.bensam.touristry.network.SyncItemPricesS2CPayload;
import org.bensam.touristry.tourism.experience.ItemPrice;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class DiningExperienceMenu extends AbstractExperienceMenu<DiningExperienceMenu.Tab> implements PricingMenu {
    // Slot layout
    private static final int CONFIGURATION_SLOT_COUNT = 3; // target key + entry fee + default cost
    private static final int EXPERIENCE_PAYMENT_SLOT_COUNT = DiningExperienceBlockEntity.PAYMENT_SLOT_SIZE;
    private static final int EXPERIENCE_PAYMENT_SLOT_START_X = 216;
    private static final int EXPERIENCE_PAYMENT_SLOT_START_Y = 17;
    private static final int EXPERIENCE_TARGET_KEY_SLOT = EXPERIENCE_PAYMENT_SLOT_COUNT;
    private static final int CONFIGURATION_TARGET_KEY_SLOT = 0;
    private static final int EXPERIENCE_TARGET_KEY_SLOT_X = 180;
    private static final int EXPERIENCE_TARGET_KEY_SLOT_Y = 35;
    private static final int EXPERIENCE_ENTRY_FEE_SLOT = EXPERIENCE_TARGET_KEY_SLOT + 1;
    private static final int CONFIGURATION_ENTRY_FEE_SLOT = 1;
    private static final int EXPERIENCE_ENTRY_FEE_SLOT_X = 180;
    private static final int EXPERIENCE_ENTRY_FEE_SLOT_Y = 53;
    private static final int MENU_DEFAULT_COST_SLOT = EXPERIENCE_ENTRY_FEE_SLOT + 1;
    private static final int CONFIGURATION_DEFAULT_COST_SLOT = 2;
    private static final int MENU_DEFAULT_COST_SLOT_X = 216;
    private static final int MENU_DEFAULT_COST_SLOT_Y = 19;
    private static final int MENU_ITEM_FOR_SALE_SLOT = MENU_DEFAULT_COST_SLOT + 1;
    private static final int MENU_ITEM_FOR_SALE_SLOT_X = 162;
    private static final int MENU_ITEM_FOR_SALE_SLOT_Y = 51;
    private static final int MENU_ITEM_COST_SLOT = MENU_ITEM_FOR_SALE_SLOT + 1;
    private static final int MENU_ITEM_COST_SLOT_X = 216;
    private static final int MENU_ITEM_COST_SLOT_Y = 51;
    private static final int EXPERIENCE_SLOT_COUNT = EXPERIENCE_PAYMENT_SLOT_COUNT + CONFIGURATION_SLOT_COUNT + ItemPricingContainer.ITEM_PRICING_SLOTS;

    // Player inventory layout
    private static final int PLAYER_SLOT_START = EXPERIENCE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_ROW_X = 108;
    private static final int PLAYER_INVENTORY_ROW_Y = 84;

    // Tab layout
    public enum Tab {
        STATUS,
        TARGETS,
        MENU
    }

    private final ContainerLevelAccess containerLevelAccess;

    // Client-side snapshot fields:
    private int syncedMenuPricesRevision;
    private List<ItemPrice> syncedMenuPrices = List.of();

    // Client-side constructor:
    // Uses dummy containers so the menu can be constructed on the client
    // Real state is synced from the server through ContainerData and slot containers.
    public DiningExperienceMenu(int containerId, Inventory playerInventory) {
        this(
                containerId,
                playerInventory,
                new SimpleContainer(EXPERIENCE_SLOT_COUNT),
                new SimpleContainerData(AbstractExperienceBlockEntity.DATA_COUNT),
                ContainerLevelAccess.NULL
        );
    }

    public DiningExperienceMenu(int containerId, Inventory playerInventory, Container experienceContainer, ContainerData data, ContainerLevelAccess access) {
        super(ModMenus.DINING_EXPERIENCE_MENU.get(), containerId, playerInventory, experienceContainer, createConfigurationContainer(experienceContainer), data, access);
        this.containerLevelAccess = access;
        ItemPricingContainer itemPricingContainer = new ItemPricingContainer(this);

        // Add payment slots.
        this.add3x3PaymentSlots(Tab.STATUS, EXPERIENCE_PAYMENT_SLOT_START_X, EXPERIENCE_PAYMENT_SLOT_START_Y);

        // Add target key slot.
        this.addTargetKeySlot(Tab.STATUS, CONFIGURATION_TARGET_KEY_SLOT, EXPERIENCE_TARGET_KEY_SLOT_X, EXPERIENCE_TARGET_KEY_SLOT_Y);

        // Add entry fee slot.
        this.addEntryFeeSlot(Tab.STATUS, CONFIGURATION_ENTRY_FEE_SLOT, EXPERIENCE_ENTRY_FEE_SLOT_X, EXPERIENCE_ENTRY_FEE_SLOT_Y);

        // Add default cost slot.
        this.addSlot(new CloneSlot<>(
                this,
                this.getConfigurationContainer(),
                CONFIGURATION_DEFAULT_COST_SLOT,
                MENU_DEFAULT_COST_SLOT_X,
                MENU_DEFAULT_COST_SLOT_Y,
                menu -> menu.isSelectedTab(Tab.MENU)
        ) {
            @Override
            public void setChanged() {
                super.setChanged();
                if (DiningExperienceMenu.this.getExperienceContainer() instanceof DiningExperienceBlockEntity diningExperienceBlockEntity) {
                    diningExperienceBlockEntity.setDefaultCost(this.getItem());
                }
            }
        });

        // Add menu pricing slots.
        this.addSlot(new CloneSlot<>(
                this,
                itemPricingContainer,
                ItemPricingContainer.ITEM_FOR_SALE_SLOT,
                MENU_ITEM_FOR_SALE_SLOT_X,
                MENU_ITEM_FOR_SALE_SLOT_Y,
                menu -> menu.isSelectedTab(Tab.MENU)
        ));

        this.addSlot(new CloneSlot<>(
                this,
                itemPricingContainer,
                ItemPricingContainer.COST_SLOT,
                MENU_ITEM_COST_SLOT_X,
                MENU_ITEM_COST_SLOT_Y,
                menu -> menu.isSelectedTab(Tab.MENU)
        ));

        // Add the player inventory slots.
        this.addPlayerInventorySlots(
                menu -> menu.isSelectedTab(Tab.STATUS) || menu.isSelectedTab(Tab.MENU),
                PLAYER_INVENTORY_ROW_X,
                PLAYER_INVENTORY_ROW_Y);

        // Add data slots for data sync.
        this.addDataSlots(data);
    }

    private static Container createConfigurationContainer(Container experienceContainer) {
        SimpleContainer configurationContainer = new SimpleContainer(CONFIGURATION_SLOT_COUNT);
        if (experienceContainer instanceof DiningExperienceBlockEntity diningExperienceBlockEntity) {
            configurationContainer.setItem(CONFIGURATION_TARGET_KEY_SLOT, diningExperienceBlockEntity.createTargetKey());
            configurationContainer.setItem(CONFIGURATION_ENTRY_FEE_SLOT, diningExperienceBlockEntity.getEntryFee());
            configurationContainer.setItem(CONFIGURATION_DEFAULT_COST_SLOT, diningExperienceBlockEntity.getDefaultCost());
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

        if (buttonId == Tab.MENU.ordinal()) {
            this.setSelectedTab(Tab.MENU);
            return true;
        }

        return super.clickMenuButton(player, buttonId);
    }

    @Override
    public ItemStack getDefaultCost() {
        return this.getSlot(MENU_DEFAULT_COST_SLOT).getItem();
    }

    @Override
    public Slot getDefaultCostSlot() {
        return this.getSlot(MENU_DEFAULT_COST_SLOT);
    }

    @Override
    protected Tab getDefaultTab() {
        return Tab.STATUS;
    }

    @Override
    public ItemStack getFocusedItemForSale() {
        return this.getSlot(MENU_ITEM_FOR_SALE_SLOT).getItem();
    }

    @Override
    public Slot getFocusedItemForSaleSlot() {
        return this.getSlot(MENU_ITEM_FOR_SALE_SLOT);
    }

    @Override
    public ItemStack getFocusedItemCost() {
        return this.getSlot(MENU_ITEM_COST_SLOT).getItem();
    }

    @Override
    public Slot getFocusedItemCostSlot() {
        return this.getSlot(MENU_ITEM_COST_SLOT);
    }

    // client-side getter
    @Override
    public List<ItemPrice> getSyncedItemPrices() {
        return this.syncedMenuPrices;
    }

    // client-side getter
    @Override
    public int getSyncedItemPricesRevision() {
        return this.syncedMenuPricesRevision;
    }

    // server-side screen action handler
    @Override
    public void handleScreenAction(ServerPlayer serverPlayer, ExperienceScreenActionC2SPayload payload) {
        this.containerLevelAccess.execute((level, blockPos) -> {
            if (!(level instanceof ServerLevel serverLevel)) {
                return;
            }

            if (!(this.getExperienceContainer() instanceof DiningExperienceBlockEntity diningExperienceBlockEntity)) {
                return;
            }

            switch (payload.action()) {
                case REQUEST_ITEM_PRICES -> this.syncItemPrices(serverPlayer, diningExperienceBlockEntity);

                case IMPORT_ITEMS_FROM_TARGETS -> {
                    // Update menu price list by searching all target containers and adding items that are not already defined in the list.
                    int numAdded = diningExperienceBlockEntity.importItemsFromTargets(serverLevel);
                    this.syncItemPrices(serverPlayer, diningExperienceBlockEntity);
                    this.clearItemPriceSlots();
                    serverPlayer.displayClientMessage(
                            Component.literal("Imported " + numAdded + " new menu items from target containers"),
                            false
                    );
                }

                case RESET_DEFAULT_COST -> {
                    // Reset default cost of items to original value (i.e. 1 emerald).
                    diningExperienceBlockEntity.resetDefaultCost();
                    int newStateId = this.incrementStateId();
                    this.setItem(MENU_DEFAULT_COST_SLOT, newStateId, diningExperienceBlockEntity.getDefaultCost().copy());
                }

                case SELECT_ITEM_PRICE -> {
                    // Place selected menu item pricing in menu item price slots (e.g. for editing).
                    ItemPrice itemPrice = diningExperienceBlockEntity.getMenuPrice(payload.primary());
                    if (itemPrice != null) {
                        this.setItemPriceSlots(itemPrice);
                    }
                }

                case ADD_TO_FOR_SALE_QTY -> {
                    if (this.getSlot(MENU_ITEM_FOR_SALE_SLOT).hasItem()) {
                        // Adjust quantity of menu item in item for sale slot.
                        int qtyToAdd = payload.primary();
                        ItemStack itemForSale = this.getSlot(MENU_ITEM_FOR_SALE_SLOT).getItem().copy();
                        if (itemForSale.getCount() + qtyToAdd > 0) {
                            itemForSale.grow(qtyToAdd);
                            int newStateId = this.incrementStateId();
                            this.setItem(MENU_ITEM_FOR_SALE_SLOT, newStateId, itemForSale.copy());
                        }
                    }
                }

                case ADD_TO_COST_QTY -> {
                    int qtyToAdd = payload.primary();
                    if (this.getSlot(MENU_ITEM_COST_SLOT).hasItem()) {
                        // Adjust quantity of menu item in cost slot.
                        ItemStack itemCost = this.getSlot(MENU_ITEM_COST_SLOT).getItem().copy();
                        if (itemCost.getCount() + qtyToAdd > 0) {
                            itemCost.grow(qtyToAdd);
                            int newStateId = this.incrementStateId();
                            this.setItem(MENU_ITEM_COST_SLOT, newStateId, itemCost.copy());
                        }
                    } else {
                        if (qtyToAdd > 0) {
                            // Update cost from 'free' to default cost.
                            int newStateId = this.incrementStateId();
                            this.setCostSlot(null, newStateId);
                        }
                    }
                }

                case ACCEPT_ITEM_PRICE -> {
                    if (this.getSlot(MENU_ITEM_FOR_SALE_SLOT).hasItem()) {
                        // Add a new menu item price, or update an existing menu item price from item pricing slots.
                        ItemStack itemForSale = this.getSlot(MENU_ITEM_FOR_SALE_SLOT).getItem();
                        ItemPrice itemPrice = new ItemPrice(
                                itemForSale,
                                this.getSlot(MENU_ITEM_COST_SLOT).getItem()
                        );
                        diningExperienceBlockEntity.updateMenuPrice(itemPrice);
                        this.syncItemPrices(serverPlayer, diningExperienceBlockEntity);
                    }
                }

                case CLEAR_ITEM_PRICE -> this.clearItemPriceSlots();

                case REMOVE_ITEM_PRICE -> {
                    int index = payload.primary();
                    if (index >= 0 && index < diningExperienceBlockEntity.getMenuPrices().size()) {
                        ItemPrice itemPrice = diningExperienceBlockEntity.getMenuPrices().get(index);
                        diningExperienceBlockEntity.removeMenuPrice(itemPrice);
                        this.syncItemPrices(serverPlayer, diningExperienceBlockEntity);
                        this.clearItemPriceSlots();
                    }
                }

                case REMOVE_DEFAULT_ITEM_PRICES -> {
                    // Remove all item prices that are set to default values (i.e. item for sale quantity of 1 and default cost).
                    diningExperienceBlockEntity.removeDefaultMenuPrices();
                    this.syncItemPrices(serverPlayer, diningExperienceBlockEntity);
                    this.clearItemPriceSlots();
                }

                case REMOVE_ALL_ITEM_PRICES -> {
                    diningExperienceBlockEntity.removeAllMenuPrices();
                    this.syncItemPrices(serverPlayer, diningExperienceBlockEntity);
                    this.clearItemPriceSlots();
                }

                default -> super.handleScreenAction(serverPlayer, payload);
            }
        });
    }

    protected void clearItemPriceSlots() {
        int newStateId = this.incrementStateId();

        this.setItem(MENU_ITEM_FOR_SALE_SLOT, newStateId, ItemStack.EMPTY);
        this.setItem(MENU_ITEM_COST_SLOT, newStateId, ItemStack.EMPTY);
    }

    protected void setItemPriceSlots(ItemPrice itemPrice) {
        int newStateId = this.incrementStateId();

        this.setItem(MENU_ITEM_FOR_SALE_SLOT, newStateId, itemPrice.itemForSale().copy());
        this.setCostSlot(itemPrice.cost(), newStateId);
    }

    @Override
    public boolean isDefaultCostFree() {
        return !this.getSlot(MENU_DEFAULT_COST_SLOT).hasItem();
    }

    @Override
    public void onItemForSaleChanged(ItemStack itemForSale) {
        this.syncedMenuPricesRevision++;

        this.containerLevelAccess.execute((level, blockPos) -> {
            if (!(level instanceof ServerLevel)) {
                return;
            }

            if (!(this.getExperienceContainer() instanceof DiningExperienceBlockEntity diningExperienceBlockEntity)) {
                return;
            }

            int newStateId = this.incrementStateId();
            if (itemForSale.isEmpty()) {
                this.setCostSlot(ItemStack.EMPTY, newStateId);
            } else {
                ItemPrice itemPrice = diningExperienceBlockEntity.lookupItemPriceFor(itemForSale);
                this.setCostSlot(itemPrice == null ? ItemStack.EMPTY : itemPrice.cost(), newStateId);
            }
        });
    }

    private void setCostSlot(@Nullable ItemStack itemStack, int stateId) {
        if (itemStack == null) {
            this.setItem(MENU_ITEM_COST_SLOT, stateId, this.getSlot(MENU_DEFAULT_COST_SLOT).getItem().copy());
        } else {
            this.setItem(MENU_ITEM_COST_SLOT, stateId, itemStack.copy());
        }
    }

    // client-side setter
    @Override
    public void setSyncedItemPrices(List<ItemPrice> itemPrices) {
        this.syncedMenuPrices = List.copyOf(itemPrices);
        this.syncedMenuPricesRevision++;
    }

    // server-side sync initiator
    protected void syncItemPrices(ServerPlayer serverPlayer, DiningExperienceBlockEntity diningExperienceBlockEntity) {
        ServerPlayNetworking.send(
                serverPlayer,
                new SyncItemPricesS2CPayload(
                        this.containerId,
                        diningExperienceBlockEntity.getMenuPrices()
                )
        );
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
            } else if (this.isSelectedTab(Tab.MENU)) {
                if (!this.moveItemStackTo(sourceStack, MENU_ITEM_FOR_SALE_SLOT, MENU_ITEM_FOR_SALE_SLOT, false)) {
                    return ItemStack.EMPTY;
                }
            }
        }

        return super.quickMoveStack(player, slotId);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.containerLevelAccess, player, ModBlocks.DINING_EXPERIENCE.get());
    }
}
