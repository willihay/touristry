package org.bensam.touristry.block.entity;

import net.minecraft.world.item.ItemStack;

public record ItemStackKey(ItemStack itemStack) {
    public ItemStackKey {
        itemStack = itemStack.copyWithCount(1);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ItemStackKey(ItemStack other) && ItemStack.isSameItemSameComponents(this.itemStack, other);
    }

    @Override
    public int hashCode() {
        return ItemStack.hashItemAndComponents(this.itemStack);
    }
}
