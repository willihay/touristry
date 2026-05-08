package org.bensam.touristry.tourism;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.bensam.touristry.ModEntities;
import org.bensam.touristry.Touristry;
import org.bensam.touristry.block.entity.TouristBeaconBlockEntity;
import org.bensam.touristry.entity.TouristEntity;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

public class TourismManager {
    private record ScheduledTouristSpawn(int timeOfDay, BlockPos beaconPos) {}
    private static final Comparator<ScheduledTouristSpawn> SCHEDULED_TOURIST_SPAWN_COMPARATOR =
            Comparator.comparingInt(ScheduledTouristSpawn::timeOfDay);

    private static final int SPAWNS_PER_BEACON_PER_DAY = 20;
    private static final int EARLIEST_SPAWN_TIME = 2000; // 8:00 AM
    private static final int LATEST_SPAWN_TIME_EXCLUSIVE = 9001; // 3:00 PM
    private static final int MIDNIGHT_DESPAWN_TIME = 18000; // 12:00 AM
    private static final double SPAWN_DISTANCE_FROM_BEACON = 50.0D;

    private static long lastDayThreshold = -1;
    private static long lastHourThreshold = -1;
    private static long lastPreparedDay = -1;
    private static int lastTickTimeOfDay = -1;

    private static final PriorityQueue<ScheduledTouristSpawn> pendingSpawns = new PriorityQueue<>(SCHEDULED_TOURIST_SPAWN_COMPARATOR);
    private static final Map<BlockPos, TouristBeaconBlockEntity> loadedTouristBeacons = new LinkedHashMap<>();
    private static final Map<Integer, TouristEntity> loadedTourists = new LinkedHashMap<>();

    private TourismManager() {}

    private static void resetThresholds() {
        lastDayThreshold = -1;
        lastHourThreshold = -1;
        lastPreparedDay = -1;
        lastTickTimeOfDay = -1;
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
    }

    public static void shutdown() {
        resetThresholds();
        pendingSpawns.clear();
        loadedTouristBeacons.clear();
        loadedTourists.clear();
    }

    //region Tourist Beacon Helpers
    public static List<TouristBeaconBlockEntity> getLoadedTouristBeacons(ServerLevel overworld) {
        pruneInvalidTouristBeacons(overworld);
        return List.copyOf(loadedTouristBeacons.values());
    }

    private static void pruneInvalidTouristBeacons(ServerLevel overworld) {
        loadedTouristBeacons.entrySet().removeIf(entry -> {
            TouristBeaconBlockEntity touristBeaconBlockEntity = entry.getValue();
            return touristBeaconBlockEntity.isRemoved()
                    || touristBeaconBlockEntity.getLevel() != overworld
                    || overworld.getBlockEntity(entry.getKey()) != touristBeaconBlockEntity;
        });
    }

    public static void registerTouristBeacon(TouristBeaconBlockEntity touristBeaconBlockEntity) {
        if (!(touristBeaconBlockEntity.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (serverLevel != serverLevel.getServer().overworld()) {
            return;
        }

        loadedTouristBeacons.put(touristBeaconBlockEntity.getBlockPos(), touristBeaconBlockEntity);
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
    //endregion

    public static void tick(ServerLevel overworld) {
        long dayCount = overworld.getDayCount();
        long dayTime = overworld.getDayTime();
        int tickTimeOfDay = (int)(dayTime % 24000L);
        int tickHour = tickTimeOfDay / 1000;
        int hour24 = (tickHour + 6) % 24;
        int hour = hour24 % 12;
        String ampm = hour24 < 12 ? "AM" : "PM";
        if (hour24 == 0) {
            hour = 12;
        }

        // (Informational only) Write hourly heartbeat to log.
        if (dayCount > lastDayThreshold || tickHour > lastHourThreshold) {
            lastDayThreshold = dayCount;
            lastHourThreshold = tickHour;
            Touristry.LOGGER.info("[TourismManager] Minecraft Day: {}; Time: {} {}; DayTime: {}", lastDayThreshold, hour, ampm, dayTime);
        }

        // Prepare spawn schedule for the new day.
        if (dayCount > lastPreparedDay) {
            lastPreparedDay = dayCount;
            prepareSpawnTimes(overworld);
        }

        // (Scaffolding, until tourists have autonomy) Despawn tourists at midnight.
        if (tickTimeOfDay >= MIDNIGHT_DESPAWN_TIME
                && (lastTickTimeOfDay < MIDNIGHT_DESPAWN_TIME || lastTickTimeOfDay > tickTimeOfDay)) {
            despawnTouristsAtMidnight(overworld);
        }

        // Spawn tourists throughout the day according to schedule.
        while (!pendingSpawns.isEmpty() && tickTimeOfDay >= pendingSpawns.peek().timeOfDay()) {
            pruneInvalidTouristBeacons(overworld);
            spawnTourist(overworld, pendingSpawns.poll());
        }

        lastTickTimeOfDay = tickTimeOfDay;
    }

    //region Spawn Helpers
    private static void prepareSpawnTimes(ServerLevel world) {
        pendingSpawns.clear();
        RandomSource random = world.getRandom();

        for (TouristBeaconBlockEntity touristBeaconBlockEntity : getLoadedTouristBeacons(world)) {
            BlockPos beaconPos = touristBeaconBlockEntity.getBlockPos().immutable();
            int availableSpawnTimes = LATEST_SPAWN_TIME_EXCLUSIVE - EARLIEST_SPAWN_TIME;
            int spawnCount = Math.min(SPAWNS_PER_BEACON_PER_DAY, availableSpawnTimes);
            Set<Integer> spawnTimes = new LinkedHashSet<>(spawnCount);

            while (spawnTimes.size() < spawnCount) {
                spawnTimes.add(random.nextInt(EARLIEST_SPAWN_TIME, LATEST_SPAWN_TIME_EXCLUSIVE));
            }

            for (int spawnTime : spawnTimes) {
                pendingSpawns.add(new ScheduledTouristSpawn(spawnTime, beaconPos));
                Touristry.LOGGER.info(
                        "[TourismManager] Added pending spawn for beacon {} at time {}",
                        beaconPos,
                        spawnTime
                );
            }
        }
    }

    private static @Nullable BlockPos getSpawnPoint(ServerLevel world, BlockPos beaconPos, Entity touristEntity) {
        RandomSource random = world.getRandom();

        for (int attempt = 0; attempt < 8; attempt++) {
            double angle = random.nextDouble() * (Math.PI * 2.0D);
            int offsetX = (int)Math.round(Math.cos(angle) * SPAWN_DISTANCE_FROM_BEACON);
            int offsetZ = (int)Math.round(Math.sin(angle) * SPAWN_DISTANCE_FROM_BEACON);

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

    private static void spawnTourist(ServerLevel world, ScheduledTouristSpawn scheduledTouristSpawn) {
        TouristBeaconBlockEntity touristBeaconBlockEntity = loadedTouristBeacons.get(scheduledTouristSpawn.beaconPos());
        if (touristBeaconBlockEntity == null) {
            return;
        }

        TouristEntity tourist = ModEntities.TOURIST.get().create(world, EntitySpawnReason.EVENT);
        if (tourist == null) {
            return;
        }

        BlockPos spawnPoint = getSpawnPoint(world, scheduledTouristSpawn.beaconPos(), tourist);
        if (spawnPoint == null) {
            Touristry.LOGGER.warn(
                    "[TourismManager] No safe spawn point found for beacon {} for scheduled time {}",
                    scheduledTouristSpawn.beaconPos(),
                    scheduledTouristSpawn.timeOfDay()
            );
            return;
        }

        Touristry.LOGGER.info(
                "[TourismManager] Spawning tourist at {} for beacon {} for scheduled time {}",
                spawnPoint,
                scheduledTouristSpawn.beaconPos(),
                scheduledTouristSpawn.timeOfDay()
        );

        tourist.snapTo(spawnPoint, world.random.nextFloat() * 360.0F, 0.0F);
        tourist.setBeaconTarget(scheduledTouristSpawn.beaconPos());
        tourist.finalizeSpawn(world, world.getCurrentDifficultyAt(tourist.blockPosition()), EntitySpawnReason.EVENT, null);
        world.addFreshEntity(tourist);
    }
    //endregion
}
