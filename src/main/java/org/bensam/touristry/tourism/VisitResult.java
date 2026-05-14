package org.bensam.touristry.tourism;

public enum VisitResult {
    GOOD(1.0),
    GREAT(2.0),
    FAILED_SPAWN(-1.0),
    LOST(-1.0),
    CLOSED(-2.0),
    HURT_EN_ROUTE(-3.0),
    HURT_ON_PREMISES(-6.0),
    KILLED_EN_ROUTE(-3.0),
    KILLED_ON_PREMISES(-9.0);

    private final double baseReputationDelta;

    VisitResult(double baseReputationDelta) {
        this.baseReputationDelta = baseReputationDelta;
    }

    public double baseReputationDelta() {
        return this.baseReputationDelta;
    }
}
