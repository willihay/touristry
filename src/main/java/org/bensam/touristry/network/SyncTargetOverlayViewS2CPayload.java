package org.bensam.touristry.network;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.bensam.touristry.Touristry;
import org.bensam.touristry.tourism.experience.TargetOverlayView;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.UUID;

public record SyncTargetOverlayViewS2CPayload(
        UUID experienceUUID,
        List<TargetOverlayView> targets
) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "target_overlay_view");
    public static final Type<SyncTargetOverlayViewS2CPayload> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncTargetOverlayViewS2CPayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, SyncTargetOverlayViewS2CPayload::experienceUUID,
            TargetOverlayView.STREAM_CODEC.apply(ByteBufCodecs.list()), SyncTargetOverlayViewS2CPayload::targets,
            SyncTargetOverlayViewS2CPayload::new
    );

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
