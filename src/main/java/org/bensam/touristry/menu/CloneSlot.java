package org.bensam.touristry.menu;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.function.Predicate;

public class CloneSlot<T extends Enum<T>> extends TabbedMenuSlot<T> {
    CloneSlot(AbstractExperienceMenu<T> menu, Container container, int containerSlotId, int x, int y, Predicate<AbstractExperienceMenu<T>> visibleWhen) {
        super(menu, container, containerSlotId, x, y, visibleWhen);
    }

    public static ItemStack createUndamagedCopy(ItemStack itemStack, int count) {
        ItemStack copy = itemStack.copyWithCount(count);
        if (copy.isDamageableItem()) {
            copy.setDamageValue(0);
        }
        return copy;
    }

    @Override
    public boolean mayPlace(ItemStack itemStack) {
        return !itemStack.isEmpty();
    }

    @Override
    public ItemStack safeInsert(ItemStack itemStack, int amount) {
        if (!this.mayPlace(itemStack)) {
            return itemStack;
        }

        int copiedCount = Math.min(amount, itemStack.getMaxStackSize());
        this.set(createUndamagedCopy(itemStack, copiedCount));
        return itemStack; // unchanged - player's stack is not consumed
    }

    @Override
    public Optional<ItemStack> tryRemove(int amount, int maxAmount, Player player) {
        ItemStack target = this.getItem();
        if (target.isEmpty()) {
            return Optional.empty();
        }

        int removedCount = Math.min(amount, target.getCount());
        if (removedCount >= target.getCount()) {
            this.set(ItemStack.EMPTY);
        } else {
            this.set(target.copyWithCount(target.getCount() - removedCount));
        }

        return Optional.empty(); // destroy removed stack instead of giving it to the player
    }
}
