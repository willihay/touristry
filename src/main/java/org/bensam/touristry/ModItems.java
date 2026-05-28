package org.bensam.touristry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import org.bensam.touristry.config.ModServerConfig;
import org.bensam.touristry.item.BeaconKeyItem;

import java.util.function.Function;
import java.util.function.Supplier;

public final class ModItems {
    private ModItems() {}

    private static BeaconKeyItem beaconKeyItem;
    public static final Supplier<BeaconKeyItem> BEACON_KEY = () -> beaconKeyItem;

    public static void initialize() {
        ModServerConfig defaults = ModServerConfig.defaults();

        // Register mod items.
        beaconKeyItem = register(
                "beacon_key",
                BeaconKeyItem::new,
                new Item.Properties()
        );
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
