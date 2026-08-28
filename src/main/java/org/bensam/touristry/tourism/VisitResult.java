package org.bensam.touristry.tourism;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum VisitResult implements StringRepresentable {
    ARRIVED(0.0, 1.0),
    GOOD(1.0, 1.0),
    GREAT(2.0, 2.0),
    UNFAVORABLE(-1.5, -1.0),
    FAILED_SPAWN(-1.0, 0.0),
    LOST(-1.5, -1.0),
    CLOSED_EARLY(-1.5, -1.0),
    UNAFFORDABLE(-0.25, -0.25),
    PAYMENT_FAILED(-0.5, -0.5),
    HURT_EN_ROUTE(-2.0, -2.0),
    HURT_ON_PREMISES(-4.0, -2.0),
    KILLED_EN_ROUTE(-3.0, 0.0),
    KILLED_ON_PREMISES(-9.0, 0.0);

    public static final Codec<VisitResult> CODEC = StringRepresentable.fromEnum(VisitResult::values);

    private final double baseReputationDelta;
    private final double moodDelta;

    VisitResult(double baseReputationDelta, double moodDelta) {
        this.baseReputationDelta = baseReputationDelta;
        this.moodDelta = moodDelta;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public double baseReputationDelta() {
        return this.baseReputationDelta;
    }

    public double moodDelta() {
        return this.moodDelta;
    }
}
