package org.bensam.touristry.client.config;

public class ModClientConfig {
    public static final int CURRENT_VERSION = 1;

    public int version = CURRENT_VERSION;
    public boolean verboseTooltips = true;

    public ModClientConfig() {}

    public ModClientConfig(int version, boolean verboseTooltips) {
        this.version = version;
        this.verboseTooltips = verboseTooltips;
    }

    public static ModClientConfig defaults() {
        return new ModClientConfig(
                CURRENT_VERSION,
                true
        );
    }

    public int version() {
        return this.version;
    }

    public boolean verboseTooltips() {
        return this.verboseTooltips;
    }
}
