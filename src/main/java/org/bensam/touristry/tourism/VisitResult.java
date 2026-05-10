package org.bensam.touristry.tourism;

public enum VisitResult {
    GOOD(1.0),
    GREAT(2.0),
    LOST(-3.0),
    CLOSED(-4.0),
    HURT_EN_ROUTE(-6.0),
    HURT_ON_PREMISES(-8.0),
    KILLED_EN_ROUTE(-9.0),
    KILLED_ON_PREMISES(-15.0);

    private final double baseReputationDelta;

    VisitResult(double baseReputationDelta) {
        this.baseReputationDelta = baseReputationDelta;
    }

    public double baseReputationDelta() {
        return this.baseReputationDelta;
    }
}
