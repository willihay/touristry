package org.bensam.touristry.entity;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.bensam.touristry.tourism.ReviewTarget;

import java.util.Locale;

public enum TouristState implements StringRepresentable {
    IDLE(false, ReviewTarget.NONE),
    PLANNING_NEXT_MOVE(false, ReviewTarget.NONE),
    TRAVELING_TO_BEACON(true, ReviewTarget.BEACON),
    CHOOSING_EXPERIENCE(true, ReviewTarget.BEACON),
    TRAVELING_TO_EXPERIENCE(false, ReviewTarget.EXPERIENCE),
    ENTERING_EXPERIENCE(false, ReviewTarget.EXPERIENCE),
    CHOOSING_EXPERIENCE_TARGET(false, ReviewTarget.EXPERIENCE),
    TRAVELING_TO_EXPERIENCE_TARGET(false, ReviewTarget.EXPERIENCE),
    EXPERIENCING_TARGET(false, ReviewTarget.EXPERIENCE),
    ENJOYING_EXPERIENCE(false, ReviewTarget.EXPERIENCE), // TODO: remove
    LEAVING_EXPERIENCE(false, ReviewTarget.EXPERIENCE),
    WANDERING_AT_BEACON(true, ReviewTarget.BEACON),
    WANDERING_AT_EXPERIENCE(false, ReviewTarget.EXPERIENCE),
    WANDERING_WORLD(false, ReviewTarget.NONE),
    SLEEPING(true, ReviewTarget.EXPERIENCE),
    DESPAWNING(false, ReviewTarget.NONE),
    LOST(false, ReviewTarget.NONE),
    FINISHED(false, ReviewTarget.NONE);

    private final boolean requiresBeacon;
    private final ReviewTarget reviewTarget;

    public static final Codec<TouristState> CODEC = StringRepresentable.fromEnum(TouristState::values);

    TouristState(boolean requiresBeacon, ReviewTarget reviewTarget) {
        this.requiresBeacon = requiresBeacon;
        this.reviewTarget = reviewTarget;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public boolean requiresBeacon() {
        return this.requiresBeacon;
    }

    public ReviewTarget reviewTarget() {
        return this.reviewTarget;
    }
}
