package org.bensam.touristry.tourism;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.bensam.touristry.ModEntities;
import org.bensam.touristry.Touristry;
import org.bensam.touristry.block.entity.AbstractExperienceBlockEntity;
import org.bensam.touristry.block.entity.TouristBeaconBlockEntity;
import org.bensam.touristry.config.ModServerConfigManager;
import org.bensam.touristry.config.Verbosity;
import org.bensam.touristry.entity.TouristEntity;
import org.bensam.touristry.tourism.experience.TouristExperience;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

public class TourismManager {
    public record ScheduledTouristSpawn(int timeOfDay, UUID beaconUUID) {}
    public static final Comparator<ScheduledTouristSpawn> SCHEDULED_TOURIST_SPAWN_COMPARATOR =
            Comparator.comparingInt(ScheduledTouristSpawn::timeOfDay);

    private static final int SPAWN_ATTEMPTS_PER_BEACON = 8;

    private static @Nullable TourismSavedData tourismSavedData;

    private static long lastDayThreshold = -1;
    private static long lastHourThreshold = -1;
    private static long lastPreparedDay = -1;
    private static int lastTickTimeOfDay = -1;

    private static final PriorityQueue<ScheduledTouristSpawn> pendingSpawns = new PriorityQueue<>(SCHEDULED_TOURIST_SPAWN_COMPARATOR);
    private static final Map<UUID, TouristBeaconBlockEntity> loadedTouristBeacons = new LinkedHashMap<>();
    private static final Map<UUID, TouristExperience> loadedExperiences = new LinkedHashMap<>();
    private static final Map<BlockPos, UUID> loadedExperiencesByPos = new HashMap<>();
    private static final Map<Integer, TouristEntity> loadedTourists = new LinkedHashMap<>();

    private static boolean despawnAllTourists = false;
    private static boolean clearDespawnAllNextTick = false;

    private TourismManager() {}

    private static void resetThresholds() {
        lastDayThreshold = -1;
        lastHourThreshold = -1;
        lastPreparedDay = -1;
        lastTickTimeOfDay = -1;
        tourismSavedData = null;
        despawnAllTourists = false;
        clearDespawnAllNextTick = false;
    }

    public static void initialize(ServerLevel serverLevel) {
        logActivity(Verbosity.LEVEL_2_DIAGNOSTICS, "Initializing TourismManager");
        if (serverLevel == null) {
            logActivity(Verbosity.ERRORS, "ServerLevel is null! TourismManager will not be initialized.");
            return;
        }

        resetThresholds();
        pendingSpawns.clear();
        loadedTouristBeacons.clear();
        loadedTourists.clear();
        loadPersistentState(serverLevel);
    }

    public static void shutdown() {
        persistPendingSpawns();
        resetThresholds();
        pendingSpawns.clear();
        loadedTouristBeacons.clear();
        loadedTourists.clear();
    }

    protected static void logActivity(Verbosity verbosityLevel, String message) {
        Verbosity verbosityConfig = ModServerConfigManager.getConfig().tourismManagerConfig().getVerbosityLevel();
        if (verbosityLevel == Verbosity.ERRORS) {
            Touristry.LOGGER.error("[TourismManager] {}", message);
        } else if (verbosityLevel.ordinal() <= verbosityConfig.ordinal()) {
            Touristry.LOGGER.info("[TourismManager] {}", message);
        } else {
            Touristry.LOGGER.debug("[TourismManager] {}", message);
        }
    }

    protected static void logActivity(Verbosity verbosityLevel, String message, Object... args) {
        Verbosity verbosityConfig = ModServerConfigManager.getConfig().tourismManagerConfig().getVerbosityLevel();
        if (verbosityLevel == Verbosity.ERRORS) {
            Touristry.LOGGER.error("[TourismManager] " + message, args);
        } else if (verbosityLevel.ordinal() <= verbosityConfig.ordinal()) {
            Touristry.LOGGER.info("[TourismManager] " + message, args);
        } else {
            Touristry.LOGGER.debug("[TourismManager] " + message, args);
        }
    }

    private static void loadPersistentState(ServerLevel serverLevel) {
        tourismSavedData = serverLevel.getDataStorage().computeIfAbsent(TourismSavedData.TYPE);

        long currentDay = serverLevel.getDayCount();
        int currentTimeOfDay = (int)(serverLevel.getDayTime() % 24000L);
        if (tourismSavedData.getPreparedDay() != currentDay) {
            return;
        }

        lastPreparedDay = currentDay;
        for (TourismSavedData.PendingTouristSpawnData pendingTouristSpawnData : tourismSavedData.getPendingSpawns()) {
            if (pendingTouristSpawnData.timeOfDay() >= currentTimeOfDay) {
                pendingSpawns.add(new ScheduledTouristSpawn(
                        pendingTouristSpawnData.timeOfDay(),
                        pendingTouristSpawnData.beaconUUID()
                ));
            }
        }

        persistPendingSpawns();
    }

    private static void persistPendingSpawns() {
        if (tourismSavedData == null) {
            return;
        }

        tourismSavedData.setScheduleState(
                lastPreparedDay,
                pendingSpawns.stream()
                        .map(scheduledTouristSpawn -> new TourismSavedData.PendingTouristSpawnData(
                                scheduledTouristSpawn.timeOfDay(),
                                scheduledTouristSpawn.beaconUUID()
                        ))
                        .toList()
        );
    }

    public static Component getTouristBlockNameOrPos(Level level, TouristLocation locationType, BlockPos blockPos) {
        switch (locationType) {
            case BEACON -> {
                TouristBeaconBlockEntity beaconBlockEntity = getBeaconBlockEntity(level, blockPos);
                if (beaconBlockEntity != null) {
                    return beaconBlockEntity.getDisplayName();
                }
            }

            case EXPERIENCE -> {
                TouristExperience experienceBlockEntity = getTouristExperienceByPos(blockPos);
                if (experienceBlockEntity != null) {
                    return experienceBlockEntity.getDisplayName();
                }
            }

            default -> {} // fallthrough
        }

        return Component.literal(blockPos.toShortString());
    }

    //region Experience Helpers
    public static @Nullable AbstractExperienceBlockEntity findClosestExperienceEntity(BlockPos pos) {
        TouristExperience closestExperience = null;
        double closestDistanceSq = Double.MAX_VALUE;

        for (TouristExperience experience : loadedExperiences.values()) {
            double distanceSq = pos.distSqr(experience.getBlockPos());
            if (distanceSq < closestDistanceSq) {
                closestDistanceSq = distanceSq;
                closestExperience = experience;
            }
        }

        AbstractExperienceBlockEntity closestBlockEntity = null;
        if (closestExperience instanceof AbstractExperienceBlockEntity experienceBlockEntity) {
            closestBlockEntity = experienceBlockEntity;
        }
        return closestBlockEntity;
    }

    public static @Nullable TouristExperience getTouristExperienceByPos(BlockPos blockPos) {
        return loadedExperiences.get(loadedExperiencesByPos.get(blockPos));
    }

    public static @Nullable TouristExperience getTouristExperienceById(UUID uuid) {
        return loadedExperiences.get(uuid);
    }

    public static List<TouristExperience> getTouristExperiences(ServerLevel serverLevel) {
        pruneInvalidTouristExperiences(serverLevel);
        return List.copyOf(loadedExperiences.values());
    }

    public static List<TouristExperience> getTouristExperiencesNearBeacon(ServerLevel serverLevel, TouristBeaconBlockEntity beaconBlockEntity) {
        BlockPos beaconPos = beaconBlockEntity.getBlockPos();
        double radius = ModServerConfigManager.getConfig().tourismManagerConfig().getMaxExperienceDistanceToBeacon();
        double radiusSq = radius * radius;

        pruneInvalidTouristExperiences(serverLevel);

        return loadedExperiences.values().stream()
                .filter(exp -> exp.getParentExperienceUUID() == null)
                .filter(exp -> exp.getBlockPos().distSqr(beaconPos) <= radiusSq)
                .sorted(Comparator.comparingDouble(exp -> exp.getBlockPos().distSqr(beaconPos)))
                .toList();
    }

    public static void pruneInvalidTouristExperiences(ServerLevel serverLevel) {
        loadedExperiences.entrySet().removeIf(entry -> {
            TouristExperience experience = entry.getValue();
            BlockEntity blockEntity = serverLevel.getBlockEntity(experience.getBlockPos());

            if (!(blockEntity instanceof TouristExperience) || blockEntity.isRemoved()) {
                // Remove from BlockPos index too.
                loadedExperiencesByPos.remove(experience.getBlockPos());
                return true;
            }

            return false;
        });
    }

    public static void registerTouristExperience(TouristExperience experience) {
        if (experience instanceof BlockEntity blockEntity && blockEntity.getLevel() instanceof ServerLevel serverLevel) {
            if (serverLevel != serverLevel.getServer().overworld()) {
                return;
            }
        }

        // Check if a different UUID already exists at this position.
        // This handles the item placement lifecycle:
        // 1. clearRemoved() registers with temporary UUID from constructor
        // 2. applyImplicitComponents() re-registers with actual UUID from item components
        UUID existingUUID = loadedExperiencesByPos.get(experience.getBlockPos());
        if (existingUUID != null && !existingUUID.equals(experience.getUUID())) {
            // Different UUID at same position - remove the old stale entry
            TouristExperience oldExperience = loadedExperiences.remove(existingUUID);
            if (oldExperience != null) {
                logActivity(Verbosity.LEVEL_2_DIAGNOSTICS, 
                    "[TourismManager] Replaced stale registration at {} (old UUID: {}, new UUID: {})", 
                    experience.getBlockPos().toShortString(), existingUUID, experience.getUUID());
            }
        }

        loadedExperiences.put(experience.getUUID(), experience);
        loadedExperiencesByPos.put(experience.getBlockPos(), experience.getUUID());
    }

    public static void unregisterTouristExperience(TouristExperience experience) {
        TouristExperience removedExperience = loadedExperiences.remove(experience.getUUID());
        if (removedExperience != null) {
            loadedExperiencesByPos.remove(removedExperience.getBlockPos());
        }
    }
    //endregion

    //region Tourist Beacon Helpers
    public static @Nullable TouristBeaconBlockEntity findClosestBeaconEntity(BlockPos pos) {
        TouristBeaconBlockEntity closest = null;
        double closestDistanceSq = Double.MAX_VALUE;

        for (TouristBeaconBlockEntity beaconBlockEntity : loadedTouristBeacons.values()) {
            double distanceSq = pos.distSqr(beaconBlockEntity.getBlockPos());
            if (distanceSq < closestDistanceSq) {
                closestDistanceSq = distanceSq;
                closest = beaconBlockEntity;
            }
        }

        return closest;
    }

    public static @Nullable TouristBeaconBlockEntity getBeaconBlockEntity(Level level, BlockPos pos) {
        if (level == null || pos == null) {
            return null;
        }

        if (level.getBlockEntity(pos) instanceof TouristBeaconBlockEntity beaconBlockEntity) {
            return beaconBlockEntity;
        }

        return null;
    }

    public static List<TouristBeaconBlockEntity> getTouristBeacons(ServerLevel serverLevel) {
        pruneInvalidTouristBeacons(serverLevel);
        return List.copyOf(loadedTouristBeacons.values());
    }

    public static List<TouristBeaconBlockEntity> getTouristBeaconsByDistance(ServerLevel serverLevel, BlockPos pos) {
        return getTouristBeaconsByDistance(serverLevel, pos, beaconBlockEntity -> true);
    }

    public static List<TouristBeaconBlockEntity> getTouristBeaconsByDistance(
            ServerLevel serverLevel,
            BlockPos pos,
            Predicate<TouristBeaconBlockEntity> filter
    ) {
        pruneInvalidTouristBeacons(serverLevel);
        return loadedTouristBeacons.values().stream()
                .filter(filter)
                .sorted(Comparator.comparingDouble(beaconBlockEntity -> pos.distSqr(beaconBlockEntity.getBlockPos())))
                .toList();
    }

    private static void pruneInvalidTouristBeacons(ServerLevel serverLevel) {
        loadedTouristBeacons.entrySet().removeIf(entry -> {
            TouristBeaconBlockEntity beaconBlockEntity = entry.getValue();
            return beaconBlockEntity.isRemoved()
                    || beaconBlockEntity.getLevel() != serverLevel
                    || serverLevel.getBlockEntity(beaconBlockEntity.getBlockPos()) != beaconBlockEntity;
        });
    }

    public static void registerTouristBeacon(TouristBeaconBlockEntity beaconBlockEntity) {
        if (!(beaconBlockEntity.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (serverLevel != serverLevel.getServer().overworld()) {
            return;
        }

        loadedTouristBeacons.entrySet().removeIf(entry -> entry.getValue() == beaconBlockEntity);
        loadedTouristBeacons.put(beaconBlockEntity.getUUID(), beaconBlockEntity);
    }

    public static void unregisterTouristBeacon(TouristBeaconBlockEntity beaconBlockEntity) {
        loadedTouristBeacons.entrySet().removeIf(entry -> entry.getValue() == beaconBlockEntity);
    }
    
    public static @Nullable TouristBeaconBlockEntity getBeaconBlockEntityByUUID(UUID beaconUUID) {
        return loadedTouristBeacons.get(beaconUUID);
    }
    //endregion

    //region Tourist Helpers
    public static void registerTourist(TouristEntity touristEntity) {
        if (!(touristEntity.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (serverLevel != serverLevel.getServer().overworld()) {
            return;
        }

        loadedTourists.put(touristEntity.getId(), touristEntity);
    }

    public static void unregisterTourist(TouristEntity touristEntity) {
        loadedTourists.remove(touristEntity.getId(), touristEntity);
    }

    public static void setForceDespawnAll() {
        despawnAllTourists = true;
        clearDespawnAllNextTick = false;
    }

    public static boolean shouldForceDespawn(TouristEntity touristEntity) {
        return despawnAllTourists;
    }
    //endregion

    public static void tick(ServerLevel serverLevel) {
        long dayCount = serverLevel.getDayCount();
        long dayTime = serverLevel.getDayTime();
        int tickTimeOfDay = (int)(dayTime % 24000L);
        int tickHour = tickTimeOfDay / 1000;

        // (Informational only) Write hourly heartbeat to log.
        if (dayCount > lastDayThreshold || tickHour > lastHourThreshold) {
            lastDayThreshold = dayCount;
            lastHourThreshold = tickHour;
            logActivity(Verbosity.LEVEL_2_DIAGNOSTICS, "Minecraft Day: {}; Time: {}; Ticks: {}", dayCount, getFriendlyTimeOfDay(dayTime), dayTime);
        }

        if (despawnAllTourists) {
            if (clearDespawnAllNextTick) {
                despawnAllTourists = false;
                clearDespawnAllNextTick = false;
            } else {
                clearDespawnAllNextTick = true;
            }
        } else {
            // Prepare spawn schedule for the new day.
            if (dayCount > lastPreparedDay) {
                prepareSpawnTimes(serverLevel, tickTimeOfDay);
            }
            if (lastPreparedDay != dayCount) {
                lastPreparedDay = dayCount; // we never want the lastPreparedDay to get *ahead* of the actual day count on the server (e.g. from a time set command)
                persistPendingSpawns();
            }

            // Spawn tourists throughout the day according to schedule.
            while (!pendingSpawns.isEmpty() && tickTimeOfDay >= pendingSpawns.peek().timeOfDay()) {
                pruneInvalidTouristBeacons(serverLevel);
                ScheduledTouristSpawn scheduledTouristSpawn = pendingSpawns.poll();
                persistPendingSpawns();
                spawnTouristOnSchedule(serverLevel, scheduledTouristSpawn);
            }
        }

        lastTickTimeOfDay = tickTimeOfDay;
    }

    //region Spawn Helpers
    public static void clearSpawnSchedule() {
        pendingSpawns.clear();
        persistPendingSpawns();
    }

    public static void resetSpawnSchedule() {
        pendingSpawns.clear();
        lastPreparedDay = -1;
        persistPendingSpawns();
    }

    public static String getFriendlyTimeOfDay(long dayTimeTicks) {
        int tickTimeOfDay = (int)(dayTimeTicks % 24000L);
        int tickHour = tickTimeOfDay / 1000;
        int ticksIntoHour = tickTimeOfDay % 1000;
        int minutes = ticksIntoHour * 60 / 1000;
        int hour24 = (tickHour + 6) % 24;
        int hour = hour24 % 12;
        String ampm = hour24 < 12 ? "AM" : "PM";
        if (hour == 0) {
            hour = 12;
        }

        return String.format("%d:%02d %s", hour, minutes, ampm);
    }

    public static List<ScheduledTouristSpawn> getPendingSpawns() {
        return pendingSpawns.stream()
                .sorted(SCHEDULED_TOURIST_SPAWN_COMPARATOR)
                .toList();
    }

    private static void prepareSpawnTimes(ServerLevel serverLevel, int currentTickTime) {
        pendingSpawns.clear();
        RandomSource random = serverLevel.getRandom();
        int latestSpawnTimeExclusive = Math.max(1, ModServerConfigManager.getConfig().tourismManagerConfig().getLatestSpawnTimeTicks() + 1) % 24000;

        if (currentTickTime >= latestSpawnTimeExclusive) {
            logActivity(Verbosity.GAMEPLAY_WARNINGS, "Too late in the day to add spawn times");
            return;
        }

        int earliestSpawnTime = Math.max(0, ModServerConfigManager.getConfig().tourismManagerConfig().getEarliestSpawnTimeTicks()) % 24000;
        int effectiveStartTime = Math.max(currentTickTime, earliestSpawnTime);
        int windowLength = Math.max(1, latestSpawnTimeExclusive - earliestSpawnTime);
        int remainingWindow = Math.max(0, latestSpawnTimeExclusive - effectiveStartTime);
        double remainingFraction = (double) remainingWindow / windowLength;
        int spawnCount = Math.max(1, (int) Math.ceil(ModServerConfigManager.getConfig().tourismManagerConfig().getMaxSpawnsPerBeaconPerDay() * remainingFraction));

        for (TouristBeaconBlockEntity beaconBlockEntity : getTouristBeacons(serverLevel)) {
            if (!beaconBlockEntity.isOpenForBusiness()) {
                continue;
            }
            UUID beaconUUID = beaconBlockEntity.getUUID();
            BlockPos beaconPos = beaconBlockEntity.getBlockPos();
            Set<Integer> spawnTimes = new LinkedHashSet<>(spawnCount);

            while (spawnTimes.size() < spawnCount) {
                spawnTimes.add(random.nextInt(effectiveStartTime, latestSpawnTimeExclusive));
            }

            for (int spawnTime : spawnTimes) {
                pendingSpawns.add(new ScheduledTouristSpawn(spawnTime, beaconUUID));
                logActivity(Verbosity.LEVEL_2_DIAGNOSTICS,
                        "Added pending spawn for {} at {} @ time {} ticks ({})",
                        beaconBlockEntity.getPlainTextName(),
                        beaconPos,
                        spawnTime,
                        getFriendlyTimeOfDay(spawnTime)
                );
            }
        }

        lastPreparedDay = serverLevel.getDayCount();
        persistPendingSpawns();
    }

    private static void spawnTouristOnSchedule(ServerLevel serverLevel, ScheduledTouristSpawn scheduledTouristSpawn) {
        TouristBeaconBlockEntity beaconBlockEntity = loadedTouristBeacons.get(scheduledTouristSpawn.beaconUUID());
        if (beaconBlockEntity == null) {
            // Beacon has been unloaded, moved, or removed. This spawn is skipped.
            logActivity(Verbosity.LEVEL_2_DIAGNOSTICS,
                    "Beacon UUID {} not found for scheduled spawn at time {} ticks ({})",
                    scheduledTouristSpawn.beaconUUID(),
                    scheduledTouristSpawn.timeOfDay(),
                    getFriendlyTimeOfDay(scheduledTouristSpawn.timeOfDay())
            );
            return;
        }

        BlockPos beaconPos = beaconBlockEntity.getBlockPos();

        if (!beaconBlockEntity.isOpenForBusiness()) {
            beaconBlockEntity.rateVisit(VisitResult.CLOSED_EARLY);
            return;
        }

        TouristEntity tourist = ModEntities.TOURIST.get().create(serverLevel, EntitySpawnReason.EVENT);
        if (tourist == null) {
            return;
        }

        BlockPos spawnPoint = getSpawnPoint(serverLevel, beaconPos, tourist);
        if (spawnPoint == null) {
            beaconBlockEntity.rateVisit(VisitResult.FAILED_SPAWN);
            logActivity(Verbosity.GAMEPLAY_WARNINGS,
                    "No safe spawn point found for {} at {} for scheduled time {} ticks ({})",
                    beaconBlockEntity.getPlainTextName(),
                    beaconPos,
                    scheduledTouristSpawn.timeOfDay(),
                    getFriendlyTimeOfDay(scheduledTouristSpawn.timeOfDay())
            );
            return;
        }

        logActivity(Verbosity.MAJOR_EVENTS,
                "Spawning tourist at {} for {} at {} for scheduled time {} ticks ({})",
                spawnPoint,
                beaconBlockEntity.getPlainTextName(),
                beaconPos,
                scheduledTouristSpawn.timeOfDay(),
                getFriendlyTimeOfDay(scheduledTouristSpawn.timeOfDay())
        );

        tourist.snapTo(spawnPoint, serverLevel.random.nextFloat() * 360.0F, 0.0F);
        tourist.getMind().prepareForJourney(beaconPos);
        tourist.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(tourist.blockPosition()), EntitySpawnReason.EVENT, null);
        // TODO: Implement random tourist names (ensuring name isn't currently in use)
        tourist.setCustomName(Component.literal("Ned Flanders"));
        serverLevel.addFreshEntity(tourist);
    }

    public static boolean trySpawnTouristForBeacon(ServerLevel serverLevel, @Nullable BlockPos requestedSpawnPoint, @NonNull TouristBeaconBlockEntity beaconBlockEntity) {
        TouristEntity tourist = ModEntities.TOURIST.get().create(serverLevel, EntitySpawnReason.COMMAND);
        if (tourist == null) {
            return false;
        }

        BlockPos spawnPoint = (requestedSpawnPoint != null) ? requestedSpawnPoint : getSpawnPoint(serverLevel, beaconBlockEntity.getBlockPos(), tourist);
        if (spawnPoint == null) {
            logActivity(Verbosity.GAMEPLAY_WARNINGS,
                    "No safe spawn point found for {} at {}",
                    beaconBlockEntity.getPlainTextName(),
                    beaconBlockEntity.getBlockPos()
            );
            return false;
        }

        logActivity(Verbosity.MAJOR_EVENTS,
                "Spawning tourist at {} for {} at {} by command",
                spawnPoint,
                beaconBlockEntity.getPlainTextName(),
                beaconBlockEntity.getBlockPos()
        );

        tourist.snapTo(spawnPoint, serverLevel.random.nextFloat() * 360.0F, 0.0F);
        tourist.getMind().prepareForJourney(beaconBlockEntity.getBlockPos());
        tourist.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(tourist.blockPosition()), EntitySpawnReason.COMMAND, null);
        tourist.setCustomName(Component.literal("Tassian Candor"));
        serverLevel.addFreshEntity(tourist);
        return true;
    }

    public static @Nullable BlockPos getSpawnPoint(ServerLevel serverLevel, BlockPos beaconPos, Entity touristEntity) {
        RandomSource random = serverLevel.getRandom();
        int minSpawnDistanceToBeacon = Math.max(0, ModServerConfigManager.getConfig().tourismManagerConfig().getMinSpawnDistanceToBeacon());
        int spawnDistanceRangeDelta = Math.max(0, ModServerConfigManager.getConfig().tourismManagerConfig().getMaxSpawnDistanceToBeacon() - minSpawnDistanceToBeacon);

        for (int attempt = 0; attempt < SPAWN_ATTEMPTS_PER_BEACON; attempt++) {
            double distance = minSpawnDistanceToBeacon + (random.nextDouble() * spawnDistanceRangeDelta);
            double angle = random.nextDouble() * (Math.PI * 2.0D);
            int offsetX = (int)Math.round(Math.cos(angle) * distance);
            int offsetZ = (int)Math.round(Math.sin(angle) * distance);

            if (offsetX == 0 && offsetZ == 0) {
                continue;
            }

            int spawnX = beaconPos.getX() + offsetX;
            int spawnZ = beaconPos.getZ() + offsetZ;
            int spawnY = serverLevel.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawnX, spawnZ);
            BlockPos spawnPos = new BlockPos(spawnX, spawnY, spawnZ);

            if (isSafeSpawnPoint(serverLevel, spawnPos, touristEntity)) {
                return spawnPos;
            }
        }

        return null;
    }

    private static boolean isSafeSpawnPoint(ServerLevel serverLevel, BlockPos spawnPos, Entity touristEntity) {
        BlockPos groundPos = spawnPos.below();
        BlockState groundState = serverLevel.getBlockState(groundPos);
        if (groundState.isAir() || !groundState.entityCanStandOn(serverLevel, groundPos, touristEntity)) {
            return false;
        }

        BlockState feetState = serverLevel.getBlockState(spawnPos);
        if (!feetState.getCollisionShape(serverLevel, spawnPos).isEmpty() || !serverLevel.getFluidState(spawnPos).isEmpty()) {
            return false;
        }

        BlockPos headPos = spawnPos.above();
        BlockState headState = serverLevel.getBlockState(headPos);
        return headState.getCollisionShape(serverLevel, headPos).isEmpty() && serverLevel.getFluidState(headPos).isEmpty();
    }
    //endregion
}
