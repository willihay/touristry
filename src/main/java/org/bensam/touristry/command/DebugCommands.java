package org.bensam.touristry.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
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
import org.jspecify.annotations.NonNull;

public final class DebugCommands {
    private DebugCommands() {}

    public static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("debug")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_ADMIN))
                .then(Commands.literal("beacon")
                        .then(addBeaconActions(
                                Commands.argument("beaconPos", BlockPosArgument.blockPos()),
                                ctx -> TourCommand.requireBeacon(
                                        ctx.getSource(),
                                        BlockPosArgument.getLoadedBlockPos(ctx, "beaconPos"))))
                        .then(addBeaconActions(
                                Commands.literal("nearest"),
                                ctx -> TourCommand.requireNearestBeacon(ctx.getSource()))))
                .then(Commands.literal("tourismManager")
                        .then(Commands.literal("clearSchedule")
                                .executes(ctx -> clearSpawnSchedule(ctx.getSource())))
                        .then(Commands.literal("remakeSchedule")
                                .executes(ctx -> remakeSpawnSchedule(ctx.getSource()))))
                .then(Commands.literal("tourist")
                        .then(Commands.literal("despawnAll")
                                .executes(ctx -> despawnAllTourists(ctx.getSource())))
                        .then(Commands.literal("spawnHere")
                                .executes(ctx -> spawnTourist(ctx.getSource()))))
        );
    }

    private static <T extends ArgumentBuilder<CommandSourceStack, T>> T addBeaconActions(
            T parent,
            TourCommand.BeaconResolver resolver
    ) {
        return parent
                .then(Commands.literal("reset")
                        .then(Commands.literal("allStats")
                                .executes(ctx -> resetBeaconStats(
                                        ctx.getSource(),
                                        resolver.resolve(ctx))))
                        .then(Commands.literal("reputation")
                                .executes(ctx -> resetBeaconReputation(
                                        ctx.getSource(),
                                        resolver.resolve(ctx)))))
                .then(Commands.literal("spawnTouristInRange")
                        .executes(ctx -> spawnBeaconTourist(
                                ctx.getSource(),
                                resolver.resolve(ctx))))
                .then(Commands.literal("spawnTouristHere")
                        .executes(ctx -> spawnBeaconTouristHere(
                                ctx.getSource(),
                                resolver.resolve(ctx))));
    }

    private static int resetBeaconStats(CommandSourceStack source, @NonNull TouristBeaconBlockEntity beaconBlockEntity) {
        beaconBlockEntity.resetAllStats();
        Component message = Component.literal("Reset all stats for ")
                .append(beaconBlockEntity.getName().copy())
                .append(Component.literal(" @ " + beaconBlockEntity.getBlockPos().toShortString()));
        source.sendSuccess(() -> message, true);
        return 1;
    }

    private static int resetBeaconReputation(CommandSourceStack source, @NonNull TouristBeaconBlockEntity beaconBlockEntity) {
        beaconBlockEntity.resetReputation();
        Component message = Component.literal("Reset reputation for ")
                .append(beaconBlockEntity.getName().copy())
                .append(Component.literal(" @ " + beaconBlockEntity.getBlockPos().toShortString()));
        source.sendSuccess(() -> message, true);
        return 1;
    }

    private static int spawnBeaconTourist(CommandSourceStack source, @NonNull TouristBeaconBlockEntity beaconBlockEntity) {
        if (source.getLevel() != source.getLevel().getServer().overworld()) {
            source.sendFailure(Component.literal("Tourists will only spawn in the overworld"));
            return -1;
        }

        if (TourismManager.trySpawnTourist(source.getLevel(), null, beaconBlockEntity, false)) {
            Component message = Component.literal("Spawned tourist for ")
                    .append(beaconBlockEntity.getName().copy());
            source.sendSuccess(() -> message, false);
            return 1;
        } else {
            source.sendFailure(Component.literal("Unable to spawn tourist for beacon"));
            return -1;
        }
    }

    private static int spawnBeaconTouristHere(CommandSourceStack source, @NonNull TouristBeaconBlockEntity beaconBlockEntity) {
        if (source.getLevel() != source.getLevel().getServer().overworld()) {
            source.sendFailure(Component.literal("Tourists will only spawn in the overworld"));
            return -1;
        }

        ServerPlayer serverPlayer = source.getPlayer();
        if (serverPlayer == null) {
            source.sendFailure(Component.literal("No player position available"));
            return -1;
        }

        Vec3 lookAngle = serverPlayer.getLookAngle();
        Vec3 horizonLookAngle = new Vec3(lookAngle.x(), 0, lookAngle.z());
        BlockPos spawnPos = BlockPos.containing(serverPlayer.position().add(horizonLookAngle.normalize().scale(2.0)));

        if (TourismManager.trySpawnTourist(source.getLevel(), spawnPos, beaconBlockEntity, false)) {
            Component message = Component.literal("Spawned tourist for ")
                    .append(beaconBlockEntity.getName().copy())
                    .append(Component.literal(" found @ " + beaconBlockEntity.getBlockPos().toShortString()));
            source.sendSuccess(() -> message, false);
            return 1;
        } else {
            source.sendFailure(Component.literal("Unable to spawn tourist here"));
            return -1;
        }
    }

    private static int clearSpawnSchedule(CommandSourceStack source) {
        TourismManager.clearSpawnSchedule();
        source.sendSuccess(() -> Component.literal("Cleared today's tourist schedule"), true);
        return 1;
    }

    private static int remakeSpawnSchedule(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("Resetting and recalculating today's tourist schedule..."), true);
        TourismManager.resetSpawnSchedule();
        return 1;
    }

    private static int despawnAllTourists(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("Instructing all tourists to despawn..."), true);
        TourismManager.setForceDespawnAll();
        return 1;
    }

    private static int spawnTourist(CommandSourceStack source) {
        ServerPlayer serverPlayer = source.getPlayer();
        if (serverPlayer == null) {
            source.sendFailure(Component.literal("No player position available"));
            return -1;
        }

        Vec3 lookAngle = serverPlayer.getLookAngle();
        Vec3 horizonLookAngle = new Vec3(lookAngle.x(), 0, lookAngle.z());
        BlockPos spawnPos = BlockPos.containing(serverPlayer.position().add(horizonLookAngle.normalize().scale(2.0)));

        if (TourismManager.trySpawnTourist(source.getLevel(), spawnPos, null, true)) {
            return 1;
        } else {
            source.sendFailure(Component.literal("Unable to spawn tourist"));
            return -1;
        }
    }
}
