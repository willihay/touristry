package org.bensam.touristry.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.bensam.touristry.block.entity.AbstractExperienceBlockEntity;
import org.bensam.touristry.block.entity.TouristBeaconBlockEntity;
import org.bensam.touristry.tourism.TourismManager;

public final class TourCommand {
    private TourCommand() {}

    @FunctionalInterface
    public interface BeaconResolver {
        TouristBeaconBlockEntity resolve(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException;
    }

    @FunctionalInterface
    public interface ExperienceResolver {
        AbstractExperienceBlockEntity resolve(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException;
    }

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

    public static TouristBeaconBlockEntity requireBeacon(CommandSourceStack source, BlockPos blockPos) throws CommandSyntaxException {
        if (source.getServer().overworld().getBlockEntity(blockPos) instanceof TouristBeaconBlockEntity beaconBlockEntity) {
            return beaconBlockEntity;
        }

        throw new SimpleCommandExceptionType(Component.literal("No beacon found @ " + blockPos.toShortString())).create();
    }

    public static TouristBeaconBlockEntity requireNearestBeacon(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer serverPlayer = source.getPlayer();
        if (serverPlayer == null) {
            throw new SimpleCommandExceptionType(Component.literal("No player position available")).create();
        }

        TouristBeaconBlockEntity beaconBlockEntity = TourismManager.findClosestBeaconEntity(serverPlayer.blockPosition());
        if (beaconBlockEntity == null) {
            throw new SimpleCommandExceptionType(Component.literal("No beacon found in this dimension")).create();
        }

        return beaconBlockEntity;
    }

    public static AbstractExperienceBlockEntity requireExperience(CommandSourceStack source, BlockPos blockPos) throws CommandSyntaxException {
        if (source.getServer().overworld().getBlockEntity(blockPos) instanceof AbstractExperienceBlockEntity experienceBlockEntity) {
            return experienceBlockEntity;
        }

        throw new SimpleCommandExceptionType(Component.literal("No experience block found @ " + blockPos.toShortString())).create();
    }

    public static AbstractExperienceBlockEntity requireNearestExperience(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer serverPlayer = source.getPlayer();
        if (serverPlayer == null) {
            throw new SimpleCommandExceptionType(Component.literal("No player position available")).create();
        }

        AbstractExperienceBlockEntity experienceBlockEntity = TourismManager.findClosestExperienceEntity(serverPlayer.blockPosition());
        if (experienceBlockEntity == null) {
            throw new SimpleCommandExceptionType(Component.literal("No experience block found in dimension")).create();
        }

        return experienceBlockEntity;
    }
}
