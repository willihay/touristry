package org.bensam.touristry.tourism;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.bensam.touristry.ModEntities;
import org.bensam.touristry.Touristry;
import org.bensam.touristry.block.entity.TouristBeaconBlockEntity;
import org.bensam.touristry.entity.TouristEntity;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

public class TourismManager {
    public record ScheduledTouristSpawn(int timeOfDay, BlockPos beaconPos) {}
    public static final Comparator<ScheduledTouristSpawn> SCHEDULED_TOURIST_SPAWN_COMPARATOR =
            Comparator.comparingInt(ScheduledTouristSpawn::timeOfDay);

    private static final int SPAWNS_PER_BEACON_PER_DAY = 5;
    private static final int EARLIEST_SPAWN_TIME = 2000; // 8:00 AM
    private static final int LATEST_SPAWN_TIME_EXCLUSIVE = 9001; // 3:00 PM
    private static final int MIDNIGHT_DESPAWN_TIME = 18000; // 12:00 AM
    private static final double SPAWN_DISTANCE_FROM_BEACON_MIN = 35.0D;
    private static final double SPAWN_DISTANCE_FROM_BEACON_MAX = 70.0D;
    private static final double SPAWN_DISTANCE_RANGE_DELTA = SPAWN_DISTANCE_FROM_BEACON_MAX - SPAWN_DISTANCE_FROM_BEACON_MIN;
    private static final int SPAWN_ATTEMPTS_PER_BEACON = 8;

    private static @Nullable TourismSavedData tourismSavedData;

    private static long lastDayThreshold = -1;
    private static long lastHourThreshold = -1;
    private static long lastPreparedDay = -1;
    private static int lastTickTimeOfDay = -1;

    private static final PriorityQueue<ScheduledTouristSpawn> pendingSpawns = new PriorityQueue<>(SCHEDULED_TOURIST_SPAWN_COMPARATOR);
    private static final Map<BlockPos, TouristBeaconBlockEntity> loadedTouristBeacons = new LinkedHashMap<>();
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

    public static void initialize(ServerLevel overworld) {
        Touristry.LOGGER.info("Initializing TourismManager");
        if (overworld == null) {
            Touristry.LOGGER.error("Overworld is null! TourismManager will not be initialized.");
            return;
        }

        resetThresholds();
        pendingSpawns.clear();
        loadedTouristBeacons.clear();
        loadedTourists.clear();
        loadPersistentState(overworld);
    }

    public static void shutdown() {
        persistPendingSpawns();
        resetThresholds();
        pendingSpawns.clear();
        loadedTouristBeacons.clear();
        loadedTourists.clear();
    }

    private static void loadPersistentState(ServerLevel overworld) {
        tourismSavedData = overworld.getDataStorage().computeIfAbsent(TourismSavedData.TYPE);

        long currentDay = overworld.getDayCount();
        int currentTimeOfDay = (int)(overworld.getDayTime() % 24000L);
        if (tourismSavedData.getPreparedDay() != currentDay) {
            return;
        }

        lastPreparedDay = currentDay;
        for (TourismSavedData.PendingTouristSpawnData pendingTouristSpawnData : tourismSavedData.getPendingSpawns()) {
            if (pendingTouristSpawnData.timeOfDay() >= currentTimeOfDay) {
                pendingSpawns.add(new ScheduledTouristSpawn(
                        pendingTouristSpawnData.timeOfDay(),
                        pendingTouristSpawnData.beaconPos().immutable()
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
                                scheduledTouristSpawn.beaconPos().immutable()
                        ))
                        .toList()
        );
    }

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

    public static @Nullable TouristBeaconBlockEntity getBeaconBlockEntity(@NonNull Level level, @NonNull BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof TouristBeaconBlockEntity beaconBlockEntity) {
            return beaconBlockEntity;
        }
        return null;
    }

    public static List<TouristBeaconBlockEntity> getLoadedTouristBeacons(ServerLevel overworld) {
        pruneInvalidTouristBeacons(overworld);
        return List.copyOf(loadedTouristBeacons.values());
    }

    private static void pruneInvalidTouristBeacons(ServerLevel overworld) {
        loadedTouristBeacons.entrySet().removeIf(entry -> {
            TouristBeaconBlockEntity beaconBlockEntity = entry.getValue();
            return beaconBlockEntity.isRemoved()
                    || beaconBlockEntity.getLevel() != overworld
                    || overworld.getBlockEntity(entry.getKey()) != beaconBlockEntity;
        });
    }

    public static void registerTouristBeacon(TouristBeaconBlockEntity beaconBlockEntity) {
        if (!(beaconBlockEntity.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (serverLevel != serverLevel.getServer().overworld()) {
            return;
        }

        loadedTouristBeacons.put(beaconBlockEntity.getBlockPos(), beaconBlockEntity);
    }

    public static void unregisterTouristBeacon(TouristBeaconBlockEntity touristBeaconBlockEntity) {
        loadedTouristBeacons.remove(touristBeaconBlockEntity.getBlockPos(), touristBeaconBlockEntity);
    }
    //endregion

    //region Tourist Helpers
    private static void despawnTouristsAtMidnight(ServerLevel overworld) {
        pruneInvalidTourists(overworld);

        for (TouristEntity touristEntity : List.copyOf(loadedTourists.values())) {
            touristEntity.discard();
        }

        loadedTourists.clear();
        Touristry.LOGGER.info("[TourismManager] Despawned all loaded tourists at midnight");
    }

    private static void pruneInvalidTourists(ServerLevel overworld) {
        loadedTourists.entrySet().removeIf(entry -> {
            TouristEntity touristEntity = entry.getValue();
            return touristEntity.isRemoved()
                    || touristEntity.level() != overworld;
        });
    }

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
        if (despawnAllTourists) {
            return true;
        }

        // TODO: Implement single entity check for despawn.

        return false;
    }
    //endregion

    public static void tick(ServerLevel overworld) {
        long dayCount = overworld.getDayCount();
        long dayTime = overworld.getDayTime();
        int tickTimeOfDay = (int)(dayTime % 24000L);
        int tickHour = tickTimeOfDay / 1000;

        // (Informational only) Write hourly heartbeat to log.
        // TODO: Remove (or change to LOGGER.debug()) before publishing
        if (dayCount > lastDayThreshold || tickHour > lastHourThreshold) {
            lastDayThreshold = dayCount;
            lastHourThreshold = tickHour;
            Touristry.LOGGER.info("[TourismManager] Minecraft Day: {}; Time: {}; Ticks: {}", dayCount, getFriendlyTimeOfDay(dayTime), dayTime);
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
                prepareSpawnTimes(overworld, tickTimeOfDay);
            }
            if (lastPreparedDay != dayCount) {
                lastPreparedDay = dayCount; // we never want the lastPreparedDay to get *ahead* of the actual day count on the server (e.g. from a time set command)
                persistPendingSpawns();
            }

            // (Scaffolding, until tourists have autonomy) Despawn tourists at midnight.
            if (tickTimeOfDay >= MIDNIGHT_DESPAWN_TIME
                    && (lastTickTimeOfDay < MIDNIGHT_DESPAWN_TIME || lastTickTimeOfDay > tickTimeOfDay)) {
                despawnTouristsAtMidnight(overworld);
                // TODO: When this moves to tourist logic, record failed visits when applicable.
            }

            // Spawn tourists throughout the day according to schedule.
            while (!pendingSpawns.isEmpty() && tickTimeOfDay >= pendingSpawns.peek().timeOfDay()) {
                pruneInvalidTouristBeacons(overworld);
                ScheduledTouristSpawn scheduledTouristSpawn = pendingSpawns.poll();
                persistPendingSpawns();
                spawnTouristOnSchedule(overworld, scheduledTouristSpawn);
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

    private static void prepareSpawnTimes(ServerLevel world, int currentTickTime) {
        pendingSpawns.clear();
        RandomSource random = world.getRandom();

        if (currentTickTime >= LATEST_SPAWN_TIME_EXCLUSIVE) {
            Touristry.LOGGER.info("[TourismManager] Too late in the day to add spawn times");
            return;
        }

        for (TouristBeaconBlockEntity beaconBlockEntity : getLoadedTouristBeacons(world)) {
            int effectiveStartTime = Math.max(currentTickTime, EARLIEST_SPAWN_TIME);
            int windowLength = LATEST_SPAWN_TIME_EXCLUSIVE - EARLIEST_SPAWN_TIME;
            int remainingWindow = Math.max(0, LATEST_SPAWN_TIME_EXCLUSIVE - effectiveStartTime);

            double remainingFraction = (double) remainingWindow / windowLength;
            int spawnCount = Math.max(1, (int) Math.ceil(SPAWNS_PER_BEACON_PER_DAY * remainingFraction));

            BlockPos beaconPos = beaconBlockEntity.getBlockPos().immutable();
            Set<Integer> spawnTimes = new LinkedHashSet<>(spawnCount);

            while (spawnTimes.size() < spawnCount) {
                spawnTimes.add(random.nextInt(currentTickTime, LATEST_SPAWN_TIME_EXCLUSIVE));
            }

            for (int spawnTime : spawnTimes) {
                pendingSpawns.add(new ScheduledTouristSpawn(spawnTime, beaconPos));
                Touristry.LOGGER.info(
                        "[TourismManager] Added pending spawn for {} at {} @ time {} ticks ({})",
                        beaconBlockEntity.getPlainTextName(),
                        beaconPos,
                        spawnTime,
                        getFriendlyTimeOfDay(spawnTime)
                );
            }
        }

        lastPreparedDay = world.getDayCount();
        persistPendingSpawns();
    }

    private static void spawnTouristOnSchedule(ServerLevel world, ScheduledTouristSpawn scheduledTouristSpawn) {
        TouristBeaconBlockEntity beaconBlockEntity = loadedTouristBeacons.get(scheduledTouristSpawn.beaconPos());
        if (beaconBlockEntity == null) {
            return;
        }

        TouristEntity tourist = ModEntities.TOURIST.get().create(world, EntitySpawnReason.EVENT);
        if (tourist == null) {
            return;
        }

        BlockPos spawnPoint = getSpawnPoint(world, scheduledTouristSpawn.beaconPos(), tourist);
        if (spawnPoint == null) {
            beaconBlockEntity.rateVisit(VisitResult.FAILED_SPAWN);
            Touristry.LOGGER.warn(
                    "[TourismManager] No safe spawn point found for {} at {} for scheduled time {} ticks ({})",
                    beaconBlockEntity.getPlainTextName(),
                    scheduledTouristSpawn.beaconPos(),
                    scheduledTouristSpawn.timeOfDay(),
                    getFriendlyTimeOfDay(scheduledTouristSpawn.timeOfDay())
            );
            return;
        }

        Touristry.LOGGER.info(
                "[TourismManager] Spawning tourist at {} for {} at {} for scheduled time {} ticks ({})",
                spawnPoint,
                beaconBlockEntity.getPlainTextName(),
                scheduledTouristSpawn.beaconPos(),
                scheduledTouristSpawn.timeOfDay(),
                getFriendlyTimeOfDay(scheduledTouristSpawn.timeOfDay())
        );

        tourist.snapTo(spawnPoint, world.random.nextFloat() * 360.0F, 0.0F);
        tourist.setBeaconTarget(scheduledTouristSpawn.beaconPos());
        tourist.finalizeSpawn(world, world.getCurrentDifficultyAt(tourist.blockPosition()), EntitySpawnReason.EVENT, null);
        // TODO: Implement random tourist names (ensuring name isn't currently in use)
        tourist.setCustomName(Component.literal("Ned Flanders"));
        world.addFreshEntity(tourist);
    }

    public static boolean trySpawnTouristForBeacon(ServerLevel world, @Nullable BlockPos requestedSpawnPoint, @NonNull TouristBeaconBlockEntity beaconBlockEntity) {
        TouristEntity tourist = ModEntities.TOURIST.get().create(world, EntitySpawnReason.COMMAND);
        if (tourist == null) {
            return false;
        }

        BlockPos spawnPoint = (requestedSpawnPoint != null) ? requestedSpawnPoint : getSpawnPoint(world, beaconBlockEntity.getBlockPos(), tourist);
        if (spawnPoint == null) {
            Touristry.LOGGER.warn(
                    "[TourismManager] No safe spawn point found for {} at {}",
                    beaconBlockEntity.getPlainTextName(),
                    beaconBlockEntity.getBlockPos()
            );
            return false;
        }

        Touristry.LOGGER.info(
                "[TourismManager] Spawning tourist at {} for {} at {} by command",
                spawnPoint,
                beaconBlockEntity.getPlainTextName(),
                beaconBlockEntity.getBlockPos()
        );

        tourist.snapTo(spawnPoint, world.random.nextFloat() * 360.0F, 0.0F);
        tourist.setBeaconTarget(beaconBlockEntity.getBlockPos());
        tourist.finalizeSpawn(world, world.getCurrentDifficultyAt(tourist.blockPosition()), EntitySpawnReason.COMMAND, null);
        tourist.setCustomName(Component.literal("Tassian Candor"));
        world.addFreshEntity(tourist);
        return true;
    }

    public static @Nullable BlockPos getSpawnPoint(ServerLevel world, BlockPos beaconPos, Entity touristEntity) {
        RandomSource random = world.getRandom();

        for (int attempt = 0; attempt < SPAWN_ATTEMPTS_PER_BEACON; attempt++) {
            double distance = SPAWN_DISTANCE_FROM_BEACON_MIN + (random.nextDouble() * SPAWN_DISTANCE_RANGE_DELTA);
            double angle = random.nextDouble() * (Math.PI * 2.0D);
            int offsetX = (int)Math.round(Math.cos(angle) * distance);
            int offsetZ = (int)Math.round(Math.sin(angle) * distance);

            if (offsetX == 0 && offsetZ == 0) {
                continue;
            }

            int spawnX = beaconPos.getX() + offsetX;
            int spawnZ = beaconPos.getZ() + offsetZ;
            int spawnY = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawnX, spawnZ);
            BlockPos spawnPos = new BlockPos(spawnX, spawnY, spawnZ);

            if (isSafeSpawnPoint(world, spawnPos, touristEntity)) {
                return spawnPos;
            }
        }

        return null;
    }

    private static boolean isSafeSpawnPoint(ServerLevel world, BlockPos spawnPos, Entity touristEntity) {
        BlockPos groundPos = spawnPos.below();
        BlockState groundState = world.getBlockState(groundPos);
        if (groundState.isAir() || !groundState.entityCanStandOn(world, groundPos, touristEntity)) {
            return false;
        }

        BlockState feetState = world.getBlockState(spawnPos);
        if (!feetState.getCollisionShape(world, spawnPos).isEmpty() || !world.getFluidState(spawnPos).isEmpty()) {
            return false;
        }

        BlockPos headPos = spawnPos.above();
        BlockState headState = world.getBlockState(headPos);
        return headState.getCollisionShape(world, headPos).isEmpty() && world.getFluidState(headPos).isEmpty();
    }
    //endregion
}
