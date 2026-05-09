package org.bensam.touristry.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public final class TourCommand {
    private TourCommand() {}

    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher,
            CommandBuildContext registryAccess,
            Commands.CommandSelection environment
    ) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("tour");

        ConfigCommands.register(root);
        DebugCommands.register(root);
        PlayerCommands.register(root);

        dispatcher.register(root);
    }
}
