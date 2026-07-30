package org.bensam.touristry.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.bensam.touristry.Touristry;
import org.bensam.touristry.tourism.experience.TargetView;
import org.jspecify.annotations.NonNull;

import java.util.List;

public record SyncTargetViewS2CPayload(
        int containerId,
        boolean orderedTargets,
        List<TargetView> targets
) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "target_view");
    public static final Type<SyncTargetViewS2CPayload> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncTargetViewS2CPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SyncTargetViewS2CPayload::containerId,
            ByteBufCodecs.BOOL, SyncTargetViewS2CPayload::orderedTargets,
            TargetView.STREAM_CODEC.apply(ByteBufCodecs.list()), SyncTargetViewS2CPayload::targets,
            SyncTargetViewS2CPayload::new
    );

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
