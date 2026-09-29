package org.bensam.touristry.client.screen;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Items;
import org.bensam.touristry.ModBlocks;
import org.bensam.touristry.ModItems;
import org.bensam.touristry.client.screen.tabs.PricingTab;
import org.bensam.touristry.client.screen.tabs.StatusTab;
import org.bensam.touristry.client.screen.tabs.TargetsTab;
import org.bensam.touristry.menu.DiningExperienceMenu;

import java.util.List;

public class DiningExperienceScreen extends AbstractTabbedExperienceScreen<DiningExperienceMenu, DiningExperienceMenu.Tab> {
    private static final int BG_TEXTURE_WIDTH = 512;
    private static final int BG_SCREEN_WIDTH = 276;
    private static final int BG_SCREEN_HEIGHT = 166;

    public DiningExperienceScreen(DiningExperienceMenu containerMenu, Inventory inventory, Component title) {
        super(containerMenu, inventory, title, List.of(
                new StatusTab<>(DiningExperienceMenu.Tab.STATUS, ModBlocks.DINING_EXPERIENCE.get().asItem()),
                new TargetsTab<>(DiningExperienceMenu.Tab.TARGETS, ModItems.EXPERIENCE_TARGET_KEY.get()),
                new PricingTab<>(DiningExperienceMenu.Tab.MENU, Items.EMERALD)));
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
