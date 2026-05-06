package org.bensam.touristry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

public class ModMenus {
    private ModMenus() {}

//    private static MenuType<WandEnchantingMenu> wandEnchantingMenuInternal;
//    public static final Supplier<MenuType<WandEnchantingMenu>> WAND_ENCHANTING_MENU = () -> wandEnchantingMenuInternal;

    public static void initialize() {
        // wandEnchantingMenuInternal = register("wand_enchanting_menu", WandEnchantingMenu::new);
    }

    public static <T extends AbstractContainerMenu> MenuType<T> register(
            String name,
            MenuType.MenuSupplier<T> menuFactory
    ) {
        return Registry.register(
                BuiltInRegistries.MENU,
                Identifier.fromNamespaceAndPath(Touristry.MOD_ID, name),
                new MenuType<>(menuFactory, FeatureFlagSet.of())
        );
    }
}
