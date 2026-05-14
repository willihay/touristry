package org.bensam.touristry.entity.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import org.bensam.touristry.Touristry;
import org.bensam.touristry.entity.TouristEntity;
import org.jspecify.annotations.NonNull;

import java.util.EnumSet;

public class MoveToBeaconGoal extends Goal {
    private static final double ARRIVAL_DISTANCE_SQUARED = 4.0;
    private static final int REPATH_INTERVAL_GOALTICKS = 20;
    private static final int CHECK_PROGRESS_GOALTICKS = 40;
    private static final int PROGRESS_CHECK_RETRIES = 5;

    private final TouristEntity tourist;
    private final double speedModifier;
    private int nextRepathTick;
    private int nextCheckProgressTick;
    private int failedProgressChecksRemaining;
    private double closestCheckedDistance;

    public MoveToBeaconGoal(TouristEntity tourist, double speedModifier) {
        this.tourist = tourist;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return this.tourist.getBeaconTarget() != null && !this.tourist.hasCompletedVisit();
    }

    @Override
    public boolean canContinueToUse() {
        return this.tourist.getBeaconTarget() != null && !this.tourist.hasCompletedVisit();
    }

    @Override
    public void start() {
        this.nextRepathTick = 0;
        this.nextCheckProgressTick = CHECK_PROGRESS_GOALTICKS;
        this.failedProgressChecksRemaining = PROGRESS_CHECK_RETRIES;
        this.closestCheckedDistance = Double.MAX_VALUE;
        BlockPos beaconTarget = this.tourist.getBeaconTarget();
        if (beaconTarget != null && !this.isAtBeacon(beaconTarget)) {
            this.closestCheckedDistance = Math.sqrt(this.getDistanceToBeaconSqr(beaconTarget));
            this.moveToBeacon();
        } else {
            this.tourist.getNavigation().stop();
        }
    }

    @Override
    public void tick() {
        // DEV NOTE: This Goal tick() runs at 10 TPS because Goal.requiresUpdateEveryTick() is false.

        BlockPos beaconTarget = this.tourist.getBeaconTarget();
        if (beaconTarget == null) {
            return;
        }

        if (this.isAtBeacon(beaconTarget)) {
            this.tourist.getNavigation().stop();
            this.tourist.onArrivedAtBeacon();
            return;
        }

        if (this.nextRepathTick > 0) {
            this.nextRepathTick--;
        }

        if (this.nextRepathTick <= 0 || this.tourist.getNavigation().isDone()) {
            this.moveToBeacon();
        }

        if (this.nextCheckProgressTick > 0) {
            this.nextCheckProgressTick--;
        }

        if (nextCheckProgressTick <= 0 && !this.isAtBeacon(beaconTarget)) {
            double distanceToTarget = Math.sqrt(getDistanceToBeaconSqr(beaconTarget));
            if ((this.closestCheckedDistance - distanceToTarget) < 0.5) {
                this.failedProgressChecksRemaining--;
                if (this.failedProgressChecksRemaining <= 0) {
                    this.tourist.onNavigationFailed();
                } else {
                    if (this.tourist.level() instanceof ServerLevel) {
                        Touristry.LOGGER.info("[MoveToBeaconGoal] {} nav retries remaining: {}", this.tourist.getDisplayName().getString(), this.failedProgressChecksRemaining);
                    }
                }
            } else {
                this.failedProgressChecksRemaining = PROGRESS_CHECK_RETRIES;
                this.closestCheckedDistance = distanceToTarget;
            }
            this.nextCheckProgressTick = CHECK_PROGRESS_GOALTICKS;
        }
    }

    @Override
    public void stop() {
        this.tourist.getNavigation().stop();
    }

    private void moveToBeacon() {
        BlockPos beaconTarget = this.tourist.getBeaconTarget();
        if (beaconTarget == null) {
            return;
        }

        boolean isMoving = this.tourist.getNavigation().moveTo(
                beaconTarget.getX() + 0.5,
                beaconTarget.getY(),
                beaconTarget.getZ() + 0.5,
                this.speedModifier
        );

        this.nextRepathTick = REPATH_INTERVAL_GOALTICKS;
    }

    private double getDistanceToBeaconSqr(BlockPos beaconTarget) {
        double beaconCenterX = beaconTarget.getX() + 0.5;
        double beaconCenterY = beaconTarget.getY();
        double beaconCenterZ = beaconTarget.getZ() + 0.5;
        return this.tourist.distanceToSqr(beaconCenterX, beaconCenterY, beaconCenterZ);
    }

    private boolean isAtBeacon(@NonNull BlockPos beaconTarget) {
        return this.getDistanceToBeaconSqr(beaconTarget) <= ARRIVAL_DISTANCE_SQUARED;
    }
}
