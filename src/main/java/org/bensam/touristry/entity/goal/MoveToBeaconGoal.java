package org.bensam.touristry.entity.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import org.bensam.touristry.block.entity.TouristBeaconBlockEntity;
import org.bensam.touristry.config.Verbosity;
import org.bensam.touristry.entity.TouristEntity;
import org.bensam.touristry.tourism.TourismManager;
import org.jspecify.annotations.NonNull;

import java.util.EnumSet;

public class MoveToBeaconGoal extends Goal {
    private static final double ARRIVAL_DISTANCE_SQUARED = 4.0;
    private static final int REPATH_INTERVAL_GOALTICKS = 20;
    private static final int CHECK_PROGRESS_GOALTICKS = 40;
    private static final int PROGRESS_CHECK_RETRIES = 5;

    private final TouristEntity tourist;
    private int nextRepathTicks;
    private int nextCheckProgressTicks;

    public MoveToBeaconGoal(TouristEntity tourist) {
        this.tourist = tourist;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return this.tourist.getBeaconTarget() != null && this.tourist.isTravellingToBeacon();
    }

    @Override
    public boolean canContinueToUse() {
        return this.tourist.getBeaconTarget() != null && this.tourist.isTravellingToBeacon();
    }

    @Override
    public void start() {
        this.nextRepathTicks = 0;
        this.nextCheckProgressTicks = CHECK_PROGRESS_GOALTICKS;
        BlockPos beaconTarget = this.tourist.getBeaconTarget();
        if (beaconTarget == null) {
            return;
        }

        if (this.tourist.level().isClientSide()) {
            return;
        }

        TouristBeaconBlockEntity beaconBlockEntity = TourismManager.getBeaconBlockEntity(this.tourist.level(), this.tourist.getBeaconTarget());
        if (beaconBlockEntity != null) {
            TouristEntity.logActivity(Verbosity.LEVEL_2_DIAGNOSTICS, "Starting MoveToBeaconGoal to navigate to " + beaconBlockEntity.getPlainTextName());
        } else {
            TouristEntity.logActivity(Verbosity.GAMEPLAY_WARNINGS, "Starting MoveToBeaconGoal to navigate to *unknown beacon*");
        }

        double distanceToTarget = Math.sqrt(this.getDistanceToBeaconSqr(beaconTarget));
        this.tourist.getMind().recordProgressTowardsBeaconTarget(distanceToTarget, 0);

        if (!this.isAtBeacon(beaconTarget)) {
            this.moveToBeacon();
        } else {
            this.tourist.stopNavigation();
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
            this.tourist.getMind().arriveAtBeacon();
            return;
        }

        if (this.nextRepathTicks > 0) {
            this.nextRepathTicks--;
        }

        if (this.nextRepathTicks <= 0 || this.tourist.getNavigation().isDone()) {
            this.moveToBeacon();
        }

        if (this.nextCheckProgressTicks > 0) {
            this.nextCheckProgressTicks--;
        }

        if (nextCheckProgressTicks <= 0 && !this.isAtBeacon(beaconTarget)) {
            double closestDistanceToBeacon = this.tourist.getClosestDistanceToBeacon();
            double distanceToTarget = Math.sqrt(getDistanceToBeaconSqr(beaconTarget));
            if ((closestDistanceToBeacon - distanceToTarget) < 0.5) {
                int consecutiveFailedProgressChecks = this.tourist.getConsecutiveFailedProgressChecks();
                consecutiveFailedProgressChecks++;
                this.tourist.getMind().recordProgressTowardsBeaconTarget(closestDistanceToBeacon, consecutiveFailedProgressChecks);

                if (consecutiveFailedProgressChecks > PROGRESS_CHECK_RETRIES) {
                    this.tourist.getMind().onLost();
                } else {
                    if (this.tourist.level() instanceof ServerLevel) {
                        TouristEntity.logActivity(Verbosity.LEVEL_2_DIAGNOSTICS, "[MoveToBeaconGoal] " + this.tourist.getDisplayName().getString() + " failed " + consecutiveFailedProgressChecks + " consecutive nav progress checks");
                    }
                }
            } else {
                this.tourist.getMind().recordProgressTowardsBeaconTarget(distanceToTarget, 0);
            }
            this.nextCheckProgressTicks = CHECK_PROGRESS_GOALTICKS;
        }
    }

    @Override
    public void stop() {
        this.tourist.stopNavigation();
    }

    private void moveToBeacon() {
        BlockPos beaconTarget = this.tourist.getBeaconTarget();
        if (beaconTarget == null) {
            return;
        }

        boolean moveStarted = this.tourist.getNavigation().moveTo(
                beaconTarget.getX() + 0.5,
                beaconTarget.getY(),
                beaconTarget.getZ() + 0.5,
                1.0 // speed modifier
        );

        if (!moveStarted && this.tourist.level() instanceof ServerLevel) {
            TouristEntity.logActivity(Verbosity.LEVEL_1_DIAGNOSTICS,
                    "[MoveToBeaconGoal] Unable to path {} toward beacon at {}",
                    this.tourist.getDisplayName().getString(),
                    beaconTarget.toShortString());
        }

        this.nextRepathTicks = REPATH_INTERVAL_GOALTICKS;
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
