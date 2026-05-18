package org.bensam.touristry.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

public enum TouristState {
    IDLE,
    PLANNING_NEXT_MOVE,
    TRAVELLING_TO_BEACON,
    CHOOSING_EXPERIENCE,
    ENJOYING_EXPERIENCE,
    WANDERING_AT_BEACON,
    WANDERING_WORLD,
    SLEEPING,
    DESPAWNING,
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
