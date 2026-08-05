package org.bensam.touristry;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import org.bensam.touristry.network.*;

public class ModNetworks {
    private ModNetworks() {}

    public static void initialize() {
        // Register packets.
        PayloadTypeRegistry.playC2S().register(SyncClientConfigC2SPayload.TYPE, SyncClientConfigC2SPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SyncServerConfigS2CPayload.TYPE, SyncServerConfigS2CPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(ExperienceScreenActionC2SPayload.TYPE, ExperienceScreenActionC2SPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SyncTargetViewS2CPayload.TYPE, SyncTargetViewS2CPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SyncTargetOverlayViewS2CPayload.TYPE, SyncTargetOverlayViewS2CPayload.STREAM_CODEC);
    }
}
