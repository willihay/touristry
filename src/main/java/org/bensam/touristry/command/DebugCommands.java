package org.bensam.touristry.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.phys.Vec3;
import org.bensam.touristry.block.entity.TouristBeaconBlockEntity;
import org.bensam.touristry.tourism.TourismManager;

public final class DebugCommands {
    private DebugCommands() {}

    public static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("debug")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_ADMIN))
                .then(Commands.literal("despawn")
                        .then(Commands.literal("all")
                                .executes(ctx -> despawnAll(ctx.getSource()))))
                .then(Commands.literal("spawn")
                        .then(Commands.literal("clearSchedule")
                                .executes(ctx -> clearSpawnSchedule(ctx.getSource())))
                        .then(Commands.literal("forNearestBeacon")
                                .executes(ctx -> spawnHere(ctx.getSource())))
                        .then(Commands.literal("resetSchedule")
                                .executes(ctx -> resetSpawnSchedule(ctx.getSource()))))
        );
    }

    private static int despawnAll(CommandSourceStack source) {
        TourismManager.setForceDespawnAll();
        source.sendSuccess(() -> Component.literal("Instructing all tourists to despawn..."), true);
        return 1;
    }

    private static int clearSpawnSchedule(CommandSourceStack source) {
        TourismManager.clearSpawnSchedule();
        source.sendSuccess(() -> Component.literal("Cleared today's tourist schedule"), true);
        return 1;
    }

    private static int spawnHere(CommandSourceStack source) {
        ServerPlayer serverPlayer = source.getPlayer();
        if (serverPlayer == null) {
            source.sendFailure(Component.literal("Command requires player position"));
            return -1;
        }

        TouristBeaconBlockEntity beaconBlockEntity = TourismManager.findClosestBeaconEntity(serverPlayer.blockPosition());
        if (beaconBlockEntity == null) {
            source.sendFailure(Component.literal("No beacon found in this dimension"));
            return -1;
        }

        Vec3 lookAngle = serverPlayer.getLookAngle();
        Vec3 horizonLookAngle = new Vec3(lookAngle.x(), 0, lookAngle.z());
        BlockPos spawnPos = BlockPos.containing(serverPlayer.position().add(horizonLookAngle.normalize().scale(2.0)));

        if (TourismManager.trySpawnTouristForBeacon(source.getLevel(), spawnPos, beaconBlockEntity)) {
            Component message = Component.literal("Spawned tourist for ")
                    .append(beaconBlockEntity.getName().copy())
                    .append(Component.literal(" @ " + beaconBlockEntity.getBlockPos()));
            source.sendSuccess(() -> message, false);
            return 1;
        } else {
            source.sendFailure(Component.literal("Unable to spawn tourist here"));
            return -1;
        }
    }

    private static int resetSpawnSchedule(CommandSourceStack source) {
        TourismManager.resetSpawnSchedule();
        source.sendSuccess(() -> Component.literal("Resetting and recalculating today's tourist schedule..."), true);
        return 1;
    }
}
