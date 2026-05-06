package org.bensam.touristry.client.config;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.api.SyntaxError;
import net.fabricmc.loader.api.FabricLoader;
import org.bensam.touristry.Touristry;
import org.bensam.touristry.client.network.ConfigClientPackets;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ModClientConfigManager {
    private static final Path CONFIG_PATH =
            FabricLoader.getInstance().getConfigDir().resolve(Touristry.MOD_ID + "-client-config.json5").normalize();

    private static final Jankson JANKSON = Jankson.builder().build();

    private static ModClientConfig config = ModClientConfig.defaults();

    private ModClientConfigManager() {}

    public static void initialize() {
        load();
    }

    public static ModClientConfig getConfig() {
        return config;
    }

    public static void setConfig(ModClientConfig newConfig) {
        config = newConfig;
        save();
        ConfigClientPackets.sendClientPreferences();
    }

    public static void load() {
        Touristry.LOGGER.debug("[load] Client config path is: {}", CONFIG_PATH);

        if (!Files.exists(CONFIG_PATH)) {
            Touristry.LOGGER.info("[load] Client config file not found, setting to defaults");
            config = ModClientConfig.defaults();
            save();
            return;
        }

        try {
            String raw = Files.readString(CONFIG_PATH);
            JsonObject json = JANKSON.load(raw);
            ModClientConfig loaded = JANKSON.fromJson(json, ModClientConfig.class);
            if (loaded == null) {
                throw new IOException("[load] Jankson returned null while deserializing ModClientConfig");
            }

            // Add migration logic here when we increment past version 1...
            if (loaded.version() < ModClientConfig.CURRENT_VERSION) {
                // Call into a ModClientConfigMigrator.migrate(loaded) method...
                // save();
            }

            config = loaded;
            Touristry.LOGGER.debug("[load] Client config loaded");
        } catch (IOException | SyntaxError e) {
            Touristry.LOGGER.error("[load] Failed to load client config from disk, using defaults", e);
            config = ModClientConfig.defaults();
            // Implementation note: don't save here. Leave the bad config untouched and give the player a chance to fix the error.
        }
    }

    public static void save() {
        try {
            Touristry.LOGGER.debug("[save] Client config path is: {}", CONFIG_PATH);
            JsonObject json = (JsonObject) JANKSON.toJson(config);
            String pretty = json.toJson(true, true);

            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, pretty);
            Touristry.LOGGER.info("[save] Successfully saved client config to disk");
        } catch (IOException e) {
            Touristry.LOGGER.error("[save] Failed to save client config to disk", e);
        }
    }
}
