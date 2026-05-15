package org.bensam.touristry.tourism.experience;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

public record SightseeingExperience(boolean openForBusiness, BlockPos blockPos) {
    public static final Codec<SightseeingExperience> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("open_for_business").forGetter(SightseeingExperience::openForBusiness),
            BlockPos.CODEC.fieldOf("block_pos").forGetter(SightseeingExperience::blockPos)
    ).apply(instance, SightseeingExperience::new));
}
