package org.bensam.touristry.entity;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.bensam.touristry.tourism.TouristLocation;

import java.util.Locale;

public enum TouristState implements StringRepresentable {
    IDLE(false, TouristLocation.WORLD, TouristLocation.WORLD),
    PLANNING_NEXT_MOVE(false, TouristLocation.WORLD, TouristLocation.WORLD),
    TRAVELING_TO_BEACON(true, TouristLocation.WORLD, TouristLocation.BEACON),
    CHOOSING_EXPERIENCE(true, TouristLocation.BEACON, TouristLocation.BEACON),
    TRAVELING_TO_EXPERIENCE(false, TouristLocation.BEACON, TouristLocation.EXPERIENCE),
    ENTERING_EXPERIENCE(false, TouristLocation.EXPERIENCE, TouristLocation.EXPERIENCE),
    CHOOSING_EXPERIENCE_ACTIVITY(false, TouristLocation.EXPERIENCE, TouristLocation.EXPERIENCE),
    TRAVELING_TO_EXPERIENCE_TARGET(false, TouristLocation.EXPERIENCE, TouristLocation.EXPERIENCE),
    POSITIONING_AT_TARGET(false, TouristLocation.EXPERIENCE, TouristLocation.EXPERIENCE),
    EXPERIENCING_TARGET(false, TouristLocation.EXPERIENCE, TouristLocation.EXPERIENCE),
    LEAVING_EXPERIENCE(false, TouristLocation.EXPERIENCE, TouristLocation.EXPERIENCE),
    WANDERING_AT_BEACON(true, TouristLocation.BEACON, TouristLocation.BEACON),
    WANDERING_AT_EXPERIENCE(false, TouristLocation.EXPERIENCE, TouristLocation.EXPERIENCE),
    WANDERING_WORLD(false, TouristLocation.WORLD, TouristLocation.WORLD),
    SLEEPING(true, TouristLocation.EXPERIENCE, TouristLocation.EXPERIENCE),
    DESPAWNING(false, TouristLocation.WORLD, TouristLocation.WORLD),
    LOST(false, TouristLocation.WORLD, TouristLocation.WORLD),
    FINISHED(false, TouristLocation.WORLD, TouristLocation.WORLD);

    private final boolean requiresBeacon;
    private final TouristLocation touristLocation;
    private final TouristLocation reviewTarget;

    public static final Codec<TouristState> CODEC = StringRepresentable.fromEnum(TouristState::values);

    TouristState(boolean requiresBeacon, TouristLocation touristLocation, TouristLocation reviewTarget) {
        this.requiresBeacon = requiresBeacon;
        this.touristLocation = touristLocation;
        this.reviewTarget = reviewTarget;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public boolean isAtBeacon() {
        return this.touristLocation == TouristLocation.BEACON;
    }

    public boolean isAtExperience() {
        return this.touristLocation == TouristLocation.EXPERIENCE;
    }

    public boolean isAtTouristLocation() {
        return this.touristLocation != TouristLocation.WORLD;
    }

    public boolean isTraveling() {
        return this == TRAVELING_TO_BEACON || this == TRAVELING_TO_EXPERIENCE || this == TRAVELING_TO_EXPERIENCE_TARGET;
    }

    public boolean isWandering() {
        return this == WANDERING_WORLD || this == WANDERING_AT_BEACON || this == WANDERING_AT_EXPERIENCE;
    }

    public boolean requiresBeacon() {
        return this.requiresBeacon;
    }

    public TouristLocation reviewTarget() {
        return this.reviewTarget;
    }

    public TouristLocation touristLocation() {
        return this.touristLocation;
    }
}
