package org.bensam.touristry.tourism.experience;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public record ExperienceTarget(
        BlockPos pos,
        Direction playerFacing,
        @Nullable UUID childExperienceUUID,
        long registeredAtTicks
) {
    public static final Codec<ExperienceTarget> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.fieldOf("pos").forGetter(ExperienceTarget::pos),
            Direction.CODEC.fieldOf("player_facing").forGetter(ExperienceTarget::playerFacing),
            UUIDUtil.CODEC.optionalFieldOf("child_experience_uuid").forGetter(target -> Optional.ofNullable(target.childExperienceUUID())),
            Codec.LONG.fieldOf("registered_at_ticks").forGetter(ExperienceTarget::registeredAtTicks)
    ).apply(instance, (pos, facing, childUUID, time) -> new ExperienceTarget(pos, facing, childUUID.orElse(null), time))
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ExperienceTarget> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            ExperienceTarget::pos,
            Direction.STREAM_CODEC,
            ExperienceTarget::playerFacing,
            ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC),
            target -> Optional.ofNullable(target.childExperienceUUID()),
            ByteBufCodecs.VAR_LONG,
            ExperienceTarget::registeredAtTicks,
            (pos, facing, childUUID, time) ->
                    new ExperienceTarget(pos, facing, childUUID.orElse(null), time)
    );

    public boolean isChildExperience() {
        return childExperienceUUID != null;
    }
}
