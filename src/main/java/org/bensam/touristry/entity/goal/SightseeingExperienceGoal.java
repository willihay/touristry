package org.bensam.touristry.entity.goal;

import net.minecraft.core.BlockPos;
import org.bensam.touristry.config.Verbosity;
import org.bensam.touristry.entity.TouristEntity;

public class SightseeingExperienceGoal extends LookAtTargetPosGoal {
    private final TouristEntity tourist;
    private final int durationAtTarget;
    private int tickCount;
    private final int adjustedTimeAtTarget;

    public SightseeingExperienceGoal(TouristEntity tourist, BlockPos targetPos, int startingTickCount, int timeAtTarget) {
        super(tourist, targetPos, false);
        this.tourist = tourist;
        this.tickCount = this.adjustedTickDelay(startingTickCount);
        this.adjustedTimeAtTarget = this.adjustedTickDelay(timeAtTarget);
        this.durationAtTarget = Math.max(0, timeAtTarget - startingTickCount);
    }

    @Override
    public void start() {
        super.start();

        TouristEntity.logActivity(Verbosity.LEVEL_2_DIAGNOSTICS, "[SightseeingExperienceGoal] Starting sightseeing at target for {} ticks",
                this.durationAtTarget);
    }

    @Override
    public void tick() {
        super.tick();
        this.tickCount++;

        if (this.tourist.level().isClientSide()) {
            return;
        }

        if (this.tickCount >= this.adjustedTimeAtTarget) {
            this.tourist.getMind().finishTargetGoal();
        }
    }
}
