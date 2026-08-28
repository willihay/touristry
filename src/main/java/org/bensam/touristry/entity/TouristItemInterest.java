package org.bensam.touristry.entity;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

public enum TouristItemInterest implements StringRepresentable {
    ARMOR(0.1F),
    BOOKS(0.2F),
    ENCHANTED(0.2F),
    FIREWORKS(0.05F),
    FLORA(0.1F),
    FOOD(0.25F),
    MUSIC(0.2F),
    POTIONS(0.1F),
    RARE(0.05F),
    SPAWN_EGGS(0.05F),
    TOOLS(0.1F),
    WEAPONS(0.1F);

    public static final Codec<TouristItemInterest> CODEC = StringRepresentable.fromEnum(TouristItemInterest::values);

    private final float probability;

    TouristItemInterest(float probability) {
        this.probability = probability;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
    
    boolean isAMatch(ItemStack itemStack) {
        switch (this) {
            case ARMOR: {
                return itemStack.has(DataComponents.TRIM);
            }
        }

        return false;
    }

    float probability() {
        return this.probability;
    }
}
