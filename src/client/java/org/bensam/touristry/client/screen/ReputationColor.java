package org.bensam.touristry.client.screen;

public enum ReputationColor {
    POSITIVE(0xFF80FF20), // light green
    NEUTRAL(0xFF404040), // gray
    NEGATIVE(0xFFFF6060); // light red

    private final int color;

    ReputationColor(int color) {
        this.color = color;
    }

    public static int getColor(double reputation) {
        if (reputation < 0.0) {
            return NEGATIVE.value();
        } else if (reputation > 0.0) {
            return POSITIVE.value();
        } else {
            return NEUTRAL.color;
        }
    }

    public int value() {
        return this.color;
    }
}
