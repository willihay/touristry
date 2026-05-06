package org.bensam.touristry.client.integration.yacl;

import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.bensam.touristry.client.config.ModClientConfigManager;
import org.bensam.touristry.client.network.ConfigClientPackets;

public class YACLConfigScreen {
    private YACLConfigScreen() {}

    public static Screen create(Screen parentScreen) {
        return YetAnotherConfigLib.createBuilder()
                .title(Component.literal("Touristry config"))
                .save(YACLConfigScreen::saveConfig)
                .category(ConfigCategory.createBuilder()
                        .name(Component.literal("Client Configuration"))
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.literal("Verbose tooltips"))
                                .description(OptionDescription.of(Component.literal("Show full help text in tooltips.")))
                                .binding(
                                        true, // default value
                                        () -> ModClientConfigManager.getConfig().verboseTooltips(), // getter to current value
                                        newValue -> ModClientConfigManager.getConfig().verboseTooltips = newValue
                                )
                                .controller(TickBoxControllerBuilder::create)
                                .build()
                        )
                        .build()
                )
                .build()
                .generateScreen(parentScreen);
    }

    protected static void saveConfig() {
        ModClientConfigManager.save();
        ConfigClientPackets.sendClientPreferences();
    }
}
