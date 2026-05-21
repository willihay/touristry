package org.bensam.touristry.entity;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum TouristState implements StringRepresentable {
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

    public static final Codec<TouristState> CODEC = StringRepresentable.fromEnum(TouristState::values);

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
