package org.bensam.touristry.tourism;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record TouristBeaconStats(int successfulVisits, int failedVisits, double reputation) {
    public static final TouristBeaconStats EMPTY = new TouristBeaconStats(0, 0, 0d);

    public static final Codec<TouristBeaconStats> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("successful_visits", 0).forGetter(TouristBeaconStats::successfulVisits),
            Codec.INT.optionalFieldOf("failed_visits", 0).forGetter(TouristBeaconStats::failedVisits),
            Codec.DOUBLE.optionalFieldOf("reputation", 0d).forGetter(TouristBeaconStats::reputation)
    ).apply(instance, TouristBeaconStats::new));
}
