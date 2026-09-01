package org.bensam.touristry.tourism;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.bensam.touristry.block.entity.AbstractExperienceBlockEntity;
import org.bensam.touristry.tourism.experience.ItemPrice;
import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TouristEconomy {
    private static final Map<CreativeModeTab, List<ItemStack>> CREATIVE_TAB_CACHE = new HashMap<>();

    private TouristEconomy() {}

    public static void initialize(Level level) {
        CreativeModeTabs.tryRebuildTabContents(FeatureFlagSet.of(FeatureFlags.VANILLA), false, level.registryAccess());
        for (CreativeModeTab tab : CreativeModeTabs.allTabs()) {
            CREATIVE_TAB_CACHE.put(tab, List.copyOf(tab.getDisplayItems()));
        }
    }

    public static float getEmeraldEquivalent(ItemStack itemStack) {
        if (itemStack == null) {
            return 0;
        }

        return itemStack.getCount();
    }

    /**
     * Returns a value judgment on the indicated entry fee for the specific experience.
     * A positive value indicates a good value, whereas
     * a negative value indicates a poor value.
     */
    public static float getEntryFeeValueAssessment(@NonNull AbstractExperienceBlockEntity experienceBlockEntity, @NonNull ItemStack itemStack) {
        // TODO Complete value assessment algorithm.
        float assessment = 0;

        // TODO Store assessment for historical averages.

        return assessment;
    }

    /**
     * Returns a value judgment based on the current, broad tourist economy.
     * A positive value indicates a price better than market rates, whereas
     * a negative value indicates a price worse than going rates for the item for sale.
     */
    public static float getItemValueAssessment(@NonNull ItemPrice itemPrice) {
        // TODO Complete value assessment algorithm.
        float assessment = 0;

        // TODO Store assessment for historical averages.

        return assessment;
    }

    public static boolean isInCreativeTab(Level level, ItemStack itemStack, ResourceKey<CreativeModeTab> creativeModeTabKey) {
        HolderLookup.Provider provider = level.registryAccess();
        Holder<CreativeModeTab> creativeModeTabHolder = provider.lookupOrThrow(Registries.CREATIVE_MODE_TAB).getOrThrow(creativeModeTabKey);
        CreativeModeTab tab = creativeModeTabHolder.value();

        List<ItemStack> items = CREATIVE_TAB_CACHE.get(tab);
        if (items == null) {
            return false;
        }

        for (ItemStack item : items) {
            if (ItemStack.isSameItemSameComponents(itemStack, item)) {
                return true;
            }
        }
        return false;
    }
}
