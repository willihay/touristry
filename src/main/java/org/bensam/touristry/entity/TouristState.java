package org.bensam.touristry.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

public enum TouristState {
    IDLE,
    TRAVELLING,
    AT_BEACON,
    LOST,
    FINISHED;

    public static final Codec<TouristState> CODEC = Codec.STRING.comapFlatMap(
            stateName -> {
                try {
                    return DataResult.success(TouristState.valueOf(stateName));
                } catch (IllegalArgumentException e) {
                    return DataResult.error(() -> "Unknown tourist state: " + stateName);
                }
            },
            TouristState::name
    );
}
