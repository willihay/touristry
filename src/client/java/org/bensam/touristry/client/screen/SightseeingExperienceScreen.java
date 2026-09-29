package org.bensam.touristry.client.screen;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.bensam.touristry.ModBlocks;
import org.bensam.touristry.ModItems;
import org.bensam.touristry.client.screen.tabs.StatusTab;
import org.bensam.touristry.client.screen.tabs.TargetsTab;
import org.bensam.touristry.menu.SightseeingExperienceMenu;

import java.util.List;

public class SightseeingExperienceScreen extends AbstractTabbedExperienceScreen<SightseeingExperienceMenu, SightseeingExperienceMenu.Tab> {
    private static final int BG_TEXTURE_WIDTH = 512;
    private static final int BG_SCREEN_WIDTH = 276;
    private static final int BG_SCREEN_HEIGHT = 166;

    public SightseeingExperienceScreen(SightseeingExperienceMenu containerMenu, Inventory inventory, Component title) {
        super(containerMenu, inventory, title, List.of(
                new StatusTab<>(SightseeingExperienceMenu.Tab.STATUS, ModBlocks.SIGHTSEEING_EXPERIENCE.get().asItem()),
                new TargetsTab<>(SightseeingExperienceMenu.Tab.TARGETS, ModItems.EXPERIENCE_TARGET_KEY.get())
        ));
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
