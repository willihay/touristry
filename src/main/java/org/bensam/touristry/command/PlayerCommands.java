package org.bensam.touristry.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import org.bensam.touristry.block.entity.TouristBeaconBlockEntity;
import org.bensam.touristry.tourism.TourismManager;

import java.util.List;

@SuppressWarnings("SameReturnValue")
public final class PlayerCommands {
    private PlayerCommands() {}

    public static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("next")
                .executes(ctx -> showNextSpawn(ctx.getSource())));

        root.then(Commands.literal("now")
                .executes(ctx -> showTimeAndDay(ctx.getSource()))
        );

        root.then(Commands.literal("list")
                .then(Commands.literal("beacons")
                        .executes(ctx -> listBeacons(ctx.getSource())))
                .then(Commands.literal("touristSchedule")
                        .executes(ctx -> listTouristSchedule(ctx.getSource()))));

    }

    private static int showNextSpawn(CommandSourceStack source) {
        List<TourismManager.ScheduledTouristSpawn> pendingSpawns = TourismManager.getPendingSpawns();
        if (pendingSpawns.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No pending tourist spawns"), false);
            return 1;
        }

        TourismManager.ScheduledTouristSpawn nextSpawn = pendingSpawns.getFirst();
        MutableComponent message = Component.literal("Next spawn at " + TourismManager.getFriendlyTimeOfDay(nextSpawn.timeOfDay()) + " for ");
        if (source.getServer().overworld().getBlockEntity(nextSpawn.beaconPos()) instanceof TouristBeaconBlockEntity beaconBlockEntity) {
            message.append(beaconBlockEntity.getName().copy());
        } else {
            message.append(Component.literal("unknown beacon"));
        }
        message.append(Component.literal(" @ " + nextSpawn.beaconPos().toShortString()));
        source.sendSuccess(() -> message, false);
        return 1;
    }

    private static int showTimeAndDay(CommandSourceStack source) {
        ServerLevel overworld = source.getServer().overworld();
        source.sendSuccess(() -> Component.literal("Current time: " + TourismManager.getFriendlyTimeOfDay(overworld.getDayTime()) + " on day " + overworld.getDayCount()), false);
        return 1;
    }

    private static int listBeacons(CommandSourceStack source) {
        List<TouristBeaconBlockEntity> loadedBeacons = TourismManager.getLoadedTouristBeacons(source.getServer().overworld());
        if (loadedBeacons.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No tourist beacons found"), false);
            return 1;
        }

        source.sendSuccess(() -> Component.literal("Tourist beacon locations:"), false);

        for (TouristBeaconBlockEntity beaconBlockEntity : loadedBeacons) {
            Component message = Component.literal(" - ")
                    .append(beaconBlockEntity.getName().copy())
                    .append(Component.literal(" @ " + beaconBlockEntity.getBlockPos().toShortString()));
            source.sendSuccess(() -> message, false);
        }
        return 1;
    }

    private static int listTouristSchedule(CommandSourceStack source) {
        List<TourismManager.ScheduledTouristSpawn> pendingSpawns = TourismManager.getPendingSpawns();
        if (pendingSpawns.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No tourists scheduled for today"), false);
            return 1;
        }

        source.sendSuccess(() -> Component.literal("Remaining tourist arrivals scheduled for today:"), false);

        for (TourismManager.ScheduledTouristSpawn spawn : pendingSpawns) {
            MutableComponent message = Component.literal(" - " + TourismManager.getFriendlyTimeOfDay(spawn.timeOfDay()) + " for ");
            if (source.getServer().overworld().getBlockEntity(spawn.beaconPos()) instanceof TouristBeaconBlockEntity beaconBlockEntity) {
                message.append(beaconBlockEntity.getName().copy());
            } else {
                message.append(Component.literal("unknown beacon"));
            }
            message.append(Component.literal(" @ " + spawn.beaconPos().toShortString()));
            source.sendSuccess(() -> message, false);
        }
        return 1;
    }
}
