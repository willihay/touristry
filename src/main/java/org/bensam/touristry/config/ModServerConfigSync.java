package org.bensam.touristry.config;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import org.bensam.touristry.network.SyncServerConfigS2CPayload;

public final class ModServerConfigSync {
    private ModServerConfigSync() {}

    public static void initialize() {
        ServerPlayConnectionEvents.JOIN.register(
                (handler, sender, server) ->
                        syncToPlayer(handler.player));
    }

    public static void syncToPlayer(ServerPlayer player) {
        ModServerConfig config = ModServerConfigManager.getConfig();
        ServerPlayNetworking.send(player, new SyncServerConfigS2CPayload(config));
    }
}
