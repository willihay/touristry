package org.bensam.touristry.entity.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import org.bensam.touristry.config.Verbosity;
import org.bensam.touristry.entity.TouristEntity;
import org.jspecify.annotations.NonNull;

import java.util.EnumSet;

public class MoveToTargetGoal extends Goal {
    private static final double ARRIVAL_DISTANCE_SQUARED = 4.0;
    private static final int REPATH_INTERVAL_GOALTICKS = 20;
    private static final int CHECK_PROGRESS_GOALTICKS = 40;
    private static final int PROGRESS_CHECK_RETRIES = 5;

    private final TouristEntity tourist;
    private int nextRepathTicks;
    private int nextCheckProgressTicks;

    public MoveToTargetGoal(TouristEntity tourist) {
        this.tourist = tourist;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return this.tourist.getMoveToTarget() != null && this.tourist.isTraveling();
    }

    @Override
    public boolean canContinueToUse() {
        return this.tourist.getMoveToTarget() != null && this.tourist.isTraveling();
    }

    @Override
    public void start() {
        this.nextRepathTicks = 0;
        this.nextCheckProgressTicks = CHECK_PROGRESS_GOALTICKS;
        BlockPos targetPos = this.tourist.getMoveToTarget();
        if (targetPos == null) {
            return;
        }

        if (this.tourist.level().isClientSide()) {
            return;
        }

        String targetName = this.tourist.getMoveToTargetName();
        if (!targetName.isEmpty()) {
            TouristEntity.logActivity(Verbosity.LEVEL_2_DIAGNOSTICS, "[MoveToTargetGoal] Starting navigation to " + targetName);
        } else {
            TouristEntity.logActivity(Verbosity.GAMEPLAY_WARNINGS, "[MoveToTargetGoal] Starting navigation to (unknown) " + targetPos.toShortString());
        }

        double distanceToTarget = Math.sqrt(this.getDistanceToTargetSqr(targetPos));
        this.tourist.getMind().recordProgressTowardsTarget(distanceToTarget, 0);

        if (!this.isAtTarget(targetPos)) {
            this.moveToTarget();
        } else {
            this.tourist.stopNavigation();
        }
    }

    @Override
    public void tick() {
        // DEV NOTE: This Goal tick() runs at 10 TPS because Goal.requiresUpdateEveryTick() is false.

        BlockPos targetPos = this.tourist.getMoveToTarget();
        if (targetPos == null) {
            return;
        }

        if (this.isAtTarget(targetPos)) {
            this.tourist.getMind().arriveAtDestination();
            return;
        }

        if (this.nextRepathTicks > 0) {
            this.nextRepathTicks--;
        }

        if (this.nextRepathTicks <= 0 || this.tourist.getNavigation().isDone()) {
            this.moveToTarget();
        }

        if (this.nextCheckProgressTicks > 0) {
            this.nextCheckProgressTicks--;
        }

        // Check on progress towards target and report. Determine if tourist is lost.
        if (nextCheckProgressTicks <= 0 && !this.isAtTarget(targetPos)) {
            double closestDistanceToTarget = this.tourist.getClosestDistanceToTarget();
            double distanceToTarget = Math.sqrt(getDistanceToTargetSqr(targetPos));
            if ((closestDistanceToTarget - distanceToTarget) < 0.5) {
                int consecutiveFailedProgressChecks = this.tourist.getConsecutiveFailedProgressChecks();
                consecutiveFailedProgressChecks++;
                this.tourist.getMind().recordProgressTowardsTarget(closestDistanceToTarget, consecutiveFailedProgressChecks);

                if (consecutiveFailedProgressChecks > PROGRESS_CHECK_RETRIES) {
                    this.tourist.getMind().onLost();
                } else {
                    if (this.tourist.level() instanceof ServerLevel) {
                        TouristEntity.logActivity(Verbosity.LEVEL_2_DIAGNOSTICS, "[MoveToTargetGoal] " + this.tourist.getDisplayName().getString() + " failed " + consecutiveFailedProgressChecks + " consecutive nav progress checks");
                    }
                }
            } else {
                this.tourist.getMind().recordProgressTowardsTarget(distanceToTarget, 0);
            }
            this.nextCheckProgressTicks = CHECK_PROGRESS_GOALTICKS;
        }
    }

    @Override
    public void stop() {
        this.tourist.stopNavigation();
    }

    private void moveToTarget() {
        BlockPos targetPos = this.tourist.getMoveToTarget();
        if (targetPos == null) {
            return;
        }

        boolean moveStarted = this.tourist.getNavigation().moveTo(
                targetPos.getX() + 0.5,
                targetPos.getY(),
                targetPos.getZ() + 0.5,
                1.0 // speed modifier
        );

        if (!moveStarted && this.tourist.level() instanceof ServerLevel) {
            TouristEntity.logActivity(Verbosity.LEVEL_1_DIAGNOSTICS,
                    "[MoveToTargetGoal] Unable to path {} toward target at {}",
                    this.tourist.getDisplayName().getString(),
                    targetPos.toShortString());
        }

        this.nextRepathTicks = REPATH_INTERVAL_GOALTICKS;
    }

    private double getDistanceToTargetSqr(BlockPos targetPos) {
        double targetCenterX = targetPos.getX() + 0.5;
        double targetCenterY = targetPos.getY();
        double targetCenterZ = targetPos.getZ() + 0.5;
        return this.tourist.distanceToSqr(targetCenterX, targetCenterY, targetCenterZ);
    }

    private boolean isAtTarget(@NonNull BlockPos targetPos) {
        return this.getDistanceToTargetSqr(targetPos) <= ARRIVAL_DISTANCE_SQUARED;
    }
}
