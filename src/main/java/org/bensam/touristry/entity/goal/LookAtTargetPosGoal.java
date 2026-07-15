package org.bensam.touristry.entity.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.ai.goal.Goal;
import org.bensam.touristry.config.Verbosity;
import org.bensam.touristry.entity.TouristEntity;
import org.bensam.touristry.entity.TouristState;

import java.util.EnumSet;

public class LookAtTargetPosGoal extends Goal {

    private final TouristEntity tourist;
    private final BlockPos targetPos;
    private final Direction playerFacing;

    public LookAtTargetPosGoal(TouristEntity tourist, BlockPos targetPos, Direction playerFacing) {
        this.tourist = tourist;
        this.targetPos = targetPos;
        this.playerFacing = playerFacing;
        this.setFlags(EnumSet.of(Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return this.tourist.getMind().getState() == TouristState.EXPERIENCING_TARGET;
    }

    @Override
    public boolean canContinueToUse() {
        return this.canUse();
    }

    @Override
    public void start() {
        if (this.tourist.level().isClientSide()) {
            return;
        }

        TouristEntity.logActivity(Verbosity.LEVEL_2_DIAGNOSTICS, "[LookAtTargetPosGoal] Starting to look at target at " + this.targetPos.toShortString());
    }

    @Override
    public void tick() {
        super.tick();
    }
}
