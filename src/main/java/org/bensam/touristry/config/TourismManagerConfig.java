package org.bensam.touristry.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class TourismManagerConfig {
    private Verbosity verbosityLevel;
    private int maxSpawnsPerBeaconPerDay;
    private int earliestSpawnTimeTicks;
    private int latestSpawnTimeTicks;
    private int minSpawnDistanceToBeacon;
    private int maxSpawnDistanceToBeacon;
    private int maxExperienceDistanceToBeacon;

    public static final Codec<TourismManagerConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Verbosity.CODEC.fieldOf("verbosityLevel").forGetter(TourismManagerConfig::getVerbosityLevel),
            Codec.INT.fieldOf("maxSpawnsPerBeaconPerDay").forGetter(TourismManagerConfig::getMaxSpawnsPerBeaconPerDay),
            Codec.INT.fieldOf("earliestSpawnTimeTicks").forGetter(TourismManagerConfig::getEarliestSpawnTimeTicks),
            Codec.INT.fieldOf("latestSpawnTimeTicks").forGetter(TourismManagerConfig::getLatestSpawnTimeTicks),
            Codec.INT.fieldOf("minSpawnDistanceToBeacon").forGetter(TourismManagerConfig::getMinSpawnDistanceToBeacon),
            Codec.INT.fieldOf("maxSpawnDistanceToBeacon").forGetter(TourismManagerConfig::getMaxSpawnDistanceToBeacon),
            Codec.INT.fieldOf("maxExperienceDistanceToBeacon").forGetter(TourismManagerConfig::getMaxExperienceDistanceToBeacon)
    ).apply(instance, TourismManagerConfig::new));

    public TourismManagerConfig() {}

    public TourismManagerConfig(
            Verbosity verbosityLevel,
            int maxSpawnsPerBeaconPerDay,
            int earliestSpawnTimeTicks,
            int latestSpawnTimeTicks,
            int minSpawnDistanceToBeacon,
            int maxSpawnDistanceToBeacon,
            int maxExperienceDistanceToBeacon
    ) {
        this.verbosityLevel = verbosityLevel;
        this.maxSpawnsPerBeaconPerDay = maxSpawnsPerBeaconPerDay;
        this.earliestSpawnTimeTicks = earliestSpawnTimeTicks;
        this.latestSpawnTimeTicks = latestSpawnTimeTicks;
        this.minSpawnDistanceToBeacon = minSpawnDistanceToBeacon;
        this.maxSpawnDistanceToBeacon = maxSpawnDistanceToBeacon;
        this.maxExperienceDistanceToBeacon = maxExperienceDistanceToBeacon;
    }

    public Verbosity getVerbosityLevel() {
        return this.verbosityLevel;
    }

    public int getMaxSpawnsPerBeaconPerDay() {
        return this.maxSpawnsPerBeaconPerDay;
    }

    public int getEarliestSpawnTimeTicks() {
        return this.earliestSpawnTimeTicks;
    }

    public int getLatestSpawnTimeTicks() {
        return this.latestSpawnTimeTicks;
    }

    public int getMinSpawnDistanceToBeacon() {
        return this.minSpawnDistanceToBeacon;
    }

    public int getMaxSpawnDistanceToBeacon() {
        return this.maxSpawnDistanceToBeacon;
    }

    public int getMaxExperienceDistanceToBeacon() {
        return this.maxExperienceDistanceToBeacon;
    }
}
