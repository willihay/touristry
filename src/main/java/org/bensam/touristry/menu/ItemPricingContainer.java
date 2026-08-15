package org.bensam.touristry.menu;

import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ItemPricingContainer implements Container {
    public static final int ITEM_FOR_SALE_SLOT = 0;
    public static final int COST_SLOT = 1;
    public static final int ITEM_PRICING_SLOTS = 2;

    private ItemStack activeItemForSale = ItemStack.EMPTY;
    private final NonNullList<ItemStack> itemStacks = NonNullList.withSize(ITEM_PRICING_SLOTS, ItemStack.EMPTY);
    private final ShoppingExperienceMenu menu;

    public ItemPricingContainer(ShoppingExperienceMenu menu) {
        this.menu = menu;
    }

    @Override
    public void clearContent() {
        this.activeItemForSale = ItemStack.EMPTY;
        this.itemStacks.clear();
    }

    @Override
    public int getContainerSize() {
        return this.itemStacks.size();
    }

    @Override
    public ItemStack getItem(int i) {
        return this.itemStacks.get(i);
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack itemStack : this.itemStacks) {
            if (!itemStack.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public ItemStack removeItem(int i, int j) {
        ItemStack itemStack = ContainerHelper.removeItem(this.itemStacks, i, j);
        if (!itemStack.isEmpty()) {
            this.setChanged();
        }

        return itemStack;
    }

    @Override
    public ItemStack removeItemNoUpdate(int i) {
        return ContainerHelper.takeItem(this.itemStacks, i);
    }

    @Override
    public void setChanged() {
        this.setActiveItemForSale();
    }

    @Override
    public void setItem(int i, ItemStack itemStack) {
        this.itemStacks.set(i, itemStack);
        itemStack.limitSize(this.getMaxStackSize(itemStack));
        this.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return this.menu.stillValid(player);
    }

    private void setActiveItemForSale() {
        ItemStack itemForSale = this.itemStacks.get(ITEM_FOR_SALE_SLOT);
        if (!ItemStack.isSameItemSameComponents(this.activeItemForSale, itemForSale)) {
            this.activeItemForSale = itemForSale.copy();
            this.menu.onItemForSaleChanged(this.activeItemForSale);
        }
    }
}
