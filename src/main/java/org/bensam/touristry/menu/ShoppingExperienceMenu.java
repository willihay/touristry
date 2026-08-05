package org.bensam.touristry.menu;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
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
import org.bensam.touristry.network.SyncTargetViewS2CPayload;
import org.bensam.touristry.tourism.ExperienceTargetOverlaySyncManager;
import org.bensam.touristry.tourism.experience.TargetView;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class ShoppingExperienceMenu extends AbstractContainerMenu implements TourismStatusMenu {
    // Slot layout
    private static final int EXPERIENCE_PAYMENT_SLOT_COUNT = ShoppingExperienceBlockEntity.PAYMENT_SLOT_SIZE;
    private static final int EXPERIENCE_SLOT_COUNT = ShoppingExperienceBlockEntity.TOTAL_INVENTORY_SIZE;
    private static final int EXPERIENCE_PAYMENT_SLOT_START_X = 216;
    private static final int EXPERIENCE_PAYMENT_SLOT_START_Y = 17;
    private static final int EXPERIENCE_TARGET_KEY_SLOT = ShoppingExperienceBlockEntity.TARGET_KEY_INDEX;
    private static final int EXPERIENCE_TARGET_KEY_SLOT_X = 180;
    private static final int EXPERIENCE_TARGET_KEY_SLOT_Y = 35;
    private static final int EXPERIENCE_ENTRY_FEE_SLOT = ShoppingExperienceBlockEntity.ENTRY_FEE_INDEX;
    private static final int EXPERIENCE_ENTRY_FEE_SLOT_X = 180;
    private static final int EXPERIENCE_ENTRY_FEE_SLOT_Y = 53;
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

    private class TabbedSlot extends Slot {
        private final Predicate<ShoppingExperienceMenu> visibleWhen;

        TabbedSlot(Container container, int slot, int x, int y, Predicate<ShoppingExperienceMenu> visibleWhen) {
            super(container, slot, x, y);
            this.visibleWhen = visibleWhen;
        }

        @Override
        public boolean isActive() {
            return this.visibleWhen.test(ShoppingExperienceMenu.this);
        }
    }

    private final Container experienceInventory;
    private final ContainerData experienceContainerData;
    private final ContainerLevelAccess containerLevelAccess;

    private Tab selectedTab = Tab.STATUS;

    // Client-side snapshot fields:
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

        // Add payment slots.
        this.add3x3GridSlots(this.experienceInventory, EXPERIENCE_PAYMENT_SLOT_START_X, EXPERIENCE_PAYMENT_SLOT_START_Y);

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
        this.addSlot(new TabbedSlot(
                this.experienceInventory,
                EXPERIENCE_ENTRY_FEE_SLOT,
                EXPERIENCE_ENTRY_FEE_SLOT_X,
                EXPERIENCE_ENTRY_FEE_SLOT_Y,
                menu -> menu.isSelectedTab(Tab.STATUS)) {
            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return !itemStack.isEmpty();
            }

            @Override
            public int getMaxStackSize(ItemStack itemStack) {
                return itemStack.getMaxStackSize();
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

            @Override
            public void onTake(Player player, ItemStack itemStack) {
                this.setChanged();
            }
        });

        // Add the player inventory slots.
        this.addPlayerInventorySlots(playerInventory, PLAYER_INVENTORY_ROW_X, PLAYER_INVENTORY_ROW_Y);

        // Add data slots for data sync.
        this.addDataSlots(this.experienceContainerData);
    }

    protected void add3x3GridSlots(Container container, int firstSlotX, int firstSlotY) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slotIndex = col + row * 3;
                this.addSlot(new TabbedSlot(
                        container,
                        slotIndex,
                        firstSlotX + col * SLOT_SIDE_LENGTH,
                        firstSlotY + row * SLOT_SIDE_LENGTH,
                        menu -> menu.isSelectedTab(Tab.STATUS)));
            }
        }
    }

    protected void addPlayerInventorySlots(Container container, int i, int j) {
        // Add standard 3-row inventory.
        for (int k = 0; k < 3; k++) {
            for (int l = 0; l < 9; l++) {
                this.addSlot(new TabbedSlot(
                        container,
                        l + (k + 1) * 9,
                        i + l * 18,
                        j + k * 18,
                        menu -> menu.isSelectedTab(Tab.STATUS) || menu.isSelectedTab(Tab.PRICING)));
            }
        }

        // Add hotbar inventory.
        int n = j + 58;
        for (int m = 0; m < 9; m++) {
            this.addSlot(new TabbedSlot(
                    container,
                    m,
                    i + m * 18,
                    n,
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

    // client-side getter
    public boolean getSyncedOrderedTargets() {
        return this.syncedOrderedTargets;
    }

    // client-side getter
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
            }
        });
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

    // client-side setter
    public void setSyncedTargets(boolean orderedTargets, List<TargetView> targets) {
        this.syncedOrderedTargets = orderedTargets;
        this.syncedTargets = List.copyOf(targets);
    }

    // server-side sync initiator
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

        if (slotIndex == EXPERIENCE_ENTRY_FEE_SLOT) {
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
        // Player inventory -> payment slots only
        else {
            if (!this.moveItemStackTo(sourceStack, 0, EXPERIENCE_PAYMENT_SLOT_COUNT, false)) {
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
        return stillValid(this.containerLevelAccess, player, ModBlocks.SHOPPING_EXPERIENCE.get());
    }
}
