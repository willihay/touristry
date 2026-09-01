package org.bensam.touristry.entity;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import org.bensam.touristry.tourism.TouristEconomy;

import java.util.Locale;

public enum TouristItemInterest implements StringRepresentable {
    GENERAL(0.0F),
    ARMOR(0.1F),
    BOOKS(0.2F),
    ENCHANTED_ITEMS(0.2F),
    EPIC_ITEMS(0.025F),
    FIREWORKS(0.05F),
    FLORA(0.1F),
    FOOD(0.25F),
    MUSIC(0.2F),
    PAINTINGS(0.2F),
    POTIONS(0.1F),
    RARE_ITEMS(0.05F),
    REDSTONE_BLOCKS(0.1F),
    SPAWN_EGGS(0.05F),
    TOOLS(0.1F),
    WEAPONS(0.1F);

    private static final float PROBABILITY_INTEREST_IN_ANY_ITEM = 0.1F;
    public static final Codec<TouristItemInterest> CODEC = StringRepresentable.fromEnum(TouristItemInterest::values);

    private final float probability;

    TouristItemInterest(float probability) {
        this.probability = probability;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
    
    boolean isAMatch(ItemStack itemStack, Level level) {
        // Use assignment to make compiler catch forgotten updates when new TouristItemInterest enums are added.
        boolean match = switch (this) {
            case GENERAL -> level.getRandom().nextFloat() < PROBABILITY_INTEREST_IN_ANY_ITEM;

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

            case FOOD -> itemStack.has(DataComponents.FOOD) || TouristEconomy.isInCreativeTab(level, itemStack, CreativeModeTabs.FOOD_AND_DRINKS);

            case MUSIC -> itemStack.has(DataComponents.JUKEBOX_PLAYABLE) || itemStack.has(DataComponents.INSTRUMENT);

            case PAINTINGS -> itemStack.is(Items.PAINTING);

            case POTIONS -> itemStack.is(Items.POTION);

            case RARE_ITEMS -> itemStack.has(DataComponents.RARITY) && itemStack.get(DataComponents.RARITY) != Rarity.COMMON;

            case REDSTONE_BLOCKS -> TouristEconomy.isInCreativeTab(level, itemStack, CreativeModeTabs.REDSTONE_BLOCKS);

            case SPAWN_EGGS -> itemStack.has(DataComponents.ENTITY_DATA) || TouristEconomy.isInCreativeTab(level, itemStack, CreativeModeTabs.SPAWN_EGGS);

            case TOOLS -> itemStack.has(DataComponents.TOOL) || TouristEconomy.isInCreativeTab(level, itemStack, CreativeModeTabs.TOOLS_AND_UTILITIES);

            case WEAPONS -> itemStack.has(DataComponents.WEAPON);
        };

        return match;
    }

    float probabilityOfInterest() {
        return this.probability;
    }
}
