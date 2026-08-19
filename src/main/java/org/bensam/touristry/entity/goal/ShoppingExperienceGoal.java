package org.bensam.touristry.entity.goal;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import org.bensam.touristry.block.entity.ShoppingExperienceBlockEntity;
import org.bensam.touristry.entity.TouristEntity;

public class ShoppingExperienceGoal extends Goal {

    private final TouristEntity tourist;
    private final ShoppingExperienceBlockEntity shoppingExperience;
    private int ticksAtExperience;

    public ShoppingExperienceGoal(TouristEntity tourist, ShoppingExperienceBlockEntity shoppingExperience) {
        this.tourist = tourist;
        this.shoppingExperience = shoppingExperience;
    }

    @Override
    public boolean canUse() {
        return this.tourist.getMind().getState().isAtExperience();
    }

    @Override
    public void start() {
        if (!(this.tourist.level() instanceof ServerLevel)) {
            return;
        }


    }

    @Override
    public void tick() {
        this.ticksAtExperience++;

    }

    public int getTicksAtExperience() {
        return this.ticksAtExperience;
    }
}
