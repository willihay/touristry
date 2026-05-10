package org.bensam.touristry;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import org.bensam.touristry.tourism.TouristBeaconStats;

public final class ModComponents {
    private ModComponents() {}

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
