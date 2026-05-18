package org.bensam.touristry.entity.goal;

import net.minecraft.world.entity.ai.goal.Goal;
import org.bensam.touristry.entity.TouristEntity;

import java.util.EnumSet;

public class LookAtTargetPosGoal extends Goal {

    private final TouristEntity tourist;

    public LookAtTargetPosGoal(TouristEntity tourist) {
        this.tourist = tourist;
        this.setFlags(EnumSet.of(Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // TODO ...
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return super.canContinueToUse();
    }

    @Override
    public void start() {
        super.start();
    }

    @Override
    public void stop() {
        super.stop();
    }

    @Override
    public void tick() {
        super.tick();
    }
}
