package org.bensam.touristry.entity;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;

import java.util.Locale;

public enum TouristItemInterest implements StringRepresentable {
    ARMOR(0.1F),
    BOOKS(0.2F),
    ENCHANTED_ITEMS(0.2F),
    EPIC_ITEMS(0.025F),
    FIREWORKS(0.05F),
    FLORA(0.1F),
    FOOD(0.25F),
    MUSIC(0.2F),
    POTIONS(0.1F),
    RARE_ITEMS(0.05F),
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
        // Use assignment to make compiler catch forgotten updates when new TouristItemInterest enums are added.
        boolean match = switch (this) {
            case ARMOR -> !itemStack.is(ItemTags.WOOL_CARPETS) &&
                    (itemStack.has(DataComponents.TRIM) || itemStack.has(DataComponents.EQUIPPABLE));

            case BOOKS -> itemStack.is(Items.ENCHANTED_BOOK) ||
                    itemStack.is(Items.WRITABLE_BOOK) ||
                    itemStack.is(Items.WRITTEN_BOOK);

            case ENCHANTED_ITEMS -> itemStack.isEnchanted();

            case EPIC_ITEMS -> itemStack.getOrDefault(DataComponents.RARITY, Rarity.COMMON) == Rarity.EPIC;

            case FIREWORKS -> itemStack.has(DataComponents.FIREWORKS) ||
                    itemStack.is(Items.FIREWORK_ROCKET) ||
                    itemStack.is(Items.FIREWORK_STAR) ||
                    itemStack.is(Items.GUNPOWDER) ||
                    itemStack.is(Items.FIRE_CHARGE);

            case FLORA -> itemStack.is(ItemTags.FLOWERS) ||
                    itemStack.is(ItemTags.SMALL_FLOWERS) ||
                    itemStack.is(ItemTags.SAPLINGS) ||
                    itemStack.is(ItemTags.VILLAGER_PLANTABLE_SEEDS);

            case FOOD -> itemStack.has(DataComponents.FOOD);

            case MUSIC -> itemStack.has(DataComponents.JUKEBOX_PLAYABLE) || itemStack.has(DataComponents.INSTRUMENT);

            case POTIONS -> itemStack.is(Items.POTION);

            case RARE_ITEMS -> itemStack.has(DataComponents.RARITY) && itemStack.get(DataComponents.RARITY) != Rarity.COMMON;

            case SPAWN_EGGS -> itemStack.has(DataComponents.ENTITY_DATA);

            case TOOLS -> itemStack.has(DataComponents.TOOL);

            case WEAPONS -> itemStack.has(DataComponents.WEAPON);
        };

        return match;
    }

    float probability() {
        return this.probability;
    }
}
