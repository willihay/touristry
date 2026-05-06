package org.bensam.touristry.client.integration.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.Identifier;
import org.bensam.touristry.Touristry;
import org.jspecify.annotations.NonNull;

@JeiPlugin
public class JEIPlugin implements IModPlugin {
    private static final Identifier PLUGIN_UID = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "jei_plugin");

    @Override
    public @NonNull Identifier getPluginUid() {
        return PLUGIN_UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        //registration.addRecipeCategories();
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        //registration.addRecipes();
    }
}
