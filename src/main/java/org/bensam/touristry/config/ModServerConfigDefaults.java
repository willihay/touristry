package org.bensam.touristry.config;

public final class ModServerConfigDefaults {
    private ModServerConfigDefaults() {}

    public static ModServerConfig create() {
        return new ModServerConfig(
                ModServerConfig.CURRENT_VERSION
        );
    }
}
