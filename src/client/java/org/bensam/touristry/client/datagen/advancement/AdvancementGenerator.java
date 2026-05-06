package org.bensam.touristry.client.datagen.advancement;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricAdvancementProvider;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.ItemLike;
import org.bensam.touristry.Touristry;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class AdvancementGenerator extends FabricAdvancementProvider {
    public AdvancementGenerator(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registryLookup) {
        super(output, registryLookup);
    }

    @Override
    public void generateAdvancement(HolderLookup.Provider registryLookup, Consumer<AdvancementHolder> consumer) {
//        AdvancementHolder arcaneRelics = Advancement.Builder.advancement()
//                .display(
//                        ModItems.ARCANE_WAND.get(), // display icon
//                        Component.translatable(advTranslationTitle("root")), // title
//                        Component.translatable(advTranslationDesc("root")), // description
//                        Identifier.parse("minecraft:block/obsidian"), // background for this tab's advancements page
//                        AdvancementType.TASK,
//                        true, // show toast on completion
//                        true, // announce it to chat
//                        false // hide until achieved
//                )
//                .addCriterion("has_ender_eye", InventoryChangeTrigger.TriggerInstance.hasItems(Items.ENDER_EYE))
//                .rewards(new AdvancementRewards.Builder()
//                        .addRecipe(recipeKey("arcane_wand"))
//                        .addRecipe(recipeKey("wand_enchanting_table")))
//                .save(consumer, advName("relics/root"));

//        AdvancementHolder craftWandEnchantingTable = newChildAdvancement(
//                arcaneRelics,
//                ModBlocks.WAND_ENCHANTING_TABLE.get().asItem(),
//                "wand_enchanting_table",
//                AdvancementType.TASK,
//                true, true, false)
//                .addCriterion("has_wand_enchanting_table", InventoryChangeTrigger.TriggerInstance.hasItems(ModBlocks.WAND_ENCHANTING_TABLE.get().asItem()))
//                .save(consumer, advName("relics/wand_enchanting_table"));
    }

    private static Advancement.Builder newChildAdvancement(
            AdvancementHolder parent,
            ItemLike displayItem,
            String advName,
            AdvancementType advType,
            boolean showToast,
            boolean announce,
            boolean hide
    ) {
        return Advancement.Builder.advancement().parent(parent)
                .display(
                        displayItem,
                        Component.translatable(advTranslationTitle(advName)),
                        Component.translatable(advTranslationDesc(advName)),
                        null,
                        advType,
                        showToast,
                        announce,
                        hide
                );
    }

    private static String advName(String path) {
        return Touristry.MOD_ID + ":" + path;
    }

    private static String advTranslationTitle(String advancement) {
        return "advancement." + Touristry.MOD_ID + "." + advancement + ".title";
    }

    private static String advTranslationDesc(String advancement) {
        return "advancement." + Touristry.MOD_ID + "." + advancement + ".description";
    }

    private static ResourceKey<Recipe<?>> recipeKey(String path) {
        return ResourceKey.create(
                Registries.RECIPE,
                Identifier.fromNamespaceAndPath(Touristry.MOD_ID, path)
        );
    }
}
