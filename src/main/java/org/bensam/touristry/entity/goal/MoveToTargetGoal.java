package org.bensam.touristry.entity.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.bensam.touristry.config.ModServerConfigManager;
import org.bensam.touristry.config.Verbosity;
import org.bensam.touristry.entity.TouristEntity;
import org.bensam.touristry.entity.navigation.TouristPathNavigation;

import java.util.EnumSet;
import java.util.StringJoiner;

public class MoveToTargetGoal extends Goal {
    private static final boolean VERBOSE_LOGGING_OVERRIDE = false; // TODO: set to false before publishing
    private static final double IDEAL_DISTANCE_SQUARED = 2.25D;
    private static final double MAX_VERTICAL_DIFFERENCE = 2.0D;
    private static final int MAX_NAV_EXPANSION_LEVEL = 5;
    private static final int FAILED_NAV_RETRY_TICKS = 60;
    private static final int NAVIGATION_LIMIT_TICKS = 60*20; // 60 seconds * 20 ticks/second (pre-adjusted)
    private static final int NAVIGATION_LOG_TICKS = 20;
    private static final int MAX_LOGGED_PATH_NODES = 32;

    private final TouristEntity tourist;
    private final float defaultPathLength;

    private BlockPos targetPos;
    private BlockPos navPos;

    private boolean couldNotUpdatePath;
    private boolean finalRetryAttempted;
    private boolean finished;
    private int maxAcceptableDistance;
    private double maxAcceptableDistanceSq;
    private int navExpansionLevel;
    private boolean navFailed;
    private int navRetryTicks;
    private boolean retryIdealPath;
    private int timerTicks;

    private Verbosity currentVerbosityLevel;
    private boolean verboseLogging;
    private int nextNavigationLogTicks;
    private boolean observedActivePath;
    private BlockPos lastPathNode;
    private Vec3 lastPathPosition;
    private int lastPathNodeIndex;
    private int lastPathNodeCount;
    private boolean lastPathHorizontalCollision;

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
        this.targetPos = this.tourist.getMind().getMoveToTargetPos(false);
        this.navPos = this.tourist.getMind().getMoveToTargetPos(true);
        if (this.navPos == null) {
            this.navPos = this.targetPos;
        }

        this.couldNotUpdatePath = false;
        this.retryIdealPath = true;
        this.finalRetryAttempted = false;
        this.finished = false;
        this.maxAcceptableDistance = this.tourist.getMind().getMaxDistanceAwayFromTarget();
        this.maxAcceptableDistanceSq = (double) this.maxAcceptableDistance * this.maxAcceptableDistance;
        this.navExpansionLevel = 1;
        this.navFailed = false;
        this.navRetryTicks = this.adjustedTickDelay(FAILED_NAV_RETRY_TICKS);
        this.timerTicks = this.adjustedTickDelay(NAVIGATION_LIMIT_TICKS);

        this.currentVerbosityLevel = ModServerConfigManager.getConfig().touristEntityConfig().getVerbosityLevel();
        this.verboseLogging = VERBOSE_LOGGING_OVERRIDE || this.currentVerbosityLevel == Verbosity.LEVEL_1_DIAGNOSTICS;
        this.nextNavigationLogTicks = 0;
        this.observedActivePath = false;

        String targetName = this.tourist.getMind().getStateTargetName();
        if (!targetName.isEmpty()) {
            TouristEntity.logActivity(Verbosity.LEVEL_2_DIAGNOSTICS, "[{}] Starting navigation to {}, navigating to {}",
                    this.getClass().getSimpleName(),
                    targetName,
                    this.navPos.toShortString()
            );
        } else {
            TouristEntity.logActivity(Verbosity.GAMEPLAY_WARNINGS, "[{}] Starting navigation to unknown target, navigating to {}",
                    this.getClass().getSimpleName(),
                    this.navPos.toShortString()
            );
        }

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

        this.observeNavigation();

        if (this.isAtTarget()) {
            this.finished = true;
            this.tourist.getMind().arriveAtDestination();
            return;
        }

        if (this.timerTicks <= 0) {
            this.finishNavigation();
            return;
        }

        if (this.couldNotUpdatePath) {
            if (this.canMove()) {
                this.couldNotUpdatePath = false;
                this.moveToTarget();
            }
        } else if (this.tourist.getNavigation().isDone()) {
            // A completed or interrupted path may still end short of the desired position.
            if (this.navFailed) {
                if (this.finalRetryAttempted) {
                    this.finishNavigation();
                    return;
                }

                if (this.navRetryTicks > 0) {
                    this.navRetryTicks--;
                } else if (this.canMove()) {
                    // Don't consume the final retry while an airborne tourist can't update its path.
                    this.finalRetryAttempted = true;
                    this.moveToTarget();
                    if (this.tourist.getNavigation().isDone()) {
                        this.finishNavigation();
                        return;
                    }
                }
            } else if (this.navExpansionLevel == MAX_NAV_EXPANSION_LEVEL) {
                // The most lenient path also gets one delayed retry if it ended short.
                this.navFailed = true;
                this.navRetryTicks = this.adjustedTickDelay(FAILED_NAV_RETRY_TICKS);
            } else {
                if (this.navExpansionLevel == 1 && this.retryIdealPath) {
                    // Retry the ideal position once before widening the search after a path ends early.
                    this.retryIdealPath = false;
                } else {
                    this.navExpansionLevel++;
                }
                this.moveToTarget();
            }
        }

        this.timerTicks--;
    }

    @Override
    public void stop() {
        this.observeNavigation();
        if (this.observedActivePath) {
            this.logNavigationEnd("goal stopped");
        }

        this.tourist.getNavigation().setRequiredPathLength(this.defaultPathLength);
        this.tourist.getNavigation().stop();
    }

    private boolean canMove() {
        return (this.tourist.getNavigation() instanceof TouristPathNavigation touristNav) && touristNav.canUpdatePath();
    }

    private void moveToTarget() {
        if (!this.canMove()) {
            // Ground navigation may be unavailable until a newly spawned or airborne tourist lands.
            this.couldNotUpdatePath = true;
            return;
        }

        PathNavigation nav = this.tourist.getNavigation();
        int expansionLevel = this.navExpansionLevel;
        BlockPos destination = this.navPos;
        int accuracy = 1;

        nav.stop();
        Path path = null;

        while (expansionLevel <= MAX_NAV_EXPANSION_LEVEL) {
            if (expansionLevel == 2) {
                accuracy = 2;
            } else if (expansionLevel == 3) {
                destination = this.targetPos;
                accuracy = this.maxAcceptableDistance;
            } else if (expansionLevel >= 4) {
                nav.setRequiredPathLength(this.defaultPathLength * 2);
                if (expansionLevel == 5) {
                    destination = this.targetPos;
                    accuracy = this.maxAcceptableDistance;
                }
            }

            path = nav.createPath(destination, accuracy);
            if (path != null) {
                // A partial path may still make progress, so follow it if navigation accepts it.
                if (nav.moveTo(path, 1.0)) {
                    break;
                } else {
                    path = null;
                }
            }

            nav.stop();
            expansionLevel++;
            destination = this.navPos;
            accuracy = 1;
        }

        if (expansionLevel > MAX_NAV_EXPANSION_LEVEL) {
            this.navExpansionLevel = MAX_NAV_EXPANSION_LEVEL;
            this.navFailed = true;
        } else {
            this.navExpansionLevel = expansionLevel;
        }

        if (path == null) {
            TouristEntity.logActivity(Verbosity.LEVEL_2_DIAGNOSTICS,
                    "[{}] Unable to start path for {} toward {}",
                    this.getClass().getSimpleName(),
                    this.tourist.getDisplayName().getString(),
                    destination.toShortString());
        } else {
            if (!this.verboseLogging) {
                if (!path.canReach()) {
                    TouristEntity.logActivity(Verbosity.LEVEL_2_DIAGNOSTICS,
                            "[{}] Following partial path for {} toward {}",
                            this.getClass().getSimpleName(),
                            this.tourist.getDisplayName().getString(),
                            destination.toShortString());
                }
            } else {
                StringJoiner nodes = new StringJoiner(" -> ");
                int loggedNodes = Math.min(path.getNodeCount(), MAX_LOGGED_PATH_NODES);
                for (int i = 0; i < loggedNodes; i++) {
                    nodes.add(path.getNodePos(i).toShortString());
                }
                if (loggedNodes < path.getNodeCount()) {
                    nodes.add("...");
                }

                TouristEntity.logActivity(this.currentVerbosityLevel,
                        "[{}] {} ({}) path expansionLevel={}/{} destination={} accuracy={} pathTarget={} canReach={} end={} nodes=[{}]",
                        this.getClass().getSimpleName(),
                        this.tourist.getDisplayName().getString(),
                        this.tourist.getUUID(),
                        expansionLevel,
                        MAX_NAV_EXPANSION_LEVEL,
                        destination.toShortString(),
                        accuracy,
                        path.getTarget().toShortString(),
                        path.canReach(),
                        path.getEndNode() == null ? "none" : path.getEndNode().asBlockPos().toShortString(),
                        nodes);

                this.observeNavigation();
            }
        }
    }

    private void finishNavigation() {
        double dx = this.tourist.getX() - (this.targetPos.getX() + 0.5);
        double dy = this.tourist.getY() - this.targetPos.getY();
        double dz = this.tourist.getZ() - (this.targetPos.getZ() + 0.5);
        double horizontalDistanceSq = dx * dx + dz * dz;

        this.finished = true;
        // Fallback arrival is measured from the actual target, with a separate vertical bound.
        if (horizontalDistanceSq <= this.maxAcceptableDistanceSq && Math.abs(dy) <= MAX_VERTICAL_DIFFERENCE) {
            this.tourist.getMind().arriveAtDestination();
        } else {
            this.tourist.getMind().onLost();
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

    private void logNavigationEnd(String reason) {
        TouristEntity.logActivity(this.currentVerbosityLevel,
                "[{}] {} ({}) navigation ended: {} time={} pos={} expansionLevel={}/{} isStuck={} lastPos={} lastNode={}/{} at {} lastHorizontalCollision={}",
                this.getClass().getSimpleName(),
                this.tourist.getDisplayName().getString(),
                this.tourist.getUUID(),
                reason,
                this.tourist.level().getGameTime(),
                this.tourist.position(),
                this.navExpansionLevel,
                MAX_NAV_EXPANSION_LEVEL,
                this.tourist.getNavigation().isStuck(),
                this.lastPathPosition,
                this.lastPathNodeIndex,
                this.lastPathNodeCount,
                this.lastPathNode.toShortString(),
                this.lastPathHorizontalCollision);

        this.observedActivePath = false;
        this.nextNavigationLogTicks = 0;
    }

    private void observeNavigation() {
        if (!this.verboseLogging) {
            return;
        }

        PathNavigation nav = this.tourist.getNavigation();
        Path path = nav.getPath();
        if (path == null || path.isDone()) {
            if (this.observedActivePath) {
                this.logNavigationEnd(path == null ? "path cleared" : "path completed");
            }
            return;
        }

        this.observedActivePath = true;
        this.lastPathNode = path.getNextNodePos();
        this.lastPathPosition = this.tourist.position();
        this.lastPathNodeIndex = path.getNextNodeIndex();
        this.lastPathNodeCount = path.getNodeCount();
        this.lastPathHorizontalCollision = this.tourist.horizontalCollision;

        if (this.nextNavigationLogTicks <= 0) {
            TouristEntity.logActivity(this.currentVerbosityLevel,
                    "[{}] {} ({}) navigating: time={} pos={} node={}/{} at {} expansionLevel={}/{} horizontalCollision={} onGround={} isStuck={}",
                    this.getClass().getSimpleName(),
                    this.tourist.getDisplayName().getString(),
                    this.tourist.getUUID(),
                    this.tourist.level().getGameTime(),
                    this.lastPathPosition,
                    this.lastPathNodeIndex,
                    this.lastPathNodeCount,
                    this.lastPathNode.toShortString(),
                    this.navExpansionLevel,
                    MAX_NAV_EXPANSION_LEVEL,
                    this.lastPathHorizontalCollision,
                    this.tourist.onGround(),
                    nav.isStuck());

            this.nextNavigationLogTicks = this.adjustedTickDelay(NAVIGATION_LOG_TICKS);
        } else {
            this.nextNavigationLogTicks--;
        }
    }
}
