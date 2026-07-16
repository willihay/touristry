package org.bensam.touristry.entity.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.bensam.touristry.config.Verbosity;
import org.bensam.touristry.entity.TouristEntity;
import org.bensam.touristry.entity.TouristState;

import java.util.EnumSet;

/**
 * Goal that positions the tourist at an ideal viewing position relative to a target.
 * Uses the playerFacing direction to determine where the tourist should stand.
 */
public class PositionForViewingGoal extends Goal {
    private static final int MAX_POSITIONING_TICKS = 100; // 5 seconds timeout
    
    private final TouristEntity tourist;
    private final BlockPos targetPos;
    private final Direction playerFacing;
    private final int idealDistance;
    private final BlockPos idealViewingPos;
    private int ticksPositioning = 0;
    private boolean positioned = false;

    public PositionForViewingGoal(TouristEntity tourist, BlockPos targetPos, Direction playerFacing, int idealDistance) {
        this.tourist = tourist;
        this.targetPos = targetPos;
        this.playerFacing = playerFacing;
        this.idealDistance = idealDistance;
        this.idealViewingPos = this.calculateIdealViewingPosition();
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    private BlockPos calculateIdealViewingPosition() {
        // Stand at ideal distance from target, on the same side the player was standing when the target was registered.
        // For example, if player was facing NORTH looking at the target, player was standing SOUTH of target,
        // so we move SOUTH from target (opposite of player's facing direction).
        return this.targetPos.relative(this.playerFacing.getOpposite(), this.idealDistance);
    }

    @Override
    public boolean canUse() {
        return this.tourist.getMind().getState() == TouristState.POSITIONING_AT_TARGET;
    }

    @Override
    public boolean canContinueToUse() {
        return this.canUse() && !this.positioned && this.ticksPositioning < MAX_POSITIONING_TICKS;
    }

    @Override
    public void start() {
        if (!(this.tourist.level() instanceof ServerLevel)) {
            return;
        }

        TouristEntity.logActivity(Verbosity.LEVEL_2_DIAGNOSTICS, 
            "[PositionForViewingGoal] Positioning {} at ideal viewing position {} ({}  blocks from target at {})",
            this.tourist.getDisplayName().getString(),
            this.idealViewingPos.toShortString(),
            this.idealDistance,
            this.targetPos.toShortString());

        // Check if tourist can see target from ideal position.
        if (!this.hasLineOfSight()) {
            TouristEntity.logActivity(Verbosity.LEVEL_2_DIAGNOSTICS,
                "[PositionForViewingGoal] No line of sight from ideal position, skipping fine positioning");
            this.finishPositioning();
            return;
        }

        // Move to ideal viewing position.
        boolean moveStarted = this.tourist.getNavigation().moveTo(
            this.idealViewingPos.getX() + 0.5,
            this.idealViewingPos.getY(),
            this.idealViewingPos.getZ() + 0.5,
            1.0 // speed modifier
        );

        if (!moveStarted) {
            TouristEntity.logActivity(Verbosity.LEVEL_2_DIAGNOSTICS,
                "[PositionForViewingGoal] Unable to path to ideal position, skipping fine positioning");
            this.finishPositioning();
        }
    }

    @Override
    public void tick() {
        this.ticksPositioning++;

        // Check if we've reached the ideal position (within 1.5 blocks)
        if (this.tourist.blockPosition().closerToCenterThan(this.idealViewingPos.getCenter(), 1.5)) {
            this.positioned = true;
            
            // Orient tourist to look at target
            this.tourist.getLookControl().setLookAt(
                this.targetPos.getX() + 0.5,
                this.targetPos.getY() + 0.5,
                this.targetPos.getZ() + 0.5
            );

            TouristEntity.logActivity(Verbosity.LEVEL_2_DIAGNOSTICS,
                "[PositionForViewingGoal] Positioned at ideal viewing position");
            
            this.finishPositioning();
        }
    }

    @Override
    public void stop() {
        if (!this.positioned && this.ticksPositioning >= MAX_POSITIONING_TICKS) {
            TouristEntity.logActivity(Verbosity.LEVEL_2_DIAGNOSTICS,
                "[PositionForViewingGoal] Positioning timeout, proceeding anyway");
        }

        // Always finish positioning when goal stops, even if not at ideal position.
        if (!this.positioned) {
            this.finishPositioning();
        }
    }

    private void finishPositioning() {
        if (!(this.tourist.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        this.tourist.getMind().finishPositioning(serverLevel);
    }

    private boolean hasLineOfSight() {
        if (!(this.tourist.level() instanceof ServerLevel serverLevel)) {
            return true; // Assume true if not on server
        }

        Vec3 fromPos = new Vec3(
            this.idealViewingPos.getX() + 0.5,
            this.idealViewingPos.getY() + 1.5, // Eye level
            this.idealViewingPos.getZ() + 0.5
        );

        Vec3 toPos = new Vec3(
            this.targetPos.getX() + 0.5,
            this.targetPos.getY() + 0.5,
            this.targetPos.getZ() + 0.5
        );

        ClipContext context = new ClipContext(
            fromPos,
            toPos,
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            this.tourist
        );

        HitResult hitResult = serverLevel.clip(context);
        
        // If we didn't hit anything, line of sight is clear
        if (hitResult.getType() == HitResult.Type.MISS) {
            return true;
        }
        
        // If we hit something, check if it's at the target position (hitting the target itself is OK!)
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockPos hitPos = BlockPos.containing(hitResult.getLocation());
            // Allow hits at or very close to the target position (target entity/block itself)
            return hitPos.equals(this.targetPos) || 
                   hitPos.closerThan(this.targetPos, 1.5);
        }
        
        return false;
    }
}
