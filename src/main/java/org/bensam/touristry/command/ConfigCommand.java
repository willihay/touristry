package org.bensam.touristry.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import org.bensam.touristry.config.ModServerConfigManager;
import org.bensam.touristry.config.ModServerConfigSync;

public class ConfigCommand {
    public ConfigCommand(
            CommandDispatcher<CommandSourceStack> dispatcher,
            CommandBuildContext registryAccess,
            Commands.CommandSelection environment
    ) {
        LiteralArgumentBuilder<CommandSourceStack> arcCommand = Commands.literal("tour")
                .requires(player -> player.permissions().hasPermission(Permissions.COMMANDS_ADMIN));

        arcCommand.then(Commands.literal("config")
                .then(Commands.literal("reload")
                        .executes(source -> this.reloadConfig(source.getSource())))
                .then(Commands.literal("reset")
                        .executes(source -> this.resetConfig(source.getSource())))
        );

        dispatcher.register(arcCommand);
    }

    protected int reloadConfig(CommandSourceStack source) {
        if (ModServerConfigManager.reload(false)) {
            this.syncConfigToAllPlayers(source);
            source.sendSuccess(() -> Component.literal("Touristry server configuration reloaded"), true);
            return 1;
        } else {
            source.sendFailure(Component.literal("Touristry server configuration reload failed"));
            return -1;
        }
    }

    protected int resetConfig(CommandSourceStack source) {
        ModServerConfigManager.reset();
        this.syncConfigToAllPlayers(source);
        source.sendSuccess(() -> Component.literal("Touristry server configuration reset"), true);
        return 1;
    }

    private void syncConfigToAllPlayers(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ModServerConfigSync.syncToPlayer(player);
        }
    }
}
