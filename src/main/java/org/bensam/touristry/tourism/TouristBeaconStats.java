package org.bensam.touristry.tourism;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record TouristBeaconStats(
        int successfulVisits,
        int closedEarly,
        int failedSpawns,
        int navFailures,
        int touristsHurt,
        int touristsKilled,
        double reputation) {
    public static final TouristBeaconStats EMPTY = new TouristBeaconStats(0, 0, 0, 0, 0, 0, 0d);

    public static final Codec<TouristBeaconStats> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("successful_visits", 0).forGetter(TouristBeaconStats::successfulVisits),
            Codec.INT.optionalFieldOf("closed_early", 0).forGetter(TouristBeaconStats::closedEarly),
            Codec.INT.optionalFieldOf("failed_spawns", 0).forGetter(TouristBeaconStats::failedSpawns),
            Codec.INT.optionalFieldOf("nav_failures", 0).forGetter(TouristBeaconStats::navFailures),
            Codec.INT.optionalFieldOf("tourists_hurt", 0).forGetter(TouristBeaconStats::touristsHurt),
            Codec.INT.optionalFieldOf("tourists_killed", 0).forGetter(TouristBeaconStats::touristsKilled),
            Codec.DOUBLE.optionalFieldOf("reputation", 0d).forGetter(TouristBeaconStats::reputation)
    ).apply(instance, TouristBeaconStats::new));
}
