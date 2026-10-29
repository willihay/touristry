package org.bensam.touristry.client.screen;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.bensam.touristry.ModBlocks;
import org.bensam.touristry.ModItems;
import org.bensam.touristry.client.screen.tabs.StatusTab;
import org.bensam.touristry.client.screen.tabs.TargetsTab;
import org.bensam.touristry.menu.LodgingExperienceMenu;

import java.util.List;

public class LodgingExperienceScreen extends AbstractTabbedExperienceScreen<LodgingExperienceMenu, LodgingExperienceMenu.Tab> {
    private static final int BG_TEXTURE_WIDTH = 512;
    private static final int BG_SCREEN_WIDTH = 276;
    private static final int BG_SCREEN_HEIGHT = 166;
    private static final Component BED_TARGETS_TAB_TITLE = Component.translatable("screen.touristry.tourist_block.tab.beds");

    public LodgingExperienceScreen(LodgingExperienceMenu containerMenu, Inventory inventory, Component title) {
        super(containerMenu, inventory, title, List.of(
                new StatusTab<>(null, LodgingExperienceMenu.Tab.STATUS, ModBlocks.LODGING_EXPERIENCE.get().asItem()),
                new TargetsTab<>(BED_TARGETS_TAB_TITLE, LodgingExperienceMenu.Tab.TARGETS, ModItems.EXPERIENCE_TARGET_KEY.get(), false)
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
