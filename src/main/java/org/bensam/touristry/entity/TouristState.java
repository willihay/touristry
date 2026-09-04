package org.bensam.touristry.entity;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.bensam.touristry.tourism.TouristLocation;

import java.util.Locale;

// When adding new states, be sure to update the following:
// - TouristMind::getStateForLogging
// - TouristMind::getStateTargetName
public enum TouristState implements StringRepresentable {
    IDLE(false, false, TouristLocation.WORLD, TouristLocation.WORLD),
    TRAVELING_TO_BEACON(true, false, TouristLocation.WORLD, TouristLocation.BEACON),
    WAIT_AT_BEACON(false, false, TouristLocation.BEACON, TouristLocation.BEACON),
    CHOOSING_EXPERIENCE_AT_BEACON(true, false, TouristLocation.BEACON, TouristLocation.BEACON),
    TRAVELING_TO_EXPERIENCE(false, true, TouristLocation.BEACON, TouristLocation.EXPERIENCE),
    WAIT_AT_EXPERIENCE(false, false, TouristLocation.EXPERIENCE, TouristLocation.EXPERIENCE),
    ENTERING_EXPERIENCE(false, true, TouristLocation.EXPERIENCE, TouristLocation.EXPERIENCE),
    CHOOSING_EXPERIENCE_TARGET(false, true, TouristLocation.EXPERIENCE, TouristLocation.EXPERIENCE),
    TRAVELING_TO_EXPERIENCE_TARGET(false, true, TouristLocation.EXPERIENCE, TouristLocation.EXPERIENCE),
    POSITIONING_AT_TARGET(false, true, TouristLocation.EXPERIENCE, TouristLocation.EXPERIENCE),
    EXPERIENCING_TARGET(false, true, TouristLocation.EXPERIENCE, TouristLocation.EXPERIENCE),
    WANDERING_AT_BEACON(true, false, TouristLocation.BEACON, TouristLocation.BEACON),
    WANDERING_AT_EXPERIENCE(false, true, TouristLocation.EXPERIENCE, TouristLocation.EXPERIENCE),
    WANDERING_WORLD(false, false, TouristLocation.WORLD, TouristLocation.WORLD),
    SLEEPING(true, true, TouristLocation.EXPERIENCE, TouristLocation.EXPERIENCE),
    PLANNING_NEXT_MOVE(false, false, TouristLocation.WORLD, TouristLocation.WORLD),
    DESPAWNING(false, false, TouristLocation.WORLD, TouristLocation.WORLD),
    LOST(false, false, TouristLocation.WORLD, TouristLocation.WORLD),
    FINISHED(false, false, TouristLocation.WORLD, TouristLocation.WORLD);

    public static final Codec<TouristState> CODEC = StringRepresentable.fromEnum(TouristState::values);

    private final boolean requiresBeaconPos;
    private final boolean requiresExperiencePos;
    private final TouristLocation touristLocation;
    private final TouristLocation reviewTarget;

    TouristState(boolean requiresBeaconPos, boolean requiresExperiencePos, TouristLocation touristLocation, TouristLocation reviewTarget) {
        this.requiresBeaconPos = requiresBeaconPos;
        this.requiresExperiencePos = requiresExperiencePos;
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

    public boolean isAtTarget() {
        return this == EXPERIENCING_TARGET;
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

    public boolean requiresBeaconPos() {
        return this.requiresBeaconPos;
    }

    public boolean requiresExperiencePos() {
        return this.requiresExperiencePos;
    }

    public TouristLocation reviewTarget() {
        return this.reviewTarget;
    }

    public TouristLocation touristLocation() {
        return this.touristLocation;
    }
}
