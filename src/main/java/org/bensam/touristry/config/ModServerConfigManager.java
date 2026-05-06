package org.bensam.touristry.config;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.api.SyntaxError;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.bensam.touristry.Touristry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ModServerConfigManager {
    private static final Jankson JANKSON = Jankson.builder().build();

    private static ModServerConfig config = ModServerConfig.defaults();
    private static Path configPath;

    private ModServerConfigManager() {}

    public static void initialize(MinecraftServer server) {
        configPath = getConfigPath(server);
        load(true);
    }

    public static ModServerConfig getConfig() {
        return config;
    }

    private static Path getConfigPath(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT)
                .resolve("data")
                .resolve(Touristry.MOD_ID)
                .resolve("server-config.json5")
                .normalize();
    }

    public static void setConfig(ModServerConfig newConfig) {
        config = newConfig;
        save();
    }

    public static boolean load(boolean resetOnError) {
        Path loadPath = requireConfigPath();
        Touristry.LOGGER.debug("[load] Server config path is: {}", loadPath);

        if (!Files.exists(loadPath)) {
            if (resetOnError) {
                Touristry.LOGGER.info("[load] Server config file not found, setting to defaults");
                config = ModServerConfig.defaults();
                save();
                return true;
            }
            Touristry.LOGGER.warn("[load] Server config file not found, configuration in memory unchanged");
            return false;
        }

        try {
            String raw = Files.readString(loadPath);
            JsonObject json = JANKSON.load(raw);
            ModServerConfig loaded = JANKSON.fromJson(json, ModServerConfig.class);
            if (loaded == null) {
                throw new IOException("[load] Jankson returned null while deserializing ModServerConfig");
            }

            normalizeConfig(loaded);

            // Add migration logic here when we increment past version 1...
            if (loaded.version() < ModServerConfig.CURRENT_VERSION) {
                // Call into a ModServerConfigMigrator.migrate(loaded) method...
                // save();
            }

            config = loaded;
            Touristry.LOGGER.debug("[load] Server config loaded");
        } catch (IOException | SyntaxError e) {
            if (resetOnError) {
                Touristry.LOGGER.error("[load] Failed to load server config from disk, using defaults", e);
                config = ModServerConfig.defaults();
                // Implementation note: don't save here. Leave the bad config untouched and give the admin a chance to fix the error.
            } else {
                Touristry.LOGGER.error("[load] Failed to load server config from disk, configuration in memory unchanged", e);
            }
            return false;
        }

        return true;
    }

    public static boolean reload(boolean resetOnError) {
        return load(resetOnError);
    }

    public static void reset() {
        config = ModServerConfig.defaults();
        save();
    }

    public static void save() {
        Path savePath = requireConfigPath();
        Touristry.LOGGER.debug("[save] Server config path is: {}", savePath);

        try {
            JsonObject json = (JsonObject) JANKSON.toJson(config);
            String pretty = json.toJson(true, true);

            Files.createDirectories(savePath.getParent());
            Files.writeString(savePath, pretty);
            Touristry.LOGGER.info("[save] Successfully saved server config to disk");
        } catch (IOException e) {
            Touristry.LOGGER.error("[save] Failed to save server config to disk", e);
        }
    }

    private static Path requireConfigPath() {
        if (configPath == null) {
            throw new IllegalStateException("Server config path requested before ModServerConfigManager.initialize(server)");
        }
        return configPath;
    }

    private static void normalizeConfig(ModServerConfig loaded) {
        ModServerConfig defaults = ModServerConfig.defaults();

//        if (loaded.wandEnchantingTable == null) {
//            loaded.wandEnchantingTable = defaults.wandEnchantingTable();
//        }
//
//        loaded.fangWand = normalizeFangWandConfig(loaded.fangWand, defaults.fangWand());
    }

//    private static FangWandConfig normalizeFangWandConfig(FangWandConfig loaded, FangWandConfig defaults) {
//        if (loaded == null) {
//            return defaults;
//        }
//        if (loaded.balance == null) {
//            loaded.balance = defaults.balance();
//        }
//        return loaded;
//    }
}
