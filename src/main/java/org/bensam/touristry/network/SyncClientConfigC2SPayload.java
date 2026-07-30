package org.bensam.touristry.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.bensam.touristry.Touristry;
import org.jspecify.annotations.NonNull;

public record SyncClientConfigC2SPayload(boolean verboseTooltips) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "sync_client_config");
    public static final Type<SyncClientConfigC2SPayload> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncClientConfigC2SPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, SyncClientConfigC2SPayload::verboseTooltips,
                    SyncClientConfigC2SPayload::new
            );

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() { return TYPE; }
}
