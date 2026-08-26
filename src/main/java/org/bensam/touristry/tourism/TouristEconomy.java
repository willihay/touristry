package org.bensam.touristry.tourism;

import net.minecraft.world.item.ItemStack;
import org.bensam.touristry.block.entity.AbstractExperienceBlockEntity;
import org.bensam.touristry.tourism.experience.ItemPrice;
import org.jspecify.annotations.NonNull;

public class TouristEconomy {

    private TouristEconomy() {}

    public static void initialize() {}

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
}
