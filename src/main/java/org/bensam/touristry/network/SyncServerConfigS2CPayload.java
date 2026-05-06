package org.bensam.touristry.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.bensam.touristry.Touristry;
import org.bensam.touristry.config.ModServerConfig;
import org.jspecify.annotations.NonNull;

public record SyncServerConfigS2CPayload(ModServerConfig config) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "sync_server_config");
    public static final Type<SyncServerConfigS2CPayload> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncServerConfigS2CPayload> CODEC =
            ByteBufCodecs.fromCodecWithRegistries(ModServerConfig.CODEC)
                    .map(SyncServerConfigS2CPayload::new, SyncServerConfigS2CPayload::config);

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() { return TYPE; }
}
