package org.bensam.touristry.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.bensam.touristry.Touristry;
import org.bensam.touristry.tourism.experience.ExperienceScreenAction;

public record ExperienceScreenActionC2SPayload(
        int containerId,
        ExperienceScreenAction action,
        int primary,
        int secondary
) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "experience_menu_action");
    public static final Type<ExperienceScreenActionC2SPayload> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, ExperienceScreenActionC2SPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ExperienceScreenActionC2SPayload::containerId,
            ExperienceScreenAction.STREAM_CODEC, ExperienceScreenActionC2SPayload::action,
            ByteBufCodecs.VAR_INT, ExperienceScreenActionC2SPayload::primary,
            ByteBufCodecs.VAR_INT, ExperienceScreenActionC2SPayload::secondary,
            ExperienceScreenActionC2SPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
