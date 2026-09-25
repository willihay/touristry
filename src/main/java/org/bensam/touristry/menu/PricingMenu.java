package org.bensam.touristry.menu;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.bensam.touristry.tourism.experience.ItemPrice;

import java.util.List;

public interface PricingMenu {
    ItemStack getDefaultCost();
    Slot getDefaultCostSlot();
    ItemStack getFocusedItemForSale();
    Slot getFocusedItemForSaleSlot();
    ItemStack getFocusedItemCost();
    Slot getFocusedItemCostSlot();
    List<ItemPrice> getSyncedItemPrices();
    int getSyncedItemPricesRevision();
    boolean isDefaultCostFree();
}
