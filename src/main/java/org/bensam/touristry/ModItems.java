package org.bensam.touristry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import org.bensam.touristry.config.ModServerConfig;

import java.util.function.Function;

public final class ModItems {
    private ModItems() {}

    //private static ItemArcaneWand arcaneWandInternal;

    //public static final Supplier<ItemArcaneWand> ARCANE_WAND = () -> arcaneWandInternal;

//    private static final WandDefinition ARCANE_WAND_DEFINITION =
//            new WandDefinition(
//                    "item." + ArcaneRelics.MOD_ID + ".arcane_wand.info",
//                    2);

    public static void initialize() {
        ModServerConfig defaults = ModServerConfig.defaults();

        // Register mod items.
//        arcaneWandInternal = register(
//                "arcane_wand",
//                props -> new ItemArcaneWand(props, ARCANE_WAND_DEFINITION),
//                ARCANE_WAND_DEFINITION.createProperties(ItemArcaneWand.INITIAL_CHARGES, false)
//        );

//        windWandInternal = register(
//                "wind_wand",
//                props -> new ItemWindWand(props, WIND_WAND_DEFINITION),
//                WIND_WAND_DEFINITION.createProperties(defaults.windWand().balance().initialCharges(), true)
//        );
    }

    public static <T extends Item> T register(
            String name,
            Function<Item.Properties, T> itemFactory,
            Item.Properties settings
    ) {
        // Create the item key.
        ResourceKey<Item> itemKey = ResourceKey.create(
                Registries.ITEM,
                Identifier.fromNamespaceAndPath(Touristry.MOD_ID, name)
        );

        // Create the item instance.
        T item = itemFactory.apply(settings.setId(itemKey));

        // Register the item.
        T registered = Registry.register(BuiltInRegistries.ITEM, itemKey, item);

        return registered;
    }
}
