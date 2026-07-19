package org.bensam.touristry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import org.bensam.touristry.menu.ShoppingExperienceMenu;
import org.bensam.touristry.menu.SightseeingExperienceMenu;
import org.bensam.touristry.menu.TouristBeaconMenu;

import java.util.function.Supplier;

public final class ModMenus {
    private ModMenus() {}

    private static MenuType<TouristBeaconMenu> touristBeaconMenu;
    public static final Supplier<MenuType<TouristBeaconMenu>> TOURIST_BEACON_MENU = () -> touristBeaconMenu;

    private static MenuType<ShoppingExperienceMenu> shoppingExperienceMenu;
    public static final Supplier<MenuType<ShoppingExperienceMenu>> SHOPPING_EXPERIENCE_MENU = () -> shoppingExperienceMenu;

    private static MenuType<SightseeingExperienceMenu> sightseeingExperienceMenu;
    public static final Supplier<MenuType<SightseeingExperienceMenu>> SIGHTSEEING_EXPERIENCE_MENU = () -> sightseeingExperienceMenu;

    public static void initialize() {
        touristBeaconMenu = register("tourist_beacon_menu", TouristBeaconMenu::new);
        shoppingExperienceMenu = register("shopping_experience_menu", ShoppingExperienceMenu::new);
        sightseeingExperienceMenu = register("sightseeing_experience_menu", SightseeingExperienceMenu::new);
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
