package org.bensam.touristry.client.screen;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Items;
import org.bensam.touristry.ModBlocks;
import org.bensam.touristry.ModItems;
import org.bensam.touristry.client.screen.tabs.PricingTab;
import org.bensam.touristry.client.screen.tabs.StatusTab;
import org.bensam.touristry.client.screen.tabs.TargetsTab;
import org.bensam.touristry.menu.ShoppingExperienceMenu;

import java.util.List;

public class ShoppingExperienceScreen extends AbstractTabbedExperienceScreen<ShoppingExperienceMenu, ShoppingExperienceMenu.Tab> {
    private static final int BG_TEXTURE_WIDTH = 512;
    public static final int BG_SCREEN_WIDTH = 276;
    public static final int BG_SCREEN_HEIGHT = 166;

    public ShoppingExperienceScreen(ShoppingExperienceMenu containerMenu, Inventory inventory, Component title) {
        super(containerMenu, inventory, title, List.of(
                new StatusTab<>(ShoppingExperienceMenu.Tab.STATUS, ModBlocks.SHOPPING_EXPERIENCE.get().asItem()),
                new TargetsTab<>(ShoppingExperienceMenu.Tab.TARGETS, ModItems.EXPERIENCE_TARGET_KEY.get()),
                new PricingTab<>(ShoppingExperienceMenu.Tab.PRICING, Items.EMERALD)));
        this.imageWidth = BG_SCREEN_WIDTH;
    }

    @Override
    protected int getBackgroundTextureWidth() {
        return BG_TEXTURE_WIDTH;
    }

    @Override
    public int getScreenWidth() {
        return BG_SCREEN_WIDTH;
    }

    @Override
    public int getScreenHeight() {
        return BG_SCREEN_HEIGHT;
    }
}
