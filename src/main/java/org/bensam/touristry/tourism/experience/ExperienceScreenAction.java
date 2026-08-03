package org.bensam.touristry.tourism.experience;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public enum ExperienceScreenAction {
    REQUEST_TARGETS,
    MOVE_TARGET,
    REMOVE_TARGET,
    REMOVE_ALL_TARGETS,
    SET_ORDERED_TARGETS;

    public static final StreamCodec<RegistryFriendlyByteBuf, ExperienceScreenAction> STREAM_CODEC =
            adapt(ByteBufCodecs.VAR_INT.map(
                    ordinal -> ExperienceScreenAction.values()[ordinal],
                    action -> action.ordinal()
                    )
            );

    private static <T> StreamCodec<RegistryFriendlyByteBuf, T> adapt(StreamCodec<ByteBuf, T> streamCodec) {
        return StreamCodec.of(
                streamCodec::encode,
                streamCodec::decode
        );
    }
}
