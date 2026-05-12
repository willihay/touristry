package org.bensam.touristry.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.phys.Vec3;
import org.bensam.touristry.block.entity.TouristBeaconBlockEntity;
import org.bensam.touristry.tourism.TourismManager;
import org.bensam.touristry.tourism.TouristBeaconExperience;
import org.bensam.touristry.tourism.TouristBeaconStats;

public final class DebugCommands {
    private DebugCommands() {}

    public static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("debug")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_ADMIN))
                .then(Commands.literal("beacon")
                        .then(Commands.argument("beaconPos", BlockPosArgument.blockPos())
                                .then(Commands.literal("info")
                                        .executes(ctx -> listInfo(
                                                ctx.getSource(),
                                                requireBeacon(
                                                        ctx.getSource(),
                                                        BlockPosArgument.getLoadedBlockPos(ctx, "beaconPos")))))
                                .then(Commands.literal("reputation")
                                        .then(Commands.literal("reset")
                                                .executes(ctx -> resetReputation(
                                                        ctx.getSource(),
                                                        requireBeacon(
                                                                ctx.getSource(),
                                                                BlockPosArgument.getLoadedBlockPos(ctx, "beaconPos"))))))
                                .then(Commands.literal("spawn")
                                        .executes(ctx -> spawnTouristForBeacon(
                                                ctx.getSource(),
                                                requireBeacon(
                                                        ctx.getSource(),
                                                        BlockPosArgument.getLoadedBlockPos(ctx, "beaconPos")))))
                                .then(Commands.literal("stats")
                                        .then(Commands.literal("reset")
                                                .executes(ctx -> resetAllStats(
                                                        ctx.getSource(),
                                                        requireBeacon(
                                                                ctx.getSource(),
                                                                BlockPosArgument.getLoadedBlockPos(ctx, "beaconPos"))))))))
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

    private static int listInfo(CommandSourceStack source, TouristBeaconBlockEntity beaconBlockEntity) {
        if (beaconBlockEntity == null) {
            return -1;
        }

        Component message = beaconBlockEntity.getName().copy()
                .append(Component.literal(" @ " + beaconBlockEntity.getBlockPos().toShortString() + " info:"));
        source.sendSuccess(() -> message, false);

        TouristBeaconExperience experience = beaconBlockEntity.getBeaconExperience();
        source.sendSuccess(() -> Component.literal(" - status: " + (experience.beaconOpenForBusiness() ? "open for business" : "closed for business")), false);

        TouristBeaconStats stats = beaconBlockEntity.getBeaconStats();
        source.sendSuccess(() -> Component.literal(
                " - reputation: " + String.format("%.2f", stats.reputation())
                        + "; successful visits: " + stats.successfulVisits()
                        + "; failed visits: " + stats.failedVisits()
                        + "; failed spawns: " + stats.failedSpawns()),
                false);
        return 1;
    }

    private static int resetAllStats(CommandSourceStack source, TouristBeaconBlockEntity beaconBlockEntity) {
        if (beaconBlockEntity == null) {
            return -1;
        }

        beaconBlockEntity.resetAllStats();
        Component message = Component.literal("Reset all stats for ")
                .append(beaconBlockEntity.getName().copy())
                .append(Component.literal(" @ " + beaconBlockEntity.getBlockPos().toShortString()));
        source.sendSuccess(() -> message, true);
        return 1;
    }

    private static int resetReputation(CommandSourceStack source, TouristBeaconBlockEntity beaconBlockEntity) {
        if (beaconBlockEntity == null) {
            return -1;
        }

        beaconBlockEntity.resetReputation();
        Component message = Component.literal("Reset reputation for ")
                .append(beaconBlockEntity.getName().copy())
                .append(Component.literal(" @ " + beaconBlockEntity.getBlockPos().toShortString()));
        source.sendSuccess(() -> message, true);
        return 1;
    }

    private static int spawnTouristForBeacon(CommandSourceStack source, TouristBeaconBlockEntity beaconBlockEntity) {
        if (beaconBlockEntity == null) {
            return -1;
        }

        if (TourismManager.trySpawnTouristForBeacon(source.getLevel(), null, beaconBlockEntity)) {
            Component message = Component.literal("Spawned tourist for ")
                    .append(beaconBlockEntity.getName().copy());
            source.sendSuccess(() -> message, false);
            return 1;
        } else {
            source.sendFailure(Component.literal("Unable to spawn tourist for beacon"));
            return -1;
        }
    }

    private static int despawnAll(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("Instructing all tourists to despawn..."), true);
        TourismManager.setForceDespawnAll();
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
                    .append(Component.literal(" @ " + beaconBlockEntity.getBlockPos().toShortString()));
            source.sendSuccess(() -> message, false);
            return 1;
        } else {
            source.sendFailure(Component.literal("Unable to spawn tourist here"));
            return -1;
        }
    }

    private static int resetSpawnSchedule(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("Resetting and recalculating today's tourist schedule..."), true);
        TourismManager.resetSpawnSchedule();
        return 1;
    }

    private static TouristBeaconBlockEntity requireBeacon(CommandSourceStack source, BlockPos blockPos) {
        if (source.getServer().overworld().getBlockEntity(blockPos) instanceof TouristBeaconBlockEntity beaconBlockEntity) {
            return beaconBlockEntity;
        }

        source.sendFailure(Component.literal("No beacon found @ " + blockPos.toShortString()));
        return null;
    }
}
