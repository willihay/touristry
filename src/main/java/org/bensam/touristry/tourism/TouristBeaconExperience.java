package org.bensam.touristry.tourism;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.bensam.touristry.tourism.experience.SightseeingExperience;

import java.util.ArrayList;
import java.util.List;

public record TouristBeaconExperience(boolean beaconOpenForBusiness,
                                      int experienceSlots,
                                      List<SightseeingExperience> experiences) {
    public static final int BASE_EXPERIENCE_SLOTS = 2;

    public static final TouristBeaconExperience EMPTY = new TouristBeaconExperience(
            false,
            BASE_EXPERIENCE_SLOTS,
            List.of());

    public static final Codec<TouristBeaconExperience> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("beacon_open_for_business", false).forGetter(TouristBeaconExperience::beaconOpenForBusiness),
            Codec.INT.optionalFieldOf("experience_slots", BASE_EXPERIENCE_SLOTS).forGetter(TouristBeaconExperience::experienceSlots),
            SightseeingExperience.CODEC.listOf().optionalFieldOf("experiences", List.of()).forGetter(TouristBeaconExperience::experiences)
    ).apply(instance, TouristBeaconExperience::new));
}
