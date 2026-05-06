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
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Function;

public class ModBlocks {
    private ModBlocks() {}

//    private static BlockWandEnchantingTable wandEnchantingTableInternal;
//    public static final Supplier<BlockWandEnchantingTable> WAND_ENCHANTING_TABLE = () -> wandEnchantingTableInternal;

    public static void initialize() {
        // Register mod blocks.
//        wandEnchantingTableInternal = register(
//                "wand_enchanting_table",
//                BlockWandEnchantingTable::new,
//                BlockBehaviour.Properties.of()
//                        .mapColor(MapColor.COLOR_CYAN)
//                        .instrument(NoteBlockInstrument.BASEDRUM)
//                        .requiresCorrectToolForDrops()
//                        .lightLevel(blockState -> blockState.getValue(BlockWandEnchantingTable.HAS_LAPIS) ? 9 : 0)
//                        .sound(SoundType.STONE)
//                        .strength(5.0f, 1200.0f)
//        );
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
