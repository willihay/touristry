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
import org.bensam.touristry.block.entity.AbstractExperienceBlockEntity;
import org.bensam.touristry.block.entity.TouristBeaconBlockEntity;
import org.bensam.touristry.tourism.TourismManager;
import org.bensam.touristry.tourism.experience.ExperienceTarget;
import org.bensam.touristry.tourism.experience.TouristExperience;
import org.bensam.touristry.tourism.experience.TouristLocationStats;

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

        root.then(Commands.literal("experience")
                .then(addExperienceActions(
                        Commands.argument("experiencePos", BlockPosArgument.blockPos()),
                        ctx -> TourCommand.requireExperience(
                                ctx.getSource(),
                                BlockPosArgument.getLoadedBlockPos(ctx, "experiencePos"))))
                .then(addExperienceActions(
                        Commands.literal("nearest"),
                        ctx -> TourCommand.requireNearestExperience(ctx.getSource()))));

        root.then(Commands.literal("list")
                .then(Commands.literal("beacons")
                        .executes(ctx -> listBeacons(ctx.getSource())))
                .then(Commands.literal("experiences")
                        .executes(ctx -> listExperiences(ctx.getSource())))
                .then(Commands.literal("touristSchedule")
                        .executes(ctx -> listTouristSchedule(ctx.getSource())))
                .then(Commands.literal("touristStatistics")
                        .executes(ctx -> listTouristStatistics(ctx.getSource()))));

        root.then(Commands.literal("next")
                .executes(ctx -> showNextSpawn(ctx.getSource())));

        root.then(Commands.literal("now")
                .executes(ctx -> showTimeAndDay(ctx.getSource()))
        );
    }

    private static void showStats(CommandSourceStack source, TouristLocationStats stats, boolean showFailedSpawns) {
        source.sendSuccess(() -> Component.literal(
                        " - reputation: " + String.format("%.2f", stats.getReputation())
                                + "; total visits: " + stats.getTotalVisits()),
                false);
        source.sendSuccess(() -> Component.literal(
                        " - completed visits: " + stats.getCompletedVisits()
                                + "; abandoned visits: " + stats.getAbandonedVisits()),
                false);
        if (showFailedSpawns) {
            source.sendSuccess(() -> Component.literal(
                    " - failed spawns: " + stats.getFailedSpawns()
                            + "; navigation failures: " + stats.getNavFailures()),
                    false);
        }
        source.sendSuccess(() -> Component.literal(
                        " - closed early: " + stats.getClosedEarly()
                                + "; failed payments: " + stats.getPaymentFailed()),
                false);
        source.sendSuccess(() -> Component.literal(
                        " - tourists hurt: " + stats.getTouristsHurt()
                                + "; tourists killed: " + stats.getTouristsKilled()),
                false);
        long lastVisitTicks = stats.getLastVisitTime();
        String lastVisit = lastVisitTicks == 0 ? "never" : TourismManager.getFriendlyTimeOfDay(lastVisitTicks);
        source.sendSuccess(() -> Component.literal(
                        " - last visit: " + lastVisit),
                false);
    }

    private static <T extends ArgumentBuilder<CommandSourceStack, T>> T addBeaconActions(
            T parent,
            TourCommand.BeaconResolver resolver
    ) {
        return parent
                .then(Commands.literal("info")
                        .executes(ctx -> showBeaconInfo(
                                ctx.getSource(),
                                resolver.resolve(ctx))))
                .then(Commands.literal("toggleStatus")
                        .executes(ctx -> toggleBeaconStatus(
                                ctx.getSource(),
                                resolver.resolve(ctx))));
    }

    private static int showBeaconInfo(CommandSourceStack source, TouristBeaconBlockEntity beaconBlockEntity) {
        // beacon name
        Component message = beaconBlockEntity.getName().copy()
                .append(Component.literal(" @ " + beaconBlockEntity.getBlockPos().toShortString() + " info:"));
        source.sendSuccess(() -> message, false);

        // business status
        source.sendSuccess(() -> Component.literal(" - status: " + (beaconBlockEntity.isOpenForBusiness() ? "open for business" : "closed for business")), false);

        // nearby experiences
        List<TouristExperience> experiences = TourismManager.getTouristExperiencesNearBeacon(source.getLevel(), beaconBlockEntity);
        source.sendSuccess(() -> Component.literal(" - nearby experiences: " + experiences.size()), false);
        if (!experiences.isEmpty()) {
            for (TouristExperience experience : experiences) {
                MutableComponent experienceMessage = Component.literal("   - ")
                        .append(experience.getDisplayName());
                if (experience instanceof AbstractExperienceBlockEntity experienceBlockEntity) {
                    if (experienceBlockEntity.hasCustomName()) {
                        experienceMessage.append(Component.literal(" ("))
                                .append(experienceBlockEntity.getBlockState().getBlock().getName())
                                .append(Component.literal(")"));
                    }
                }
                experienceMessage.append(Component.literal(" @ " + experience.getBlockPos().toShortString()));
                source.sendSuccess(() -> experienceMessage, false);
            }
        }

        // stats
        TouristLocationStats stats = beaconBlockEntity.getStatistics();
        showStats(source, stats, true);

        // uuid
        source.sendSuccess(() -> Component.literal(" - uuid: " + beaconBlockEntity.getUUID().toString()), false);

        return 1;
    }

    private static int toggleBeaconStatus(CommandSourceStack source, TouristBeaconBlockEntity beaconBlockEntity) {
        beaconBlockEntity.setOpenForBusiness(!beaconBlockEntity.isOpenForBusiness());

        Component message = beaconBlockEntity.getName().copy()
                .append(Component.literal(" @ " + beaconBlockEntity.getBlockPos().toShortString() + " is now "))
                .append(Component.translatable("message." + Touristry.MOD_ID
                        + (beaconBlockEntity.isOpenForBusiness() ? ".tourist_block.status.open_for_business" : ".tourist_block.status.closed_for_business")));
        source.sendSuccess(() -> message,  false);
        return 1;
    }

    private static <T extends ArgumentBuilder<CommandSourceStack, T>> T addExperienceActions(
            T parent,
            TourCommand.ExperienceResolver resolver
    ) {
        return parent
                .then(Commands.literal("info")
                        .executes(ctx -> showExperienceInfo(
                                ctx.getSource(),
                                resolver.resolve(ctx))))
                .then(Commands.literal("toggleStatus")
                        .executes(ctx -> toggleExperienceStatus(
                                ctx.getSource(),
                                resolver.resolve(ctx))));
    }

    private static int showExperienceInfo(CommandSourceStack source, AbstractExperienceBlockEntity experienceBlockEntity) {
        // beacon name
        Component message = experienceBlockEntity.getName().copy()
                .append(Component.literal(" @ " + experienceBlockEntity.getBlockPos().toShortString() + " info:"));
        source.sendSuccess(() -> message, false);

        // business status
        source.sendSuccess(() -> Component.literal(" - status: " + (experienceBlockEntity.isOpenForBusiness() ? "open for business" : "closed for business")), false);

        // targets
        List<ExperienceTarget> targets = experienceBlockEntity.getTargets(source.getLevel());
        source.sendSuccess(() -> Component.literal(" - targets: " + targets.size()), false);
        for (ExperienceTarget target : targets) {
            Component targetMessage = Component.literal("   - ")
                    .append(target.getDisplayName(source.getLevel()))
                    .append(Component.literal(" @ " + target.pos().toShortString()));
            source.sendSuccess(() -> targetMessage, false);
        }

        // stats
        TouristLocationStats stats = experienceBlockEntity.getStatistics();
        showStats(source, stats, false);

        // inventory
        MutableComponent inventoryMessage = Component.literal(" - inventory: ");
        Map<Item, Integer> totals = new LinkedHashMap<>();
        for (int i = 0; i < experienceBlockEntity.getPaymentSlotSize(); ++i) {
            ItemStack itemStack = experienceBlockEntity.getItem(i);
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
        source.sendSuccess(() -> Component.literal(" - uuid: " + experienceBlockEntity.getUUID().toString()), false);

        return 1;
    }

    private static int toggleExperienceStatus(CommandSourceStack source, AbstractExperienceBlockEntity experienceBlockEntity) {
        experienceBlockEntity.setOpenForBusiness(!experienceBlockEntity.isOpenForBusiness());

        Component message = experienceBlockEntity.getName().copy()
                .append(Component.literal(" @ " + experienceBlockEntity.getBlockPos().toShortString() + " is now "))
                .append(Component.translatable("message." + Touristry.MOD_ID
                        + (experienceBlockEntity.isOpenForBusiness() ? ".tourist_block.status.open_for_business" : ".tourist_block.status.closed_for_business")));
        source.sendSuccess(() -> message,  false);
        return 1;
    }

    private static int listBeacons(CommandSourceStack source) {
        List<TouristBeaconBlockEntity> loadedBeacons = TourismManager.getTouristBeacons(source.getServer().overworld());
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
                            + (beaconBlockEntity.isOpenForBusiness() ? ".tourist_block.status.open_for_business" : ".tourist_block.status.closed_for_business")))
                    .append(Component.literal(")"));
            source.sendSuccess(() -> message, false);
        }
        return 1;
    }

    private static int listExperiences(CommandSourceStack source) {
        List<TouristExperience> loadedExperiences = TourismManager.getTouristExperiences(source.getServer().overworld());
        if (loadedExperiences.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No tourist experiences found"), false);
            return 1;
        }

        source.sendSuccess(() -> Component.literal("Tourist experiences:"), false);

        for (TouristExperience experience : loadedExperiences) {
            Component message = Component.literal(" - ")
                    .append(experience.getDisplayName())
                    .append(" (" + experience.getClass().getSimpleName() + ")")
                    .append(Component.literal(" @ " + experience.getBlockPos().toShortString()));
            source.sendSuccess(() -> message, false);
        }
        return 1;
    }

    private static int listTouristSchedule(CommandSourceStack source) {
        List<TourismManager.ScheduledTouristSpawn> pendingSpawns = TourismManager.getPendingSpawns();
        if (pendingSpawns.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No remaining tourists scheduled for today"), false);
            return 1;
        }

        source.sendSuccess(() -> Component.literal("Remaining tourist arrivals scheduled for today:"), false);

        for (TourismManager.ScheduledTouristSpawn spawn : pendingSpawns) {
            MutableComponent message = Component.literal(" - " + TourismManager.getFriendlyTimeOfDay(spawn.timeOfDay()) + " for ");
            TouristBeaconBlockEntity beaconBlockEntity = TourismManager.getBeaconBlockEntityByUUID(spawn.beaconUUID());
            if (beaconBlockEntity != null) {
                message.append(beaconBlockEntity.getName().copy());
                message.append(Component.literal(" @ " + beaconBlockEntity.getBlockPos().toShortString()));
            } else {
                message.append(Component.literal("beacon UUID: " + spawn.beaconUUID()));
            }
            source.sendSuccess(() -> message, false);
        }
        return 1;
    }

    private static int listTouristStatistics(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("Highest tourist daily budget: " + TourismManager.getRecordHighestTouristBudget()), false);
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
        TouristBeaconBlockEntity beaconBlockEntity = TourismManager.getBeaconBlockEntityByUUID(nextSpawn.beaconUUID());
        if (beaconBlockEntity != null) {
            message.append(beaconBlockEntity.getName().copy());
            message.append(Component.literal(" @ " + beaconBlockEntity.getBlockPos().toShortString()));
        } else {
            message.append(Component.literal("beacon UUID: " + nextSpawn.beaconUUID()));
        }
        source.sendSuccess(() -> message, false);
        return 1;
    }

    private static int showTimeAndDay(CommandSourceStack source) {
        ServerLevel overworld = source.getServer().overworld();
        source.sendSuccess(() -> Component.literal("Current time: " + TourismManager.getFriendlyTimeOfDay(overworld.getDayTime()) + " on day " + overworld.getDayCount()), false);
        return 1;
    }
}
