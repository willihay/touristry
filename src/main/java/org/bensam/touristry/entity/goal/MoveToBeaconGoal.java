package org.bensam.touristry.entity.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import org.bensam.touristry.entity.TouristEntity;
import org.jspecify.annotations.NonNull;

import java.util.EnumSet;

public class MoveToBeaconGoal extends Goal {
    private static final double ARRIVAL_DISTANCE_SQUARED = 4.0;
    private static final int REPATH_INTERVAL_TICKS = 20;

    private final TouristEntity tourist;
    private final double speedModifier;
    private int nextRepathTick;

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
        BlockPos beaconTarget = this.tourist.getBeaconTarget();
        if (beaconTarget != null && !this.isAtBeacon(beaconTarget)) {
            this.moveToBeacon();
        } else {
            this.tourist.getNavigation().stop();
        }
    }

    @Override
    public void tick() {
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

        this.tourist.getNavigation().moveTo(
                beaconTarget.getX() + 0.5,
                beaconTarget.getY(),
                beaconTarget.getZ() + 0.5,
                this.speedModifier
        );
        this.nextRepathTick = REPATH_INTERVAL_TICKS;
    }

    private boolean isAtBeacon(@NonNull BlockPos beaconTarget) {
        double beaconCenterX = beaconTarget.getX() + 0.5;
        double beaconCenterY = beaconTarget.getY();
        double beaconCenterZ = beaconTarget.getZ() + 0.5;
        double distanceSqd = this.tourist.distanceToSqr(beaconCenterX, beaconCenterY, beaconCenterZ);
        return distanceSqd <= ARRIVAL_DISTANCE_SQUARED;
    }
}
