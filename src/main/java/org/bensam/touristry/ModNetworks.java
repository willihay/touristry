package org.bensam.touristry;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import org.bensam.touristry.network.SyncClientConfigC2SPayload;
import org.bensam.touristry.network.SyncServerConfigS2CPayload;

public class ModNetworks {
    private ModNetworks() {}

    public static void initialize() {
        // Register packets.
        PayloadTypeRegistry.playC2S().register(SyncClientConfigC2SPayload.TYPE, SyncClientConfigC2SPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SyncServerConfigS2CPayload.TYPE, SyncServerConfigS2CPayload.CODEC);
    }
}
