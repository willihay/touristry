package org.bensam.touristry.tourism.experience;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public record TargetView(BlockPos pos, ItemStack itemStack, String displayName) {
    public static final StreamCodec<RegistryFriendlyByteBuf, TargetView> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, TargetView::pos,
            ItemStack.STREAM_CODEC, TargetView::itemStack,
            ByteBufCodecs.STRING_UTF8, TargetView::displayName,
            TargetView::new
    );
}
