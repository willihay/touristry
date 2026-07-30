package org.bensam.touristry.menu;

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
import org.bensam.touristry.block.entity.SightseeingExperienceBlockEntity;
import org.jspecify.annotations.NonNull;

import java.util.Optional;

public class SightseeingExperienceMenu extends AbstractContainerMenu implements TourismStatusMenu {
    // Slot layout
    private static final int EXPERIENCE_PAYMENT_SLOT_COUNT = SightseeingExperienceBlockEntity.PAYMENT_SLOT_SIZE;
    private static final int EXPERIENCE_SLOT_COUNT = SightseeingExperienceBlockEntity.TOTAL_INVENTORY_SIZE;
    private static final int EXPERIENCE_PAYMENT_SLOT_START_X = 116;
    private static final int EXPERIENCE_PAYMENT_SLOT_START_Y = 17;
    private static final int EXPERIENCE_TARGET_KEY_SLOT = SightseeingExperienceBlockEntity.TARGET_KEY_INDEX;
    private static final int EXPERIENCE_TARGET_KEY_SLOT_X = 80;
    private static final int EXPERIENCE_TARGET_KEY_SLOT_Y = 35;
    private static final int EXPERIENCE_ENTRY_FEE_SLOT = SightseeingExperienceBlockEntity.ENTRY_FEE_INDEX;
    private static final int EXPERIENCE_ENTRY_FEE_SLOT_X = 80;
    private static final int EXPERIENCE_ENTRY_FEE_SLOT_Y = 53;
    private static final int SLOT_SIDE_LENGTH = 18;

    // Player inventory layout
    private static final int PLAYER_INVENTORY_ROW_X = 8;
    private static final int PLAYER_INVENTORY_ROW_Y = 84;
    private static final int PLAYER_SLOT_START = EXPERIENCE_SLOT_COUNT;

    private final Container experienceInventory;
    private final ContainerData experienceContainerData;
    private final ContainerLevelAccess containerLevelAccess;

    // Client-side constructor:
    // Uses dummy containers so the menu can be constructed on the client
    // Real state is synced from the server through ContainerData and slot containers.
    public SightseeingExperienceMenu(int containerId, Inventory playerInventory) {
        this(
                containerId,
                playerInventory,
                new SimpleContainer(EXPERIENCE_SLOT_COUNT),
                new SimpleContainerData(SightseeingExperienceBlockEntity.DATA_COUNT),
                ContainerLevelAccess.NULL
        );
    }

    // Server-side constructor:
    public SightseeingExperienceMenu(int containerId, Inventory playerInventory, Container experienceInventory, ContainerData data, ContainerLevelAccess access) {
        super(ModMenus.SIGHTSEEING_EXPERIENCE_MENU.get(), containerId);
        this.experienceInventory = experienceInventory;
        this.experienceContainerData = data;
        this.containerLevelAccess = access;

        // Add payment slots.
        this.add3x3GridSlots(this.experienceInventory, EXPERIENCE_PAYMENT_SLOT_START_X, EXPERIENCE_PAYMENT_SLOT_START_Y);

        // Add target key slot.
        this.addSlot(new Slot(this.experienceInventory, EXPERIENCE_TARGET_KEY_SLOT, EXPERIENCE_TARGET_KEY_SLOT_X, EXPERIENCE_TARGET_KEY_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) { return false; }

            @Override
            public void onTake(Player player, ItemStack stack) {
                SightseeingExperienceMenu.this.onKeyTake(player, stack);
            }
        });

        // Add entry fee slot.
        this.addSlot(new Slot(this.experienceInventory, EXPERIENCE_ENTRY_FEE_SLOT, EXPERIENCE_ENTRY_FEE_SLOT_X, EXPERIENCE_ENTRY_FEE_SLOT_Y) {
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
        this.addStandardInventorySlots(playerInventory, PLAYER_INVENTORY_ROW_X, PLAYER_INVENTORY_ROW_Y);

        // Add data slots for data sync.
        this.addDataSlots(this.experienceContainerData);
    }

    protected void add3x3GridSlots(Container container, int firstSlotX, int firstSlotY) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slotIndex = col + row * 3;
                this.addSlot(new Slot(container, slotIndex, firstSlotX + col * SLOT_SIDE_LENGTH, firstSlotY + row * SLOT_SIDE_LENGTH));
            }
        }
    }

    public int getContainerId() {
        return this.containerId;
    }

    public double getReputation() {
        return (double) this.experienceContainerData.get(SightseeingExperienceBlockEntity.DATA_REPUTATION) / 100;
    }

    public boolean isOpenForBusiness() {
        return this.experienceContainerData.get(SightseeingExperienceBlockEntity.DATA_OPEN_FOR_BUSINESS) != 0;
    }

    public void syncTargets(ServerPlayer player, ServerLevel serverLevel, AbstractExperienceBlockEntity experienceBlockEntity) {
        // TODO: Implement in an AbstractExperienceMenu class.
    }

    private void toggleOpenForBusiness() {
        this.containerLevelAccess.execute((level, blockPos) -> {
            if (level.getBlockEntity(blockPos) instanceof SightseeingExperienceBlockEntity sightseeingExperienceBlockEntity) {
                sightseeingExperienceBlockEntity.setOpenForBusiness(!sightseeingExperienceBlockEntity.isOpenForBusiness());
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

    protected void onKeyTake(Player player, ItemStack itemStack) {
        if (this.experienceInventory instanceof SightseeingExperienceBlockEntity sightseeingExperienceBlockEntity) {
            Slot slot = this.slots.get(EXPERIENCE_TARGET_KEY_SLOT);
            slot.set(sightseeingExperienceBlockEntity.createTargetKey());
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
        return stillValid(this.containerLevelAccess, player, ModBlocks.SIGHTSEEING_EXPERIENCE.get());
    }
}
