package org.bensam.touristry.entity.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Path;
import org.bensam.touristry.config.Verbosity;
import org.bensam.touristry.entity.TouristEntity;

import java.util.EnumSet;

public class MoveToTargetGoal extends Goal {
    private static final double IDEAL_DISTANCE_SQUARED = 2.25D;
    private static final double MAX_VERTICAL_DIFFERENCE = 2.0D;
    private static final int NAVIGATION_LIMIT_TICKS = 60*20; // 60 seconds * 20 ticks/second (pre-adjusted)
    private static final int PROGRESS_CHECK_TICKS = 60;
    private static final int PROGRESS_CHECK_RETRIES = 4;

    private final TouristEntity tourist;
    private final float defaultPathLength;

    private boolean finished;
    private BlockPos targetPos;
    private BlockPos navPos;
    private int maxAcceptableDistance;
    private double maxAcceptableDistanceSq;
    private int nextProgressCheckTicks;
    private int timerTicks;

    public MoveToTargetGoal(TouristEntity tourist) {
        this.tourist = tourist;
        this.defaultPathLength = this.tourist.getDefaultNavPathLength();
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return this.tourist.getMind().getMoveToTargetPos(false) != null && this.tourist.isTraveling();
    }

    @Override
    public boolean canContinueToUse() {
        return !this.finished
                && this.tourist.isTraveling()
                && this.targetPos.equals(this.tourist.getMind().getMoveToTargetPos(false));
    }

    @Override
    public void start() {
        this.finished = false;
        this.timerTicks = this.adjustedTickDelay(NAVIGATION_LIMIT_TICKS);
        this.nextProgressCheckTicks = this.adjustedTickDelay(PROGRESS_CHECK_TICKS);
        this.maxAcceptableDistance = this.tourist.getMind().getMaxDistanceAwayFromTarget();
        this.maxAcceptableDistanceSq = (double) this.maxAcceptableDistance * this.maxAcceptableDistance;

        this.targetPos = this.tourist.getMind().getMoveToTargetPos(false);
        this.navPos = this.tourist.getMind().getMoveToTargetPos(true);
        if (this.navPos == null) {
            this.navPos = this.targetPos;
        }

        String targetName = this.tourist.getMind().getStateTargetName();
        if (!targetName.isEmpty()) {
            TouristEntity.logActivity(Verbosity.LEVEL_2_DIAGNOSTICS, "[{}] Starting navigation to {} at {}",
                    this.getClass().getSimpleName(),
                    targetName,
                    this.navPos.toShortString()
            );
        } else {
            TouristEntity.logActivity(Verbosity.GAMEPLAY_WARNINGS, "[{}] Starting navigation to unknown target at {}",
                    this.getClass().getSimpleName(),
                    this.navPos.toShortString()
            );
        }

        this.tourist.getMind().recordFailedProgressChecks(0);
        double distanceToTarget = Math.sqrt(this.getDistanceToNavPosSqr());
        this.tourist.getMind().recordProgressTowardsTarget(distanceToTarget);

        if (!this.isAtTarget()) {
            this.moveToTarget();
        } else {
            this.finished = true;
            this.tourist.getMind().arriveAtDestination();
        }
    }

    @Override
    public void tick() {
        if (this.finished) {
            return;
        }

        if (this.isAtTarget()) {
            this.finished = true;
            this.tourist.getMind().arriveAtDestination();
            return;
        }

        if (this.timerTicks <= 0) {
            double distanceToTarget = Math.sqrt(this.getDistanceToNavPosSqr());
            this.tourist.getMind().recordProgressTowardsTarget(distanceToTarget);
            this.finishNavigation();
            return;
        }

        if (this.nextProgressCheckTicks <= 0) {
            double distanceToTarget = Math.sqrt(this.getDistanceToNavPosSqr());
            this.tourist.getMind().recordProgressTowardsTarget(distanceToTarget);

            if (!this.tourist.getNavigation().isInProgress()) {
                int consecutiveFailedProgressChecks = this.tourist.getMind().getConsecutiveFailedProgressChecks();
                consecutiveFailedProgressChecks++;
                this.tourist.getMind().recordFailedProgressChecks(consecutiveFailedProgressChecks);

                TouristEntity.logActivity(Verbosity.LEVEL_2_DIAGNOSTICS, "[{}] {} failed {} consecutive navigation progress checks",
                        this.getClass().getSimpleName(),
                        this.tourist.getDisplayName().getString(),
                        consecutiveFailedProgressChecks
                );

                if (consecutiveFailedProgressChecks > PROGRESS_CHECK_RETRIES) {
                    this.finishNavigation();
                    return;
                }
                this.moveToTarget();
            }

            this.nextProgressCheckTicks = this.adjustedTickDelay(PROGRESS_CHECK_TICKS);
        } else {
            this.nextProgressCheckTicks--;
        }

        this.timerTicks--;
    }

    @Override
    public void stop() {
        this.tourist.getNavigation().setRequiredPathLength(this.defaultPathLength);
        this.tourist.getNavigation().stop();
    }

    private void moveToTarget() {
        PathNavigation nav = this.tourist.getNavigation();
        int retry = this.tourist.getMind().getConsecutiveFailedProgressChecks();
        BlockPos destination = this.navPos;
        int accuracy = 1;

        if (retry == 1) {
            accuracy = 2;
        } else if (retry == 2) {
            destination = this.targetPos;
            accuracy = this.maxAcceptableDistance;
        } else if (retry >= 3) {
            nav.setRequiredPathLength(this.defaultPathLength * 2);
            if (retry >= 4) {
                destination = this.targetPos;
                accuracy = this.maxAcceptableDistance;
            }
        }

        nav.stop();
        Path path = nav.createPath(destination, accuracy);
        if (path == null || !nav.moveTo(path, 1.0)) {
            TouristEntity.logActivity(Verbosity.LEVEL_2_DIAGNOSTICS,
                    "[{}] Unable to start path for {} toward {}",
                    this.getClass().getSimpleName(),
                    this.tourist.getDisplayName().getString(),
                    destination.toShortString());
        } else if (!path.canReach()) {
            TouristEntity.logActivity(Verbosity.LEVEL_2_DIAGNOSTICS,
                    "[{}] Following partial path for {} toward {}",
                    this.getClass().getSimpleName(),
                    this.tourist.getDisplayName().getString(),
                    destination.toShortString());
        }
    }

    private double getDistanceToNavPosSqr() {
        double targetCenterX = this.navPos.getX() + 0.5;
        double targetCenterY = this.navPos.getY();
        double targetCenterZ = this.navPos.getZ() + 0.5;
        return this.tourist.distanceToSqr(targetCenterX, targetCenterY, targetCenterZ);
    }

    private boolean isAtTarget() {
        return this.getDistanceToNavPosSqr() <= IDEAL_DISTANCE_SQUARED;
    }

    private void finishNavigation() {
        double dx = this.tourist.getX() - (this.targetPos.getX() + 0.5);
        double dy = this.tourist.getY() - this.targetPos.getY();
        double dz = this.tourist.getZ() - (this.targetPos.getZ() + 0.5);
        double horizontalDistanceSq = dx * dx + dz * dz;

        this.finished = true;
        if (horizontalDistanceSq <= this.maxAcceptableDistanceSq && Math.abs(dy) <= MAX_VERTICAL_DIFFERENCE) {
            this.tourist.getMind().arriveAtDestination();
        } else {
            this.tourist.getMind().onLost();
        }
    }
}
