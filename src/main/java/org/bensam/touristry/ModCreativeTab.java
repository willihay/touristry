package org.bensam.touristry;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

public final class ModCreativeTab {
    private ModCreativeTab() {}

    public static final ResourceKey<CreativeModeTab> CUSTOM_CREATIVE_TAB_KEY =
            ResourceKey.create(
                    BuiltInRegistries.CREATIVE_MODE_TAB.key(),
                    Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "creative_tab")
            );

    private static CreativeModeTab tabInternal;
    public static final Supplier<CreativeModeTab> CUSTOM_CREATIVE_TAB = () -> tabInternal;

    public static void initialize() {
        // Build the custom creative tab.
        tabInternal = FabricItemGroup.builder()
                .icon(() -> new ItemStack(ModBlocks.TOURIST_BEACON.get().asItem()))
                .title(Component.translatable("itemGroup." + Touristry.MOD_ID))
                .displayItems((params, output) -> {
                    output.accept(ModBlocks.TOURIST_BEACON.get().asItem());
                    output.accept(ModBlocks.SHOPPING_EXPERIENCE.get().asItem());
                    output.accept(ModBlocks.SIGHTSEEING_EXPERIENCE.get().asItem());
                    output.accept(ModItems.KEY_BLANK.get());
                })
                .build();

        // Register the custom creative tab.
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, CUSTOM_CREATIVE_TAB_KEY, tabInternal);
    }
}
