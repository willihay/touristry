package org.bensam.touristry.tourism;

import net.minecraft.world.item.ItemStack;

public class TouristEconomy {

    private TouristEconomy() {}

    public static void initialize() {}

    public static int getEmeraldEquivalent(ItemStack itemStack) {
        return itemStack.getCount();
    }
}
