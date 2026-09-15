package org.bensam.touristry.entity.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.goal.Goal;
import org.bensam.touristry.config.Verbosity;
import org.bensam.touristry.entity.TouristEntity;

import java.util.EnumSet;

public class LookAtTargetPosGoal extends Goal {

    private final TouristEntity tourist;
    private final BlockPos targetPos;
    private final boolean alwaysLookAtTarget;
    private final boolean canUseCamera;
    private final int initialGazeTicks;
    private int tickCount;
    private int ticksUntilNextLookChange;
    private boolean hasTakenPicture;
    private double lookUpOffset;
    private double xVariation;
    private double yVariation;
    private double zVariation;

    public LookAtTargetPosGoal(TouristEntity tourist, BlockPos targetPos, boolean alwaysLookAtTarget, boolean canUseCamera) {
        this.tourist = tourist;
        this.targetPos = targetPos;
        this.alwaysLookAtTarget = alwaysLookAtTarget;
        this.canUseCamera = canUseCamera;
        this.initialGazeTicks = this.adjustedTickDelay(30 + this.tourist.getRandom().nextInt(20));
        this.setFlags(EnumSet.of(Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return this.tourist.isAtExperienceTarget();
    }

    @Override
    public void start() {
        if (this.tourist.level().isClientSide()) {
            return;
        }
        TouristEntity.logActivity(Verbosity.LEVEL_2_DIAGNOSTICS, "[LookAtTargetPosGoal] Starting to look at target at " + this.targetPos.toShortString());
    }

    @Override
    public void stop() {
        this.tourist.setCrouching(false);
        this.tourist.setUsingCamera(false);
    }

    @Override
    public void tick() {
        this.tickCount++;

        if (this.tourist.level().isClientSide()) {
            return;
        }

        // Vary the look behavior to appear more natural:
        // 1. Initial focused gaze at target (first 2 seconds, approximately)
        // 2. Look up/around briefly (every 1-2 seconds)
        // 3. Add slight random variations when looking at target
        
        if (this.tickCount <= this.initialGazeTicks) {
            // Initial focused gaze - look directly at target center.
            this.lookAtTarget(0.0, 0.0, 0.0);
        } else {
            // After initial gaze, or at calculated intervals, vary the look-at direction.
            if (this.ticksUntilNextLookChange <= 0) {
                // Time to change look direction. Prepare variations.
                this.chooseLookActionAndDirection();
                this.hasTakenPicture = false;
            } else {
                this.ticksUntilNextLookChange--;
            }

            // Decide what to look at based on remaining time and other factors.
            boolean readyToTakePicture = !hasTakenPicture && this.tourist.isUsingCamera() && (this.ticksUntilNextLookChange / 15) == 0;
            if (readyToTakePicture) {
                this.tourist.takePicture();
                this.hasTakenPicture = true;
            }

            boolean almostReadyToChangeGaze = (this.ticksUntilNextLookChange / 10) == 0;
            if (almostReadyToChangeGaze && !this.alwaysLookAtTarget && !this.tourist.isUsingCamera()) {
                // Look up from target.
                this.lookAtTarget(0, lookUpOffset, 0);
            } else {
                // Look at target with slight random variations.
                this.lookAtTarget(xVariation, yVariation, zVariation);
            }
        }
    }

    private void chooseLookActionAndDirection() {
        RandomSource random = this.tourist.getRandom();

        // Determine if tourist is going to use (or continue using) camera.
        boolean usingCamera = this.canUseCamera && random.nextDouble() < 0.25;
        this.tourist.setUsingCamera(usingCamera);

        // Calculate look offsets.
        this.lookUpOffset = 0.5 + random.nextDouble(); // Look 0.5 to 1.5 blocks up
        this.xVariation = (random.nextDouble() - 0.5) * 0.8; // -0.4 to +0.4
        this.yVariation = random.nextDouble() * 0.5; // 0 to +0.5
        this.zVariation = (random.nextDouble() - 0.5) * 0.8; // -0.4 to +0.4
        if (usingCamera && random.nextDouble() < 0.25) {
            this.yVariation += 0.5 + (random.nextDouble() * 2.0); // occasionally add 0.5 to 2 blocks up
        }

        // Determine if tourist will be crouching to get a closer look.
        this.tourist.setCrouching(this.targetPos.getY() + 0.75 + this.yVariation < this.tourist.blockPosition().getY());

        // Determine ticks before next change.
        if (usingCamera) {
            this.ticksUntilNextLookChange = this.adjustedTickDelay(40 + random.nextInt(60)); // 2-5 seconds
        } else {
            this.ticksUntilNextLookChange = this.adjustedTickDelay(20 + random.nextInt(20)); // 1-2 seconds
        }
    }

    private void lookAtTarget(double xOffset, double yOffset, double zOffset) {
        this.tourist.getLookControl().setLookAt(
            this.targetPos.getX() + 0.5 + xOffset,
            this.targetPos.getY() + 0.5 + yOffset,
            this.targetPos.getZ() + 0.5 + zOffset
        );
    }
}
