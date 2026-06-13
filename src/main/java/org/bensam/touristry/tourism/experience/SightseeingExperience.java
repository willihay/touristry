package org.bensam.touristry.tourism.experience;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;

import java.util.UUID;

public record SightseeingExperience(UUID beaconUUID, BlockPos blockPos) {
    public static final Codec<SightseeingExperience> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("beacon_UUID").forGetter(SightseeingExperience::beaconUUID),
            BlockPos.CODEC.fieldOf("block_pos").forGetter(SightseeingExperience::blockPos)
    ).apply(instance, SightseeingExperience::new));

    public BlockPos getTargetPos() {
        return this.blockPos;
    }
}
