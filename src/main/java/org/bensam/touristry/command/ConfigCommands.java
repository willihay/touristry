package org.bensam.touristry.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import org.bensam.touristry.config.ModServerConfigManager;
import org.bensam.touristry.config.ModServerConfigSync;

public final class ConfigCommands {
    private ConfigCommands() {}

    public static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("config")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_ADMIN))
                .then(Commands.literal("reload")
                        .executes(ctx -> reloadConfig(ctx.getSource())))
                .then(Commands.literal("reset")
                        .executes(ctx -> resetConfig(ctx.getSource())))
        );
    }

    private static int reloadConfig(CommandSourceStack source) {
        if (ModServerConfigManager.reload(false)) {
            syncConfigToAllPlayers(source);
            source.sendSuccess(() -> Component.literal("Touristry server configuration reloaded"), true);
            return 1;
        } else {
            source.sendFailure(Component.literal("Touristry server configuration reload failed"));
            return -1;
        }
    }

    private static int resetConfig(CommandSourceStack source) {
        ModServerConfigManager.reset();
        syncConfigToAllPlayers(source);
        source.sendSuccess(() -> Component.literal("Touristry server configuration reset"), true);
        return 1;
    }

    private static void syncConfigToAllPlayers(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ModServerConfigSync.syncToPlayer(player);
        }
    }
}
