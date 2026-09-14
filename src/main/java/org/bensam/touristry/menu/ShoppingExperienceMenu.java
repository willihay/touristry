package org.bensam.touristry.menu;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import org.bensam.touristry.block.entity.ShoppingExperienceBlockEntity;
import org.bensam.touristry.network.ExperienceScreenActionC2SPayload;
import org.bensam.touristry.network.SyncItemPricesS2CPayload;
import org.bensam.touristry.tourism.experience.ItemPrice;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class ShoppingExperienceMenu extends AbstractExperienceMenu<ShoppingExperienceMenu.Tab> {
    // Slot layout
    private static final int CONFIGURATION_SLOT_COUNT = 3; // target key + entry fee + default cost
    private static final int EXPERIENCE_PAYMENT_SLOT_COUNT = ShoppingExperienceBlockEntity.PAYMENT_SLOT_SIZE;
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
    public static final int SHOPPING_DEFAULT_COST_SLOT = EXPERIENCE_ENTRY_FEE_SLOT + 1;
    private static final int CONFIGURATION_DEFAULT_COST_SLOT = 2;
    public static final int SHOPPING_DEFAULT_COST_SLOT_X = 216;
    public static final int SHOPPING_DEFAULT_COST_SLOT_Y = 19;
    public static final int SHOPPING_ITEM_FOR_SALE_SLOT = SHOPPING_DEFAULT_COST_SLOT + 1;
    private static final int SHOPPING_ITEM_FOR_SALE_SLOT_X = 162;
    private static final int SHOPPING_ITEM_FOR_SALE_SLOT_Y = 51;
    public static final int SHOPPING_COST_SLOT = SHOPPING_ITEM_FOR_SALE_SLOT + 1;
    public static final int SHOPPING_COST_SLOT_X = 216;
    public static final int SHOPPING_COST_SLOT_Y = 51;
    private static final int EXPERIENCE_SLOT_COUNT = EXPERIENCE_PAYMENT_SLOT_COUNT + CONFIGURATION_SLOT_COUNT + ItemPricingContainer.ITEM_PRICING_SLOTS;

    // Player inventory layout
    private static final int PLAYER_SLOT_START = EXPERIENCE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_ROW_X = 108;
    private static final int PLAYER_INVENTORY_ROW_Y = 84;

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
    public ShoppingExperienceMenu(int containerId, Inventory playerInventory) {
        this(
                containerId,
                playerInventory,
                new SimpleContainer(EXPERIENCE_SLOT_COUNT),
                new SimpleContainerData(AbstractExperienceBlockEntity.DATA_COUNT),
                ContainerLevelAccess.NULL
        );
    }

    // Server-side constructor:
    public ShoppingExperienceMenu(int containerId, Inventory playerInventory, Container experienceContainer, ContainerData data, ContainerLevelAccess access) {
        super(ModMenus.SHOPPING_EXPERIENCE_MENU.get(), containerId, playerInventory, experienceContainer, createConfigurationContainer(experienceContainer), data, access);
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
                SHOPPING_DEFAULT_COST_SLOT_X,
                SHOPPING_DEFAULT_COST_SLOT_Y,
                menu -> menu.isSelectedTab(Tab.PRICING)
        ) {
            @Override
            public void setChanged() {
                super.setChanged();
                if (ShoppingExperienceMenu.this.getExperienceContainer() instanceof ShoppingExperienceBlockEntity shoppingExperienceBlockEntity) {
                    shoppingExperienceBlockEntity.setDefaultCost(this.getItem());
                }
            }
        });

        // Add item pricing slots.
        this.addSlot(new CloneSlot<>(
                this,
                itemPricingContainer,
                ItemPricingContainer.ITEM_FOR_SALE_SLOT,
                SHOPPING_ITEM_FOR_SALE_SLOT_X,
                SHOPPING_ITEM_FOR_SALE_SLOT_Y,
                menu -> menu.isSelectedTab(Tab.PRICING)
        ));

        this.addSlot(new CloneSlot<>(
                this,
                itemPricingContainer,
                ItemPricingContainer.COST_SLOT,
                SHOPPING_COST_SLOT_X,
                SHOPPING_COST_SLOT_Y,
                menu -> menu.isSelectedTab(Tab.PRICING)
        ));

        // Add the player inventory slots.
        this.addPlayerInventorySlots(
                menu -> menu.isSelectedTab(ShoppingExperienceMenu.Tab.STATUS) || menu.isSelectedTab(ShoppingExperienceMenu.Tab.PRICING),
                PLAYER_INVENTORY_ROW_X,
                PLAYER_INVENTORY_ROW_Y);

        // Add data slots for data sync.
        this.addDataSlots(data);
    }

    private static Container createConfigurationContainer(Container experienceContainer) {
        SimpleContainer configurationContainer = new SimpleContainer(CONFIGURATION_SLOT_COUNT);
        if (experienceContainer instanceof ShoppingExperienceBlockEntity shoppingExperienceBlockEntity) {
            configurationContainer.setItem(CONFIGURATION_TARGET_KEY_SLOT, shoppingExperienceBlockEntity.createTargetKey());
            configurationContainer.setItem(CONFIGURATION_ENTRY_FEE_SLOT, shoppingExperienceBlockEntity.getEntryFee());
            configurationContainer.setItem(CONFIGURATION_DEFAULT_COST_SLOT, shoppingExperienceBlockEntity.getDefaultCost());
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

        if (buttonId == Tab.PRICING.ordinal()) {
            this.setSelectedTab(Tab.PRICING);
            return true;
        }

        return super.clickMenuButton(player, buttonId);
    }

    @Override
    protected Tab getDefaultTab() {
        return Tab.STATUS;
    }

    // client-side getter
    public List<ItemPrice> getSyncedItemPrices() {
        return this.syncedItemPrices;
    }

    // client-side getter
    public int getSyncedItemPricesRevision() {
        return this.syncedItemPricesRevision;
    }

    // server-side screen action handler
    @Override
    public void handleScreenAction(ServerPlayer serverPlayer, ExperienceScreenActionC2SPayload payload) {
        this.containerLevelAccess.execute((level, blockPos) -> {
            if (!(level instanceof ServerLevel serverLevel)) {
                return;
            }

            if (!(this.getExperienceContainer() instanceof ShoppingExperienceBlockEntity shoppingExperienceBlockEntity)) {
                return;
            }

            switch (payload.action()) {
                case REQUEST_ITEM_PRICES -> this.syncItemPrices(serverPlayer, shoppingExperienceBlockEntity);

                case IMPORT_ITEMS_FROM_TARGETS -> {
                    // Update item price list by searching all target containers and adding items that are not already defined in the list.
                    int numAdded = shoppingExperienceBlockEntity.importItemsFromTargets(serverLevel);
                    this.syncItemPrices(serverPlayer, shoppingExperienceBlockEntity);
                    this.clearItemPriceSlots();
                    serverPlayer.displayClientMessage(
                            Component.literal("Imported " + numAdded + " new items from target containers"),
                            false
                    );
                }

                case RESET_DEFAULT_COST -> {
                    // Reset default cost of items to original value (i.e. 1 emerald).
                    shoppingExperienceBlockEntity.resetDefaultCost();
                    int newStateId = this.incrementStateId();
                    this.setItem(SHOPPING_DEFAULT_COST_SLOT, newStateId, shoppingExperienceBlockEntity.getDefaultCost().copy());
                }

                case SELECT_ITEM_PRICE -> {
                    // Place selected item pricing in item price slots (e.g. for editing).
                    ItemPrice itemPrice = shoppingExperienceBlockEntity.getItemPrice(payload.primary());
                    if (itemPrice != null) {
                        this.setItemPriceSlots(itemPrice);
                    }
                }

                case ADD_TO_FOR_SALE_QTY -> {
                    if (this.getSlot(SHOPPING_ITEM_FOR_SALE_SLOT).hasItem()) {
                        // Adjust quantity of item in item for sale slot.
                        int qtyToAdd = payload.primary();
                        ItemStack itemForSale = this.getSlot(SHOPPING_ITEM_FOR_SALE_SLOT).getItem().copy();
                        if (itemForSale.getCount() + qtyToAdd > 0) {
                            itemForSale.grow(qtyToAdd);
                            int newStateId = this.incrementStateId();
                            this.setItem(SHOPPING_ITEM_FOR_SALE_SLOT, newStateId, itemForSale.copy());
                        }
                    }
                }

                case ADD_TO_COST_QTY -> {
                    int qtyToAdd = payload.primary();
                    if (this.getSlot(SHOPPING_COST_SLOT).hasItem()) {
                        // Adjust quantity of item in cost slot.
                        ItemStack itemCost = this.getSlot(SHOPPING_COST_SLOT).getItem().copy();
                        if (itemCost.getCount() + qtyToAdd > 0) {
                            itemCost.grow(qtyToAdd);
                            int newStateId = this.incrementStateId();
                            this.setItem(SHOPPING_COST_SLOT, newStateId, itemCost.copy());
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
                    if (this.getSlot(SHOPPING_ITEM_FOR_SALE_SLOT).hasItem()) {
                        // Add a new item price, or update an existing item price from item pricing slots.
                        ItemStack itemForSale = this.getSlot(SHOPPING_ITEM_FOR_SALE_SLOT).getItem();
                        ItemPrice itemPrice = new ItemPrice(
                                itemForSale,
                                this.getSlot(SHOPPING_COST_SLOT).getItem()
                        );
                        shoppingExperienceBlockEntity.updateItemPrice(itemPrice);
                        this.syncItemPrices(serverPlayer, shoppingExperienceBlockEntity);
                    }
                }

                case CLEAR_ITEM_PRICE -> this.clearItemPriceSlots();

                case REMOVE_ITEM_PRICE -> {
                    int index = payload.primary();
                    if (index >= 0 && index < shoppingExperienceBlockEntity.getItemPrices().size()) {
                        ItemPrice itemPrice = shoppingExperienceBlockEntity.getItemPrices().get(index);
                        shoppingExperienceBlockEntity.removeItemPrice(itemPrice);
                        this.syncItemPrices(serverPlayer, shoppingExperienceBlockEntity);
                        this.clearItemPriceSlots();
                    }
                }

                case REMOVE_DEFAULT_ITEM_PRICES -> {
                    // Remove all item prices that are set to default values (i.e. item for sale quantity of 1 and default cost).
                    shoppingExperienceBlockEntity.removeDefaultItemPrices();
                    this.syncItemPrices(serverPlayer, shoppingExperienceBlockEntity);
                    this.clearItemPriceSlots();
                }

                case REMOVE_ALL_ITEM_PRICES -> {
                    shoppingExperienceBlockEntity.removeAllItemPrices();
                    this.syncItemPrices(serverPlayer, shoppingExperienceBlockEntity);
                    this.clearItemPriceSlots();
                }

                default -> super.handleScreenAction(serverPlayer, payload);
            }
        });
    }

    protected void clearItemPriceSlots() {
        int newStateId = this.incrementStateId();

        this.setItem(SHOPPING_ITEM_FOR_SALE_SLOT, newStateId, ItemStack.EMPTY);
        this.setItem(SHOPPING_COST_SLOT, newStateId, ItemStack.EMPTY);
    }

    protected void setItemPriceSlots(ItemPrice itemPrice) {
        int newStateId = this.incrementStateId();

        this.setItem(SHOPPING_ITEM_FOR_SALE_SLOT, newStateId, itemPrice.itemForSale().copy());
        this.setCostSlot(itemPrice.cost(), newStateId);
    }

    public boolean isDefaultCostFree() {
        return !this.getSlot(SHOPPING_DEFAULT_COST_SLOT).hasItem();
    }

    public void onItemForSaleChanged(ItemStack itemForSale) {
        this.syncedItemPricesRevision++;

        this.containerLevelAccess.execute((level, blockPos) -> {
            if (!(level instanceof ServerLevel)) {
                return;
            }

            if (!(this.getExperienceContainer() instanceof ShoppingExperienceBlockEntity shoppingExperienceBlockEntity)) {
                return;
            }

            int newStateId = this.incrementStateId();
            if (itemForSale.isEmpty()) {
                this.setCostSlot(ItemStack.EMPTY, newStateId);
            } else {
                ItemPrice itemPrice = shoppingExperienceBlockEntity.lookupItemPriceFor(itemForSale);
                this.setCostSlot(itemPrice == null ? ItemStack.EMPTY : itemPrice.cost(), newStateId);
            }
        });
    }

    private void setCostSlot(@Nullable ItemStack itemStack, int stateId) {
        if (itemStack == null) {
            this.setItem(SHOPPING_COST_SLOT, stateId, this.getSlot(SHOPPING_DEFAULT_COST_SLOT).getItem().copy());
        } else {
            this.setItem(SHOPPING_COST_SLOT, stateId, itemStack.copy());
        }
    }

    // client-side setter
    public void setSyncedItemPrices(List<ItemPrice> itemPrices) {
        this.syncedItemPrices = List.copyOf(itemPrices);
        this.syncedItemPricesRevision++;
    }

    // server-side sync initiator
    public void syncItemPrices(ServerPlayer serverPlayer, ShoppingExperienceBlockEntity shoppingExperienceBlockEntity) {
        ServerPlayNetworking.send(
                serverPlayer,
                new SyncItemPricesS2CPayload(
                        this.containerId,
                        shoppingExperienceBlockEntity.getItemPrices()
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
            } else if (this.isSelectedTab(Tab.PRICING)) {
                if (!this.moveItemStackTo(sourceStack, SHOPPING_ITEM_FOR_SALE_SLOT, SHOPPING_ITEM_FOR_SALE_SLOT, false)) {
                    return ItemStack.EMPTY;
                }
            }
        }

        return super.quickMoveStack(player, slotId);
    }

    @Override
    public boolean stillValid(@NonNull Player player) {
        return stillValid(this.containerLevelAccess, player, ModBlocks.SHOPPING_EXPERIENCE.get());
    }
}
