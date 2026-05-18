package org.bensam.touristry.entity.goal;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import org.bensam.touristry.entity.TouristEntity;

import java.util.EnumSet;

public class TouristRandomLookAroundGoal extends RandomLookAroundGoal {

    private final TouristEntity tourist;

    public TouristRandomLookAroundGoal(TouristEntity tourist) {
        super(tourist);
        this.tourist = tourist;
        this.setFlags(EnumSet.of(Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return !this.tourist.isTravellingToBeacon() && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return !this.tourist.isTravellingToBeacon() && super.canContinueToUse();
    }

    @Override
    public void start() {
        super.start();

        if (this.tourist.level().isClientSide()) return;

        TouristEntity.logActivity("Starting TouristRandomLookAroundGoal");
    }
}
