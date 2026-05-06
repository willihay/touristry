package org.bensam.touristry.client.config;

import org.bensam.touristry.config.ModServerConfig;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class SyncedServerConfig {
    private static @Nullable ModServerConfig current;

    private SyncedServerConfig() {}

    // returns synced server config, or defaults if no server config packets have been received yet
    public static @NonNull ModServerConfig get() {
        return current != null ? current : ModServerConfig.defaults();
    }

    // returns ONLY config that's synced from the server, never default values
    public static @Nullable ModServerConfig getNullable() {
        return current;
    }

    public static void set(ModServerConfig config) {
        current = config;
    }

    public static void clear() {
        current = null;
    }
}
