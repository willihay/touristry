package org.bensam.touristry.tourism;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum TouristLocation implements StringRepresentable {
    WORLD,
    BEACON,
    EXPERIENCE;

    public static final Codec<TouristLocation> CODEC = StringRepresentable.fromEnum(TouristLocation::values);

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
