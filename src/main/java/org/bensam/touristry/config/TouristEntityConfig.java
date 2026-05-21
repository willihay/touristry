package org.bensam.touristry.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class TouristEntityConfig {
    private Verbosity verbosityLevel;
    private int maxTravelDistanceToNextBeacon;

    public static final Codec<TouristEntityConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Verbosity.CODEC.fieldOf("verbosityLevel").forGetter(TouristEntityConfig::getVerbosityLevel),
            Codec.INT.fieldOf("maxTravelDistanceToNextBeacon").forGetter(TouristEntityConfig::getMaxTravelDistanceToNextBeacon)
    ).apply(instance, TouristEntityConfig::new));

    public TouristEntityConfig() {}
    public TouristEntityConfig(
            Verbosity verbosityLevel,
            int maxTravelDistanceToNextBeacon
    ) {
        this.verbosityLevel = verbosityLevel;
        this.maxTravelDistanceToNextBeacon = maxTravelDistanceToNextBeacon;
    }

    public Verbosity getVerbosityLevel() {
        return this.verbosityLevel;
    }

    public int getMaxTravelDistanceToNextBeacon() {
        return this.maxTravelDistanceToNextBeacon;
    }
}
