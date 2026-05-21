package org.bensam.touristry.config;

import com.mojang.serialization.Codec;
import net.minecraft.util.Mth;

public enum Verbosity {
    ERRORS,
    GAMEPLAY_WARNINGS,
    MAJOR_EVENTS,
    LEVEL_2_DIAGNOSTICS,
    LEVEL_1_DIAGNOSTICS;

    public static final Codec<Verbosity> CODEC = Codec.INT.xmap(Verbosity::fromOrdinal, Verbosity::ordinal);

    private static Verbosity fromOrdinal(int i) {
        var values = Verbosity.values();
        int clampedIndex = Mth.clamp(i, 0, values.length - 1);
        return values[clampedIndex];
    }
}
