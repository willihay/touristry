package org.bensam.touristry.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.bensam.touristry.Touristry;

public record ExperienceTargetRenameC2SPayload(
        int containerId,
        int targetIndex,
        String customName
) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "experience_target_rename");
    public static final Type<ExperienceTargetRenameC2SPayload> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, ExperienceTargetRenameC2SPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ExperienceTargetRenameC2SPayload::containerId,
            ByteBufCodecs.VAR_INT, ExperienceTargetRenameC2SPayload::targetIndex,
            ByteBufCodecs.STRING_UTF8, ExperienceTargetRenameC2SPayload::customName,
            ExperienceTargetRenameC2SPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
