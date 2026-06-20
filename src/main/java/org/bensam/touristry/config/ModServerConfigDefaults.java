package org.bensam.touristry.config;

public final class ModServerConfigDefaults {
    private ModServerConfigDefaults() {}

    public static ModServerConfig create() {
        return new ModServerConfig(
                ModServerConfig.CURRENT_VERSION,
                new TourismManagerConfig(
                        Verbosity.LEVEL_2_DIAGNOSTICS,
                        5,
                        2000,
                        9000,
                        35,
                        70,
                        150
                ),
                new TouristEntityConfig(
                        Verbosity.LEVEL_2_DIAGNOSTICS,
                        150
                )
        );
    }
}
