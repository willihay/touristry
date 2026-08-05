package org.bensam.touristry.tourism.experience;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public record TargetView(
        BlockPos pos,
        @Nullable UUID entityUUID,
        ItemStack itemStack,
        boolean isWideChest,
        String displayName
) {
    public static final StreamCodec<RegistryFriendlyByteBuf, TargetView> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, TargetView::pos,
            ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC), target -> Optional.ofNullable(target.entityUUID()),
            ItemStack.STREAM_CODEC, TargetView::itemStack,
            ByteBufCodecs.BOOL, TargetView::isWideChest,
            ByteBufCodecs.STRING_UTF8, TargetView::displayName,
            (pos, entityUUID, itemStack, isWideChest, displayName) ->
                    new TargetView(pos, entityUUID.orElse(null), itemStack, isWideChest, displayName)
    );
}
