package org.bensam.touristry.menu;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.bensam.touristry.block.entity.AbstractExperienceBlockEntity;
import org.bensam.touristry.network.ExperienceScreenActionC2SPayload;
import org.bensam.touristry.network.SyncTargetViewS2CPayload;
import org.bensam.touristry.tourism.ExperienceTargetOverlaySyncManager;
import org.bensam.touristry.tourism.experience.TargetView;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.function.Predicate;

public abstract class AbstractExperienceMenu<T extends Enum<T>> extends AbstractContainerMenu implements TourismMenu {
    private static final int SLOT_SIDE_LENGTH = 18;

    private final ContainerLevelAccess containerLevelAccess;
    private final Container experienceInventory;
    private final ContainerData experienceContainerData;
    private final Container playerInventory;
    private T selectedTab;
    private int targetKeySlotId;
    private int playerInventorySlotStartId;

    // Client-side snapshot fields:
    private boolean syncedOrderedTargets = true;
    private List<TargetView> syncedTargets = List.of();

    // Server-side constructor:
    protected AbstractExperienceMenu(
            MenuType<?> menuType,
            int containerId,
            Inventory playerInventory,
            Container experienceInventory,
            ContainerData data,
            ContainerLevelAccess access
    ) {
        super(menuType, containerId);
        this.containerLevelAccess = access;
        this.experienceInventory = experienceInventory;
        this.experienceContainerData = data;
        this.playerInventory = playerInventory;
        this.selectedTab = this.getDefaultTab();
        this.targetKeySlotId = -1;
    }

    protected void add3x3PaymentSlots(T tab, int x, int y) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slotId = col + row * 3;
                this.addSlot(new TabbedMenuSlot<>(
                        this,
                        this.experienceInventory,
                        slotId,
                        x + col * SLOT_SIDE_LENGTH,
                        y + row * SLOT_SIDE_LENGTH,
                        menu -> menu.isSelectedTab(tab)));
            }
        }
    }

    protected void addPlayerInventorySlots(Predicate<AbstractExperienceMenu<T>> visibleWhen, int globalSlotStart, int x, int y) {
        this.playerInventorySlotStartId = globalSlotStart;

        // Add standard 9-col, 3-row inventory.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new TabbedMenuSlot<>(
                        this,
                        this.playerInventory,
                        col + (row + 1) * 9,
                        x + col * SLOT_SIDE_LENGTH,
                        y + row * SLOT_SIDE_LENGTH,
                        visibleWhen));
            }
        }

        // Add standard 9-col, 1-row hotbar inventory.
        int rowY = y + 58;
        for (int col = 0; col < 9; col++) {
            this.addSlot(new TabbedMenuSlot<>(
                    this,
                    this.playerInventory,
                    col,
                    x + col * SLOT_SIDE_LENGTH,
                    rowY,
                    visibleWhen));
        }
    }

    protected void addTargetKeySlot(T tab, int globalSlotId, int x, int y) {
        this.targetKeySlotId = globalSlotId;

        this.addSlot(new TabbedMenuSlot<>(
                this,
                this.experienceInventory,
                globalSlotId,
                x,
                y,
                menu -> menu.isSelectedTab(tab)) {
            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return ItemStack.isSameItemSameComponents(itemStack, this.getItem());
            }

            @Override
            public void onTake(Player player, ItemStack itemStack) {
                AbstractExperienceMenu.this.onKeyTake();
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
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < this.slots.size()) {
            Slot slot = this.getSlot(slotId);

            if (slot instanceof CloneSlot<?> cloneSlot) {
                if (clickType == ClickType.PICKUP && (button == 0 || button == 1)) {
                    this.handleCloneItemPickup(cloneSlot, button, player);
                    return;
                }

                if (clickType == ClickType.SWAP && ((button >= 0 && button < 9) || button == 40 /* 40 = offhand slot */)) {
                    this.handleCloneItemSwap(cloneSlot, button, player);
                    return;
                }
            }
        }
        super.clicked(slotId, button, clickType, player);
    }

    public int getContainerId() {
        return this.containerId;
    }

    protected abstract T getDefaultTab();

    protected Container getExperienceInventory() {
        return this.experienceInventory;
    }

    public double getReputation() {
        return (double) this.experienceContainerData.get(AbstractExperienceBlockEntity.DATA_REPUTATION) / 100;
    }

    // client-side getter
    public boolean getSyncedOrderedTargets() {
        return this.syncedOrderedTargets;
    }

    // client-side getter
    public List<TargetView> getSyncedTargets() {
        return this.syncedTargets;
    }

    private void handleCloneItemPickup(CloneSlot<?> cloneSlot, int button, Player player) {
        ItemStack carried = this.getCarried();
        ItemStack target = cloneSlot.getItem();

        // Cursor has an item: place / modify without consuming cursor stack.
        if (!carried.isEmpty()) {
            int maxStack = cloneSlot.getMaxStackSize(carried);

            // Empty target slot.
            if (target.isEmpty()) {
                // Left click places as many as possible to the slot, up to max stack size.
                // Right click places one.
                int newCount = button == 0 ? Math.min(carried.getCount(), maxStack) : 1;
                cloneSlot.setByPlayer(CloneSlot.createUndamagedCopy(carried, newCount));
                cloneSlot.setChanged();
                return;
            }

            // Same item on cursor as target slot.
            if (ItemStack.isSameItemSameComponents(carried, target)) {
                // Left click adds as many as possible to the slot, up to max stack size.
                // Right click adds exactly 1.
                int newCount = button == 0
                        ? Math.min(target.getCount() + carried.getCount(), maxStack)
                        : Math.min(target.getCount() + 1, maxStack);
                cloneSlot.setByPlayer(CloneSlot.createUndamagedCopy(carried, newCount));
                cloneSlot.setChanged();
                return;
            }

            // Different item on cursor.
            if (cloneSlot.mayPlace(carried)) {
                // Left click replaces slot contents with as many as possible of carried item, up to max stack size.
                // Right click replaces slot contents with exactly one of the carried item.
                int newCount = (button == 0) ? Math.min(carried.getCount(), maxStack) : 1;
                cloneSlot.setByPlayer(CloneSlot.createUndamagedCopy(carried, newCount));
                cloneSlot.setChanged();
            }
            return;
        }

        // Empty cursor and non-empty target slot: left click clears, right click decrements / deletes.
        if (!target.isEmpty() && cloneSlot.mayPickup(player)) {
            if (button == 0 || target.getCount() <= 1) {
                cloneSlot.setByPlayer(ItemStack.EMPTY);
            } else {
                cloneSlot.setByPlayer(target.copyWithCount(target.getCount() - 1));
            }
            cloneSlot.setChanged();
        }
    }

    private void handleCloneItemSwap(CloneSlot<?> cloneSlot, int button, Player player) {
        Inventory inventory = player.getInventory();
        ItemStack source = inventory.getItem(button);
        ItemStack target = cloneSlot.getItem();

        // Allow "delete from slot" if the swap source is empty.
        if (source.isEmpty()) {
            if (!target.isEmpty() && cloneSlot.mayPickup(player)) {
                cloneSlot.setByPlayer(ItemStack.EMPTY);
                cloneSlot.setChanged();
            }
        }

        // Otherwise, block number-key / offhand placement into dummy slots.
    }

    // server-side screen action handler
    public void handleScreenAction(ServerPlayer serverPlayer, ExperienceScreenActionC2SPayload payload) {
        this.containerLevelAccess.execute((level, blockPos) -> {
            if (!(level instanceof ServerLevel serverLevel)) {
                return;
            }

            if (!(this.getExperienceInventory() instanceof AbstractExperienceBlockEntity experienceBlockEntity)) {
                return;
            }

            switch (payload.action()) {
                case SET_OPEN_STATUS -> experienceBlockEntity.setOpenForBusiness(payload.primary() != 0);

                case REQUEST_TARGETS -> this.syncTargets(serverPlayer, serverLevel, experienceBlockEntity);

                case MOVE_TARGET -> {
                    if (experienceBlockEntity.moveTarget(payload.primary(), payload.secondary())) {
                        ExperienceTargetOverlaySyncManager.refreshPlayersHolding(serverLevel, experienceBlockEntity.getUUID());
                        this.syncTargets(serverPlayer, serverLevel, experienceBlockEntity);
                    }
                }

                case REMOVE_TARGET -> {
                    experienceBlockEntity.removeTarget(payload.primary());
                    ExperienceTargetOverlaySyncManager.refreshPlayersHolding(serverLevel, experienceBlockEntity.getUUID());
                    this.syncTargets(serverPlayer, serverLevel, experienceBlockEntity);
                }

                case REMOVE_ALL_TARGETS -> {
                    experienceBlockEntity.removeAllTargets();
                    ExperienceTargetOverlaySyncManager.refreshPlayersHolding(serverLevel, experienceBlockEntity.getUUID());
                    this.syncTargets(serverPlayer, serverLevel, experienceBlockEntity);
                }

                case SET_ORDERED_TARGETS -> {
                    experienceBlockEntity.setOrderedTargets(payload.primary() != 0);
                    this.syncTargets(serverPlayer, serverLevel, experienceBlockEntity);
                }
            }
        });
    }

    private boolean hasTargetKey() {
        return this.targetKeySlotId != -1;
    }

    @Override
    public boolean isOpenForBusiness() {
        return this.experienceContainerData.get(AbstractExperienceBlockEntity.DATA_OPEN_FOR_BUSINESS) != 0;
    }

    public boolean isSelectedTab(T tab) {
        return this.selectedTab == tab;
    }

    protected void onKeyTake() {
        if (this.hasTargetKey() && this.experienceInventory instanceof AbstractExperienceBlockEntity experienceBlockEntity) {
            Slot slot = this.getSlot(this.targetKeySlotId);
            slot.set(experienceBlockEntity.createTargetKey());
        }
    }

    @Override
    public @NonNull ItemStack quickMoveStack(@NonNull Player player, int slotId) {
        Slot slot = this.getSlot(slotId);

        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        int inventorySize = this.slots.size();
        ItemStack sourceStack = slot.getItem();
        ItemStack returnStack = sourceStack.copy();

        // The experience key slot is intentionally infinite. Returning the copied
        // key here would make Minecraft's quick-move loop keep pulling keys
        // while the refilled slot still matches the returned stack.
        if (slotId == this.targetKeySlotId) {
            ItemStack keyToMove = sourceStack.copy();
            keyToMove.setCount(1);

            if (!this.moveItemStackTo(keyToMove, this.playerInventorySlotStartId, inventorySize, false)) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, sourceStack);
            return ItemStack.EMPTY;
        }

        if (slot instanceof CloneSlot<?>) {
            slot.setByPlayer(ItemStack.EMPTY);
            slot.setChanged();
            return ItemStack.EMPTY;
        }

        // Experience slot -> player inventory
        if (slotId < this.playerInventorySlotStartId) {
            if (!this.moveItemStackTo(sourceStack, this.playerInventorySlotStartId, inventorySize, false)) {
                return ItemStack.EMPTY;
            }
        }

        // Note: Player inventory -> experience slot should be handled by child class.

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

    public void setSelectedTab(T tab) {
        this.selectedTab = tab;
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
}
