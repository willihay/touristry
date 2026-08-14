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
import org.bensam.touristry.block.entity.ShoppingExperienceBlockEntity;
import org.bensam.touristry.network.ExperienceScreenActionC2SPayload;
import org.bensam.touristry.network.SyncItemPricesS2CPayload;
import org.bensam.touristry.network.SyncTargetViewS2CPayload;
import org.bensam.touristry.tourism.ExperienceTargetOverlaySyncManager;
import org.bensam.touristry.tourism.experience.ItemPrice;
import org.bensam.touristry.tourism.experience.TargetView;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class ShoppingExperienceMenu extends AbstractContainerMenu implements TourismStatusMenu {
    // Slot layout
    private static final int EXPERIENCE_PAYMENT_SLOT_COUNT = ShoppingExperienceBlockEntity.PAYMENT_SLOT_SIZE;
    private static final int EXPERIENCE_SLOT_COUNT = ShoppingExperienceBlockEntity.TOTAL_INVENTORY_SIZE + ItemPricingContainer.ITEM_PRICING_SLOTS;
    private static final int EXPERIENCE_PAYMENT_SLOT_START_X = 216;
    private static final int EXPERIENCE_PAYMENT_SLOT_START_Y = 17;
    private static final int EXPERIENCE_TARGET_KEY_SLOT = ShoppingExperienceBlockEntity.TARGET_KEY_INDEX;
    private static final int EXPERIENCE_TARGET_KEY_SLOT_X = 180;
    private static final int EXPERIENCE_TARGET_KEY_SLOT_Y = 35;
    private static final int EXPERIENCE_ENTRY_FEE_SLOT = ShoppingExperienceBlockEntity.ENTRY_FEE_INDEX;
    private static final int EXPERIENCE_ENTRY_FEE_SLOT_X = 180;
    private static final int EXPERIENCE_ENTRY_FEE_SLOT_Y = 53;
    public static final int SHOPPING_DEFAULT_COST_SLOT = ShoppingExperienceBlockEntity.DEFAULT_COST_INDEX;
    public static final int SHOPPING_DEFAULT_COST_SLOT_X = 220;
    public static final int SHOPPING_DEFAULT_COST_SLOT_Y = 19;
    private static final int SHOPPING_ITEM_FOR_SALE_SLOT = ShoppingExperienceBlockEntity.TOTAL_INVENTORY_SIZE;
    private static final int SHOPPING_ITEM_FOR_SALE_SLOT_X = 162;
    private static final int SHOPPING_ITEM_FOR_SALE_SLOT_Y = 51;
    private static final int SHOPPING_COST_SLOT = SHOPPING_ITEM_FOR_SALE_SLOT + 1;
    public static final int SHOPPING_COST_SLOT_X = 220;
    public static final int SHOPPING_COST_SLOT_Y = 51;
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

    protected class TabbedSlot extends Slot {
        private final Predicate<ShoppingExperienceMenu> visibleWhen;

        TabbedSlot(Container container, int containerSlot, int x, int y, Predicate<ShoppingExperienceMenu> visibleWhen) {
            super(container, containerSlot, x, y);
            this.visibleWhen = visibleWhen;
        }

        @Override
        public boolean isActive() {
            return this.visibleWhen.test(ShoppingExperienceMenu.this);
        }
    }

    protected class ShoppingSlot extends TabbedSlot {

        ShoppingSlot(Container container, int containerSlot, int x, int y, Predicate<ShoppingExperienceMenu> visibleWhen) {
            super(container, containerSlot, x, y, visibleWhen);
        }

        @Override
        public boolean mayPlace(ItemStack itemStack) {
            return this.getItem().isEmpty() || ItemStack.isSameItemSameComponents(itemStack, this.getItem());
        }

        @Override
        public ItemStack safeInsert(ItemStack itemStack, int amount) {
            if (!this.mayPlace(itemStack)) {
                return itemStack;
            }

            int copiedCount = Math.min(amount, itemStack.getMaxStackSize());
            this.set(itemStack.copyWithCount(copiedCount));
            return itemStack; // unchanged - player's stack is not consumed
        }

        @Override
        public Optional<ItemStack> tryRemove(int amount, int maxAmount, Player player) {
            ItemStack current = this.getItem();
            if (current.isEmpty()) {
                return Optional.empty();
            }

            int removedCount = Math.min(amount, current.getCount());
            if (removedCount >= current.getCount()) {
                this.set(ItemStack.EMPTY);
            } else {
                this.set(current.copyWithCount(current.getCount() - removedCount));
            }

            return Optional.empty(); // destroy removed stack instead of giving it to the player
        }
    }

    private final ContainerLevelAccess containerLevelAccess;
    private final Container experienceInventory;
    private final ContainerData experienceContainerData;
    private final ItemPricingContainer itemPricingContainer;

    private Tab selectedTab = Tab.STATUS;

    // Client-side snapshot fields:
    private List<ItemPrice> syncedItemPrices = List.of();
    private boolean syncedOrderedTargets = true;
    private List<TargetView> syncedTargets = List.of();

    // Client-side constructor:
    // Uses dummy containers so the menu can be constructed on the client
    // Real state is synced from the server through ContainerData and slot containers.
    public ShoppingExperienceMenu(int containerId, Inventory playerInventory) {
        this(
                containerId,
                playerInventory,
                new SimpleContainer(EXPERIENCE_SLOT_COUNT),
                new SimpleContainerData(ShoppingExperienceBlockEntity.DATA_COUNT),
                ContainerLevelAccess.NULL
        );
    }

    // Server-side constructor:
    public ShoppingExperienceMenu(int containerId, Inventory playerInventory, Container experienceInventory, ContainerData data, ContainerLevelAccess access) {
        super(ModMenus.SHOPPING_EXPERIENCE_MENU.get(), containerId);
        this.experienceInventory = experienceInventory;
        this.experienceContainerData = data;
        this.containerLevelAccess = access;
        this.itemPricingContainer = new ItemPricingContainer(this);

        // Add payment slots.
        this.add3x3GridSlots(this.experienceInventory);

        // Add target key slot.
        this.addSlot(new TabbedSlot(
                this.experienceInventory,
                EXPERIENCE_TARGET_KEY_SLOT,
                EXPERIENCE_TARGET_KEY_SLOT_X,
                EXPERIENCE_TARGET_KEY_SLOT_Y,
                menu -> menu.isSelectedTab(Tab.STATUS)) {
            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return ItemStack.isSameItemSameComponents(itemStack, this.getItem());
            }

            @Override
            public void onTake(Player player, ItemStack itemStack) {
                ShoppingExperienceMenu.this.onKeyTake(player, itemStack);
            }

            @Override
            public ItemStack safeInsert(ItemStack itemStack, int amount) {
                if (!this.mayPlace(itemStack)) {
                    return itemStack;
                }

                // If the target keys are exactly the same, swallow the placed key out of convenience to the player
                // instead of increasing the count here, since this block entity provides infinite target keys.
                itemStack.shrink(itemStack.getCount());
                return itemStack;
            }
        });

        // Add entry fee slot.
        this.addSlot(new ShoppingSlot(
                this.experienceInventory,
                EXPERIENCE_ENTRY_FEE_SLOT,
                EXPERIENCE_ENTRY_FEE_SLOT_X,
                EXPERIENCE_ENTRY_FEE_SLOT_Y,
                menu -> menu.isSelectedTab(Tab.STATUS)
        ));

        // Add default cost slot.
        this.addSlot(new ShoppingSlot(
                this.experienceInventory,
                SHOPPING_DEFAULT_COST_SLOT,
                SHOPPING_DEFAULT_COST_SLOT_X,
                SHOPPING_DEFAULT_COST_SLOT_Y,
                menu -> menu.isSelectedTab(Tab.PRICING)
        ) {
            @Override
            public void setChanged() {
                super.setChanged();
                if (ShoppingExperienceMenu.this.experienceInventory instanceof ShoppingExperienceBlockEntity shoppingExperienceBlockEntity) {
                    shoppingExperienceBlockEntity.setDefaultCost(this.getItem().copy());
                }
            }
        });

        // Add item pricing slots.
        this.addSlot(new ShoppingSlot(
                this.itemPricingContainer,
                0,
                SHOPPING_ITEM_FOR_SALE_SLOT_X,
                SHOPPING_ITEM_FOR_SALE_SLOT_Y,
                menu -> menu.isSelectedTab(Tab.PRICING)
        ));

        this.addSlot(new ShoppingSlot(
                this.itemPricingContainer,
                1,
                SHOPPING_COST_SLOT_X,
                SHOPPING_COST_SLOT_Y,
                menu -> menu.isSelectedTab(Tab.PRICING)
        ));

        // Add the player inventory slots.
        this.addPlayerInventorySlots(playerInventory);

        // Add data slots for data sync.
        this.addDataSlots(this.experienceContainerData);
    }

    protected void add3x3GridSlots(Container container) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slotIndex = col + row * 3;
                this.addSlot(new TabbedSlot(
                        container,
                        slotIndex,
                        EXPERIENCE_PAYMENT_SLOT_START_X + col * SLOT_SIDE_LENGTH,
                        EXPERIENCE_PAYMENT_SLOT_START_Y + row * SLOT_SIDE_LENGTH,
                        menu -> menu.isSelectedTab(Tab.STATUS)));
            }
        }
    }

    protected void addPlayerInventorySlots(Container container) {
        // Add standard 9-col, 3-row inventory.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new TabbedSlot(
                        container,
                        col + (row + 1) * 9,
                        PLAYER_INVENTORY_ROW_X + col * SLOT_SIDE_LENGTH,
                        PLAYER_INVENTORY_ROW_Y + row * SLOT_SIDE_LENGTH,
                        menu -> menu.isSelectedTab(Tab.STATUS) || menu.isSelectedTab(Tab.PRICING)));
            }
        }

        // Add standard 9-col, 1-row hotbar inventory.
        int rowY = ShoppingExperienceMenu.PLAYER_INVENTORY_ROW_Y + 58;
        for (int col = 0; col < 9; col++) {
            this.addSlot(new TabbedSlot(
                    container,
                    col,
                    ShoppingExperienceMenu.PLAYER_INVENTORY_ROW_X + col * SLOT_SIDE_LENGTH,
                    rowY,
                    menu -> menu.isSelectedTab(Tab.STATUS) || menu.isSelectedTab(Tab.PRICING)));
        }
    }

    public static int buttonForTab(Tab tab) {
        return switch (tab) {
            case STATUS -> BUTTON_SELECT_STATUS;
            case TARGETS -> BUTTON_SELECT_TARGETS;
            case PRICING -> BUTTON_SELECT_PRICING;
        };
    }

    @Override
    public boolean clickMenuButton(@NonNull Player player, int buttonId) {
        if (buttonId == BUTTON_TOGGLE_OPEN_FOR_BUSINESS) {
            if (!player.level().isClientSide()) {
                this.toggleOpenForBusiness();
            }
            return true;
        }

        if (buttonId == BUTTON_SELECT_STATUS) {
            this.setSelectedTab(Tab.STATUS);
            return true;
        }

        if (buttonId == BUTTON_SELECT_TARGETS) {
            this.setSelectedTab(Tab.TARGETS);
            return true;
        }

        if (buttonId == BUTTON_SELECT_PRICING) {
            this.setSelectedTab(Tab.PRICING);
            return true;
        }

        return false;
    }

    public int getContainerId() {
        return this.containerId;
    }

    public double getReputation() {
        return (double) this.experienceContainerData.get(ShoppingExperienceBlockEntity.DATA_REPUTATION) / 100;
    }

    // client-side getters
    public List<ItemPrice> getSyncedItemPrices() {
        return this.syncedItemPrices;
    }

    public boolean getSyncedOrderedTargets() {
        return this.syncedOrderedTargets;
    }

    public List<TargetView> getSyncedTargets() {
        return this.syncedTargets;
    }

    // server-side screen action handler
    public void handleScreenAction(ServerPlayer serverPlayer, ExperienceScreenActionC2SPayload payload) {
        this.containerLevelAccess.execute((level, blockPos) -> {
            if (!(level instanceof ServerLevel serverLevel)) {
                return;
            }

            if (!(serverLevel.getBlockEntity(blockPos) instanceof ShoppingExperienceBlockEntity shoppingExperienceBlockEntity)) {
                return;
            }

            switch (payload.action()) {
                case REQUEST_TARGETS -> this.syncTargets(serverPlayer, serverLevel, shoppingExperienceBlockEntity);

                case MOVE_TARGET -> {
                    if (shoppingExperienceBlockEntity.moveTarget(payload.primary(), payload.secondary())) {
                        ExperienceTargetOverlaySyncManager.refreshPlayersHolding(serverLevel, shoppingExperienceBlockEntity.getUUID());
                        this.syncTargets(serverPlayer, serverLevel, shoppingExperienceBlockEntity);
                    }
                }

                case REMOVE_TARGET -> {
                    shoppingExperienceBlockEntity.removeTarget(payload.primary());
                    ExperienceTargetOverlaySyncManager.refreshPlayersHolding(serverLevel, shoppingExperienceBlockEntity.getUUID());
                    this.syncTargets(serverPlayer, serverLevel, shoppingExperienceBlockEntity);
                }

                case REMOVE_ALL_TARGETS -> {
                    shoppingExperienceBlockEntity.removeAllTargets();
                    ExperienceTargetOverlaySyncManager.refreshPlayersHolding(serverLevel, shoppingExperienceBlockEntity.getUUID());
                    this.syncTargets(serverPlayer, serverLevel, shoppingExperienceBlockEntity);
                }

                case SET_ORDERED_TARGETS -> {
                    shoppingExperienceBlockEntity.setOrderedTargets(payload.primary() != 0);
                    this.syncTargets(serverPlayer, serverLevel, shoppingExperienceBlockEntity);
                }

                case REQUEST_ITEM_PRICES -> this.syncItemPrices(serverPlayer, shoppingExperienceBlockEntity);

                case IMPORT_ITEMS_FROM_TARGETS -> {
                    int numAdded = shoppingExperienceBlockEntity.importItemsFromTargets(serverLevel);
                    this.syncItemPrices(serverPlayer, shoppingExperienceBlockEntity);
                    serverPlayer.displayClientMessage(
                            Component.literal("Imported " + numAdded + " new items from target containers"),
                            false
                    );
                }

                case SELECT_ITEM_PRICE -> {
                    ItemPrice itemPrice = shoppingExperienceBlockEntity.getItemPrice(payload.primary());
                    if (itemPrice != null) {
                        int newStateId = this.incrementStateId();

                        this.setItem(SHOPPING_ITEM_FOR_SALE_SLOT, newStateId, itemPrice.itemForSale().copy());
                        if (itemPrice.cost() == null) {
                            this.setItem(SHOPPING_COST_SLOT, newStateId, this.getSlot(SHOPPING_DEFAULT_COST_SLOT).getItem().copy());
                        } else {
                            this.setItem(SHOPPING_COST_SLOT, newStateId, itemPrice.cost().copy());
                        }
                    }
                }

                case ACCEPT_ITEM_PRICE -> {
                    if (this.getSlot(SHOPPING_ITEM_FOR_SALE_SLOT).hasItem()) {
                        ItemStack itemForSale = this.getSlot(SHOPPING_ITEM_FOR_SALE_SLOT).getItem();
                        ItemPrice itemPrice = new ItemPrice(
                                itemForSale.copy(),
                                this.getSlot(SHOPPING_COST_SLOT).getItem().copy()
                        );
                        shoppingExperienceBlockEntity.updateItemPrice(itemPrice);
                        this.syncItemPrices(serverPlayer, shoppingExperienceBlockEntity);
                    }
                }

                case REMOVE_ITEM_PRICE -> {
                    int index = payload.primary();
                    if (index >= 0 && index < shoppingExperienceBlockEntity.getItemPrices().size()) {
                        ItemPrice itemPrice = shoppingExperienceBlockEntity.getItemPrices().get(index);
                        shoppingExperienceBlockEntity.removeItemPrice(itemPrice);
                        this.syncItemPrices(serverPlayer, shoppingExperienceBlockEntity);
                    }
                }

                case REMOVE_ALL_ITEM_PRICES -> {
                    shoppingExperienceBlockEntity.removeAllItemPrices();
                    this.syncItemPrices(serverPlayer, shoppingExperienceBlockEntity);
                }
            }
        });
    }

    public boolean isDefaultCostFree() {
        return !this.getSlot(SHOPPING_DEFAULT_COST_SLOT).hasItem();
    }

    public boolean isOpenForBusiness() {
        return this.experienceContainerData.get(ShoppingExperienceBlockEntity.DATA_OPEN_FOR_BUSINESS) != 0;
    }

    public boolean isSelectedTab(Tab tab) {
        return this.selectedTab == tab;
    }

    public void setSelectedTab(Tab selectedTab) {
        this.selectedTab = selectedTab;
    }

    // client-side setters
    public void setSyncedItemPrices(List<ItemPrice> itemPrices) {
        this.syncedItemPrices = itemPrices;
    }

    public void setSyncedTargets(boolean orderedTargets, List<TargetView> targets) {
        this.syncedOrderedTargets = orderedTargets;
        this.syncedTargets = List.copyOf(targets);
    }

    // server-side sync initiators
    public void syncItemPrices(ServerPlayer serverPlayer, ShoppingExperienceBlockEntity shoppingExperienceBlockEntity) {
        ServerPlayNetworking.send(
                serverPlayer,
                new SyncItemPricesS2CPayload(
                        this.containerId,
                        shoppingExperienceBlockEntity.getItemPrices()
                )
        );
    }

    public void syncTargets(ServerPlayer serverPlayer, ServerLevel serverLevel, AbstractExperienceBlockEntity experienceBlockEntity) {
        ServerPlayNetworking.send(
                serverPlayer,
                new SyncTargetViewS2CPayload(
                        this.containerId,
                        experienceBlockEntity.isTargetListOrdered(),
                        experienceBlockEntity.getTargetViews(serverLevel)
                )
        );
    }

    private void toggleOpenForBusiness() {
        this.containerLevelAccess.execute((level, blockPos) -> {
            if (level.getBlockEntity(blockPos) instanceof ShoppingExperienceBlockEntity shoppingExperienceBlockEntity) {
                shoppingExperienceBlockEntity.setOpenForBusiness(!shoppingExperienceBlockEntity.isOpenForBusiness());
                level.updateNeighbourForOutputSignal(blockPos, level.getBlockState(blockPos).getBlock());
            }
        });
    }

    protected void onKeyTake(Player player, ItemStack itemStack) {
        if (this.experienceInventory instanceof ShoppingExperienceBlockEntity shoppingExperienceBlockEntity) {
            Slot slot = this.slots.get(EXPERIENCE_TARGET_KEY_SLOT);
            slot.set(shoppingExperienceBlockEntity.createTargetKey());
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

        // The experience key slot is intentionally infinite. Returning the copied
        // key here would make Minecraft's quick-move loop keep pulling keys
        // while the refilled slot still matches the returned stack.
        if (slotIndex == EXPERIENCE_TARGET_KEY_SLOT) {
            ItemStack keyToMove = sourceStack.copy();
            keyToMove.setCount(1);

            if (!this.moveItemStackTo(keyToMove, PLAYER_SLOT_START, inventorySize, false)) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, sourceStack);
            return ItemStack.EMPTY;
        }

        if (slotIndex == EXPERIENCE_ENTRY_FEE_SLOT ||
                slotIndex == SHOPPING_DEFAULT_COST_SLOT ||
                slotIndex == SHOPPING_ITEM_FOR_SALE_SLOT ||
                slotIndex == SHOPPING_COST_SLOT
        ) {
            slot.setByPlayer(ItemStack.EMPTY);
            slot.setChanged();
            return ItemStack.EMPTY;
        }

        // Experience slot -> player inventory
        if (slotIndex < EXPERIENCE_SLOT_COUNT) {
            if (!this.moveItemStackTo(sourceStack, PLAYER_SLOT_START, inventorySize, false)) {
                return ItemStack.EMPTY;
            }
        }
        // Player inventory -> experience slot
        else {
            if (this.selectedTab == Tab.STATUS) {
                if (!this.moveItemStackTo(sourceStack, 0, EXPERIENCE_PAYMENT_SLOT_COUNT, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (this.selectedTab == Tab.PRICING) {
                if (!this.moveItemStackTo(sourceStack, SHOPPING_ITEM_FOR_SALE_SLOT, SHOPPING_ITEM_FOR_SALE_SLOT, false)) {
                    return ItemStack.EMPTY;
                }
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
        return stillValid(this.containerLevelAccess, player, ModBlocks.SHOPPING_EXPERIENCE.get());
    }
}
