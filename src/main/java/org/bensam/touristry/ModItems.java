package org.bensam.touristry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import org.bensam.touristry.config.ModServerConfig;
import org.bensam.touristry.item.ExperienceTargetKeyItem;

import java.util.function.Function;
import java.util.function.Supplier;

public final class ModItems {
    private ModItems() {}

    private static ExperienceTargetKeyItem experienceTargetKeyItem;
    public static final Supplier<ExperienceTargetKeyItem> EXPERIENCE_TARGET_KEY = () -> experienceTargetKeyItem;

    private static Item keyBlankItem;
    public static final Supplier<Item> KEY_BLANK = () -> keyBlankItem;

    public static void initialize() {
        ModServerConfig defaults = ModServerConfig.defaults();

        experienceTargetKeyItem = register(
                "experience_key",
                ExperienceTargetKeyItem::new,
                new Item.Properties()
        );

        keyBlankItem = register(
                "key_blank",
                Item::new,
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
