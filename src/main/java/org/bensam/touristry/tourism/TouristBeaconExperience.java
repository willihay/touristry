package org.bensam.touristry.tourism;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.bensam.touristry.tourism.experience.SightseeingExperience;

import java.util.ArrayList;
import java.util.List;

public record TouristBeaconExperience(boolean beaconOpenForBusiness) {

    public static final TouristBeaconExperience EMPTY = new TouristBeaconExperience(false);

    public static final Codec<TouristBeaconExperience> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("beacon_open_for_business", false).forGetter(TouristBeaconExperience::beaconOpenForBusiness)
    ).apply(instance, TouristBeaconExperience::new));
}
