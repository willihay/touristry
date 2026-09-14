package org.bensam.touristry.tourism.experience;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public enum ExperienceScreenAction {
    SET_OPEN_STATUS,
    REQUEST_TARGETS,
    MOVE_TARGET,
    REMOVE_TARGET,
    REMOVE_ALL_TARGETS,
    SET_ORDERED_TARGETS,
    REQUEST_ITEM_PRICES,
    IMPORT_ITEMS_FROM_TARGETS,
    RESET_DEFAULT_COST,
    SELECT_ITEM_PRICE,
    ADD_TO_FOR_SALE_QTY,
    ADD_TO_COST_QTY,
    ACCEPT_ITEM_PRICE,
    CLEAR_ITEM_PRICE,
    REMOVE_ITEM_PRICE,
    REMOVE_DEFAULT_ITEM_PRICES,
    REMOVE_ALL_ITEM_PRICES;

    public static final StreamCodec<RegistryFriendlyByteBuf, ExperienceScreenAction> STREAM_CODEC =
            adapt(ByteBufCodecs.VAR_INT.map(
                    ordinal -> ExperienceScreenAction.values()[ordinal],
                    Enum::ordinal
                    )
            );

    private static <T> StreamCodec<RegistryFriendlyByteBuf, T> adapt(StreamCodec<ByteBuf, T> streamCodec) {
        return StreamCodec.of(
                streamCodec::encode,
                streamCodec::decode
        );
    }
}
