package org.bensam.touristry.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.bensam.touristry.Touristry;
import org.bensam.touristry.block.entity.TouristBeaconBlockEntity;
import org.bensam.touristry.tourism.TourismManager;
import org.bensam.touristry.tourism.TouristBeaconExperience;
import org.bensam.touristry.tourism.TouristBeaconStats;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("SameReturnValue")
public final class PlayerCommands {
    private PlayerCommands() {}

    public static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("beacon")
                .then(addBeaconActions(
                        Commands.argument("beaconPos", BlockPosArgument.blockPos()),
                        ctx -> TourCommand.requireBeacon(
                                ctx.getSource(),
                                BlockPosArgument.getLoadedBlockPos(ctx, "beaconPos"))))
                .then(addBeaconActions(
                        Commands.literal("nearest"),
                        ctx -> TourCommand.requireNearestBeacon(ctx.getSource()))));

        root.then(Commands.literal("list")
                .then(Commands.literal("beacons")
                        .executes(ctx -> listBeacons(ctx.getSource())))
                .then(Commands.literal("touristSchedule")
                        .executes(ctx -> listTouristSchedule(ctx.getSource()))));

        root.then(Commands.literal("next")
                .executes(ctx -> showNextSpawn(ctx.getSource())));

        root.then(Commands.literal("now")
                .executes(ctx -> showTimeAndDay(ctx.getSource()))
        );
    }

    private static <T extends ArgumentBuilder<CommandSourceStack, T>> T addBeaconActions(
            T parent,
            TourCommand.BeaconResolver resolver
    ) {
        return parent
                .then(Commands.literal("info")
                        .executes(ctx -> showInfo(
                                ctx.getSource(),
                                resolver.resolve(ctx))))
                .then(Commands.literal("toggleStatus")
                        .executes(ctx -> toggleBeaconStatus(
                                ctx.getSource(),
                                resolver.resolve(ctx))));
    }

    private static int showInfo(CommandSourceStack source, TouristBeaconBlockEntity beaconBlockEntity) {
        // beacon name
        Component message = beaconBlockEntity.getName().copy()
                .append(Component.literal(" @ " + beaconBlockEntity.getBlockPos().toShortString() + " info:"));
        source.sendSuccess(() -> message, false);

        // business status
        TouristBeaconExperience experience = beaconBlockEntity.getBeaconExperience();
        source.sendSuccess(() -> Component.literal(" - status: " + (experience.beaconOpenForBusiness() ? "open for business" : "closed for business")), false);

        // experiences
        source.sendSuccess(() -> Component.literal(
                " - experiences: " + experience.experiences().size() + " / " + experience.experienceSlots()),
                false);

        // stats
        TouristBeaconStats stats = beaconBlockEntity.getBeaconStats();
        source.sendSuccess(() -> Component.literal(
                " - reputation: " + String.format("%.2f", stats.reputation())
                        + "; successful visits: " + stats.successfulVisits()),
                false);
        source.sendSuccess(() -> Component.literal(
                " - failed spawns: " + stats.failedSpawns()
                        + "; navigation failures: " + stats.navFailures()
                        + "; closed early: " + stats.closedEarly()),
                false);
        source.sendSuccess(() -> Component.literal(
                " - tourists hurt: " + stats.touristsHurt()
                        + "; tourists killed: " + stats.touristsKilled()),
                false);

        // inventory
        MutableComponent inventoryMessage = Component.literal(" - inventory: ");
        Map<Item, Integer> totals = new LinkedHashMap<>();
        for (int i = 0; i < beaconBlockEntity.getPaymentSlotSize(); ++i) {
            ItemStack itemStack = beaconBlockEntity.getItem(i);
            if (!itemStack.isEmpty()) {
                totals.merge(itemStack.getItem(), itemStack.getCount(), Integer::sum);
            }
        }
        if (totals.isEmpty()) {
            inventoryMessage.append(Component.literal("empty"));
        } else {
            boolean first = true;
            for (Map.Entry<Item, Integer> entry : totals.entrySet()) {
                if (!first) {
                    inventoryMessage.append("; ");
                }
                inventoryMessage.append(entry.getKey().getName().copy());
                inventoryMessage.append(": " + entry.getValue());
                first = false;
            }
        }
        source.sendSuccess(() -> inventoryMessage, false);

        // uuid
        source.sendSuccess(() -> Component.literal(" - uuid: " + beaconBlockEntity.getUUID().toString()), false);

        return 1;
    }

    private static int toggleBeaconStatus(CommandSourceStack source, TouristBeaconBlockEntity beaconBlockEntity) {
        beaconBlockEntity.setOpenForBusiness(!beaconBlockEntity.isOpenForBusiness());

        Component message = beaconBlockEntity.getName().copy()
                .append(Component.literal(" @ " + beaconBlockEntity.getBlockPos().toShortString() + " is now "))
                .append(Component.translatable("message." + Touristry.MOD_ID
                        + (beaconBlockEntity.isOpenForBusiness() ? ".tourist_beacon.open_for_business" : ".tourist_beacon.closed_for_business")));
        source.sendSuccess(() -> message,  false);
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
                    .append(Component.literal(" @ " + beaconBlockEntity.getBlockPos().toShortString() + " ("))
                    .append(Component.translatable("message." + Touristry.MOD_ID
                            + (beaconBlockEntity.isOpenForBusiness() ? ".tourist_beacon.open_for_business" : ".tourist_beacon.closed_for_business")))
                    .append(Component.literal(")"));
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
}
