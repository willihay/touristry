package org.bensam.touristry.entity;

public enum MoodResult {
    GREAT(2.0),
    GOOD(1.0),
    NEUTRAL(0.0),
    UNFAVORABLE(-1.0),
    POOR(-2.0);

    private final double moodDelta;

    MoodResult(double moodDelta) {
        this.moodDelta = moodDelta;
    }

    public double moodDelta() {
        return this.moodDelta;
    }
}
