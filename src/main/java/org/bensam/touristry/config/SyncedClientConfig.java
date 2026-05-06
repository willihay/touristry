package org.bensam.touristry.config;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import org.bensam.touristry.network.SyncClientConfigC2SPayload;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Provides synced client config for any player to server-side logic.
public final class SyncedClientConfig {
    private static final boolean DEFAULT_ENABLED = true;
    private static final Map<UUID, Boolean> PLAYER_VERBOSE_TOOLTIPS = new ConcurrentHashMap<>();

    private SyncedClientConfig() {}

    public static void initialize() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            PLAYER_VERBOSE_TOOLTIPS.put(handler.player.getUUID(), DEFAULT_ENABLED);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            PLAYER_VERBOSE_TOOLTIPS.remove(handler.player.getUUID());
        });

        ServerPlayNetworking.registerGlobalReceiver(SyncClientConfigC2SPayload.TYPE, (payload, context) -> {
            PLAYER_VERBOSE_TOOLTIPS.put(context.player().getUUID(), payload.verboseTooltips());
        });
    }

    public static boolean showVerboseTooltips(ServerPlayer player) {
        return PLAYER_VERBOSE_TOOLTIPS.getOrDefault(player.getUUID(), DEFAULT_ENABLED);
    }
}
