package org.bensam.touristry.config;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.bensam.touristry.Touristry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ModServerConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

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
                .resolve("server-config.json")
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
                Touristry.LOGGER.info("[load] Server config file not found, using defaults");
                config = ModServerConfig.defaults();
                save();
                return true;
            }
            Touristry.LOGGER.warn("[load] Server config file not found, configuration in memory unchanged");
            return false;
        }

        try {
            String raw = Files.readString(loadPath);
            JsonElement json = JsonParser.parseString(raw);
            ModServerConfig loadedConfig = ModServerConfig.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();

            normalizeConfig(loadedConfig);

            // Add migration logic here when we increment past version 1...
            if (loadedConfig.version() < ModServerConfig.CURRENT_VERSION) {
                // Call into a ModServerConfigMigrator.migrate(loaded) method...
                // save();
            }

            config = loadedConfig;
            Touristry.LOGGER.debug("[load] Server config loaded");
        } catch (IOException e) {
            reportLoadError("[load] I/O error reading server config file", e, resetOnError);
            return false;
        } catch (JsonSyntaxException e) {
            reportLoadError("[load] Malformed JSON in server config file", e, resetOnError);
            return false;
        } catch (RuntimeException e) {
            reportLoadError("[load] Failed to decode server config using CODEC", e, resetOnError);
            return false;
        }

        return true;
    }

    private static void reportLoadError(String issue, Exception e, boolean resetOnError) {
        if (resetOnError) {
            Touristry.LOGGER.error(issue + ", using defaults", e);
            config = ModServerConfig.defaults();
            // Implementation note: don't save here. Leave the bad config untouched and give the admin a chance to fix the error.
        } else {
            Touristry.LOGGER.error(issue + ", configuration in memory unchanged", e);
        }
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
            JsonElement json = ModServerConfig.CODEC.encodeStart(JsonOps.INSTANCE, config).getOrThrow();
            String pretty = GSON.toJson(json);
            Files.createDirectories(savePath.getParent());
            Files.writeString(savePath, pretty);
            Touristry.LOGGER.info("[save] Successfully saved server config to disk");
        } catch (IOException e) {
            Touristry.LOGGER.error("[save] I/O error writing server config to disk", e);
        } catch (RuntimeException e) {
            Touristry.LOGGER.error("[save] Failed to encode server config using CODEC", e);
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

        if (loaded.tourismManagerConfig() == null) {
            loaded.setTourismManagerConfig(defaults.tourismManagerConfig());
        }
    }
}
