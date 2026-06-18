package org.bensam.touristry;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import org.bensam.touristry.block.SightseeingExperienceBlock;
import org.bensam.touristry.block.TouristBeaconBlock;

import java.util.function.Function;
import java.util.function.Supplier;

public class ModBlocks {
    private ModBlocks() {}

    private static TouristBeaconBlock touristBeacon;
    public static final Supplier<TouristBeaconBlock> TOURIST_BEACON = () -> touristBeacon;

    private static SightseeingExperienceBlock sightseeingExperience;
    public static final Supplier<SightseeingExperienceBlock> SIGHTSEEING_EXPERIENCE = () -> sightseeingExperience;

    public static void initialize() {
        // Register mod blocks.
        touristBeacon = register(
                "tourist_beacon",
                TouristBeaconBlock::new,
                BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_PURPLE)
                        .instrument(NoteBlockInstrument.BASEDRUM)
                        .lightLevel(blockState -> blockState.getValue(TouristBeaconBlock.OPEN_FOR_BUSINESS) ? 12 : 0)
                        .requiresCorrectToolForDrops()
                        .sound(SoundType.STONE)
                        .strength(5.0f, 1200.0f)
        );

        sightseeingExperience = register(
                "sightseeing_experience",
                SightseeingExperienceBlock::new,
                BlockBehaviour.Properties.of()
                        .strength(5.0f, 1200.0f)
        );
    }

    public static <T extends Block> T register(
            String name,
            Function<BlockBehaviour.Properties, T> blockFactory,
            BlockBehaviour.Properties settings
    ) {
        // Create the block key.
        ResourceKey<Block> blockKey = ResourceKey.create(
                Registries.BLOCK,
                Identifier.fromNamespaceAndPath(Touristry.MOD_ID, name)
        );

        // Create the block instance.
        T block = blockFactory.apply(settings.setId(blockKey));

        // Create the block item key.
        ResourceKey<Item> blockItemKey = ResourceKey.create(
                Registries.ITEM,
                Identifier.fromNamespaceAndPath(Touristry.MOD_ID, name)
        );

        // Create the block item instance.
        BlockItem blockItem = new BlockItem(
                block,
                new Item.Properties()
                        .setId(blockItemKey)
                        .useBlockDescriptionPrefix()
                        .component(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
        );

        // Register the block item.
        Registry.register(BuiltInRegistries.ITEM, blockItemKey, blockItem);

        // Register the block.
        return Registry.register(BuiltInRegistries.BLOCK, blockKey, block);
    }
}
