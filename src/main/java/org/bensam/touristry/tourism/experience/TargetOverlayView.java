package org.bensam.touristry.tourism.experience;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public record TargetOverlayView(
        BlockPos pos,
        BlockPos alternateOverlayDisplayPos,
        @Nullable UUID entityUUID,
        int targetNumber
) {
    public static final StreamCodec<RegistryFriendlyByteBuf, TargetOverlayView> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, TargetOverlayView::pos,
            BlockPos.STREAM_CODEC, TargetOverlayView::alternateOverlayDisplayPos,
            ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC), target -> Optional.ofNullable(target.entityUUID()),
            ByteBufCodecs.VAR_INT, TargetOverlayView::targetNumber,
            (pos, alternatePos, entityUUID, targetNumber) ->
                    new TargetOverlayView(pos, alternatePos, entityUUID.orElse(null), targetNumber)
    );
}
