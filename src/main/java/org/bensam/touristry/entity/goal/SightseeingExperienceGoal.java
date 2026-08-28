package org.bensam.touristry.entity.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.bensam.touristry.config.Verbosity;
import org.bensam.touristry.entity.TouristEntity;

public class SightseeingExperienceGoal extends LookAtTargetPosGoal {
    private final TouristEntity tourist;
    private int tickCount;
    private final int timeAtTarget;

    public SightseeingExperienceGoal(TouristEntity tourist, BlockPos targetPos, int startingTickCount, int timeAtTarget) {
        super(tourist, targetPos, false);
        this.tourist = tourist;
        this.tickCount = startingTickCount;
        this.timeAtTarget = timeAtTarget;
    }

    @Override
    public void start() {
        super.start();

        TouristEntity.logActivity(Verbosity.LEVEL_2_DIAGNOSTICS, "[SightseeingExperienceGoal] Starting sightseeing at target for {} ticks", this.timeAtTarget - this.tickCount);
    }

    @Override
    public void tick() {
        super.tick();
        this.tickCount++;

        if (!(this.tourist.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (this.tickCount >= this.timeAtTarget) {
            this.tourist.getMind().finishTargetGoal(serverLevel);
        }
    }
}
