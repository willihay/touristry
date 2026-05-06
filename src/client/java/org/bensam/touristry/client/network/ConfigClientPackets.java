package org.bensam.touristry.client.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import org.bensam.touristry.client.config.ModClientConfigManager;
import org.bensam.touristry.client.config.SyncedServerConfig;
import org.bensam.touristry.network.SyncClientConfigC2SPayload;
import org.bensam.touristry.network.SyncServerConfigS2CPayload;

public final class ConfigClientPackets {
    private ConfigClientPackets() {}

    public static void registerClientReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(SyncServerConfigS2CPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> SyncedServerConfig.set(payload.config()));
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> sendClientPreferences());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> SyncedServerConfig.clear());
    }

    public static void sendClientPreferences() {
        if (!ClientPlayNetworking.canSend(SyncClientConfigC2SPayload.TYPE)) {
            return;
        }

        // Make selected client config available to server-side logic.
        ClientPlayNetworking.send(new SyncClientConfigC2SPayload(
                ModClientConfigManager.getConfig().verboseTooltips()
        ));
    }
}
