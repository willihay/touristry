package org.bensam.touristry;

import net.minecraft.core.Registry;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import org.bensam.touristry.tourism.TouristBeaconExperience;
import org.bensam.touristry.tourism.TouristBeaconStats;

import java.util.UUID;

public final class ModComponents {
    private ModComponents() {}

    public static final DataComponentType<UUID> TOURIST_BEACON_UUID = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "tourist_beacon_uuid"),
            DataComponentType.<UUID>builder()
                    .persistent(UUIDUtil.CODEC)
                    .build()
    );

    public static final DataComponentType<TouristBeaconExperience> TOURIST_BEACON_EXPERIENCE = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "tourist_beacon_experience"),
            DataComponentType.<TouristBeaconExperience>builder()
                    .persistent(TouristBeaconExperience.CODEC)
                    .build()
    );

    public static final DataComponentType<TouristBeaconStats> TOURIST_BEACON_STATS = Registry.register(
        BuiltInRegistries.DATA_COMPONENT_TYPE,
        Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "tourist_beacon_stats"),
        DataComponentType.<TouristBeaconStats>builder()
                .persistent(TouristBeaconStats.CODEC)
                .build()
    );

    public static void initialize() {
        Touristry.LOGGER.debug("Registering components");
    }
}
