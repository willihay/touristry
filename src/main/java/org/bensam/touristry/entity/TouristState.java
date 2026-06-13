package org.bensam.touristry.entity;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum TouristState implements StringRepresentable {
    IDLE(false),
    PLANNING_NEXT_MOVE(false),
    TRAVELLING_TO_BEACON(true),
    CHOOSING_EXPERIENCE(true),
    TRAVELLING_TO_EXPERIENCE(false),
    ENJOYING_EXPERIENCE(false),
    WANDERING_AT_BEACON(true),
    WANDERING_WORLD(false),
    SLEEPING(true),
    DESPAWNING(false),
    LOST(false),
    FINISHED(false);

    private final boolean requiresBeacon;

    public static final Codec<TouristState> CODEC = StringRepresentable.fromEnum(TouristState::values);

    TouristState(boolean requiresBeacon) {
        this.requiresBeacon = requiresBeacon;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public boolean requiresBeacon() {
        return this.requiresBeacon;
    }
}
