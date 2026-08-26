package org.bensam.touristry.entity.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import org.bensam.touristry.config.Verbosity;
import org.bensam.touristry.entity.TouristEntity;

import java.util.EnumSet;

public class LookAtTargetPosGoal extends Goal {

    private final TouristEntity tourist;
    private final BlockPos targetPos;
    private int tickCount;
    private int ticksUntilNextLookChange;
    private double lookUpOffset;
    private double sideOffset;
    private double xVariation;
    private double yVariation;
    private double zVariation;

    public LookAtTargetPosGoal(TouristEntity tourist, BlockPos targetPos) {
        this.tourist = tourist;
        this.targetPos = targetPos;
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
    public void tick() {
        this.tickCount++;
        
        // Vary the look behavior to appear more natural:
        // 1. Initial focused gaze at target (first 2 seconds)
        // 2. Look up/around briefly (every 1-2 seconds)
        // 3. Add slight random variations when looking at target
        
        if (this.tickCount <= 40) {
            // Initial focused gaze - look directly at target center.
            this.lookAtTarget(0.0, 0.0, 0.0);
        } else {
            // After initial gaze, add variety.
            if (this.ticksUntilNextLookChange <= 0) {
                // Time to change look direction. Prepare variations.
                this.lookUpOffset = 1.0 + this.tourist.getRandom().nextDouble() * 1.5; // Look 1-2.5 blocks up
                this.sideOffset = (this.tourist.getRandom().nextDouble() - 0.5) * 2.0; // -1 to +1 block to side
                this.xVariation = (this.tourist.getRandom().nextDouble() - 0.5) * 0.8; // -0.4 to +0.4
                this.yVariation = (this.tourist.getRandom().nextDouble() - 0.5); // -0.5 to +0.5
                this.zVariation = (this.tourist.getRandom().nextDouble() - 0.5) * 0.8; // -0.4 to +0.4

                // Determine ticks before next change.
                this.ticksUntilNextLookChange = 20 + this.tourist.getRandom().nextInt(20); // 1-2 seconds
            } else {
                this.ticksUntilNextLookChange--;
            }

            // Decide what to look at based on remaining time.
            int phase = this.ticksUntilNextLookChange / 10;
            
            if (phase == 0) {
                // Look up/away from target (last second of cycle).
                this.lookNearTarget(sideOffset, lookUpOffset, sideOffset);
            } else {
                // Look at target with slight random variations.
                this.lookAtTarget(xVariation, yVariation, zVariation);
            }
        }
    }
    
    /**
     * Look at the target with optional offset variations.
     */
    private void lookAtTarget(double xOffset, double yOffset, double zOffset) {
        this.tourist.getLookControl().setLookAt(
            this.targetPos.getX() + 0.5 + xOffset,
            this.targetPos.getY() + 0.5 + yOffset,
            this.targetPos.getZ() + 0.5 + zOffset
        );
    }
    
    /**
     * Look near the target (e.g., above it, to the side).
     */
    private void lookNearTarget(double xOffset, double yOffset, double zOffset) {
        this.tourist.getLookControl().setLookAt(
            this.targetPos.getX() + 0.5 + xOffset,
            this.targetPos.getY() + 0.5 + yOffset,
            this.targetPos.getZ() + 0.5 + zOffset
        );
    }
}
