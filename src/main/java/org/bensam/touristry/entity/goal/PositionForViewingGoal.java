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
    private static final int MAX_POSITIONING_TICKS = 20;
    private static final double LOOK_AT_DISTANCE_SQ = 9.0D; // squared distance at which tourist will start looking at target
    
    private final TouristEntity tourist;
    private final BlockPos targetPos;
    private final Direction playerFacing;
    private final int idealDistance;
    private final BlockPos idealViewingPos;
    private int ticksPositioning = 0;
    private boolean positioned = false;
    private boolean aborted = false;

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
        return this.canUse() && !this.aborted && !this.positioned && this.ticksPositioning < MAX_POSITIONING_TICKS;
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
            TouristEntity.logActivity(Verbosity.LEVEL_1_DIAGNOSTICS,
                "[PositionForViewingGoal] No line of sight from ideal position, skipping fine positioning");
            this.aborted = true;
            return;
        }

        // Move to ideal viewing position.
        boolean moveStarted = this.tourist.getNavigation().moveTo(
            this.idealViewingPos.getX(),
            this.idealViewingPos.getY(),
            this.idealViewingPos.getZ(),
            1.0 // speed modifier
        );

        if (!moveStarted) {
            TouristEntity.logActivity(Verbosity.LEVEL_1_DIAGNOSTICS,
                "[PositionForViewingGoal] Unable to path to ideal position, skipping fine positioning");
            this.aborted = true;
        }
    }

    @Override
    public void tick() {
        this.ticksPositioning++;

        if (this.tourist.blockPosition().distManhattan(this.idealViewingPos) <= LOOK_AT_DISTANCE_SQ) {
            // Orient tourist to look at target.
            this.tourist.getLookControl().setLookAt(
                    this.targetPos.getX() + 0.5,
                    this.targetPos.getY() + 0.5,
                    this.targetPos.getZ() + 0.5
            );
        }

        // Check if tourist has reached the ideal XZ position (okay for Y to be less than ideal).
        BlockPos idealPos = new BlockPos(this.idealViewingPos.getX(), this.tourist.blockPosition().getY(), this.idealViewingPos.getZ());
        if (this.tourist.blockPosition().distManhattan(idealPos) <= this.idealDistance) {
            this.positioned = true;

            TouristEntity.logActivity(Verbosity.LEVEL_1_DIAGNOSTICS,
                "[PositionForViewingGoal] Positioned at ideal viewing position");
            
            this.finishPositioning();
        }
    }

    @Override
    public void stop() {
        if (!this.positioned && this.ticksPositioning >= MAX_POSITIONING_TICKS) {
            TouristEntity.logActivity(Verbosity.LEVEL_1_DIAGNOSTICS,
                "[PositionForViewingGoal] Positioning timeout, orienting tourist and proceeding anyway");

            this.tourist.getLookControl().setLookAt(
                    this.targetPos.getX() + 0.5,
                    this.targetPos.getY() + 0.5,
                    this.targetPos.getZ() + 0.5
            );
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

        // Create 2 vantage points (fromPos) and check line of sight to target (toPos).
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

        fromPos = fromPos.subtract(0.0, 1.0, 0.0);
        ClipContext context2 = new ClipContext(
                fromPos,
                toPos,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                this.tourist
        );
        HitResult hitResult2 = serverLevel.clip(context2);
        
        // If we didn't hit anything from either vantage point, line of sight is clear
        if (hitResult.getType() == HitResult.Type.MISS || hitResult2.getType() == HitResult.Type.MISS ||
                hitResult.getType() == HitResult.Type.ENTITY || hitResult2.getType() == HitResult.Type.ENTITY) {
            return true;
        }
        
        // If we hit something, check if it's at the target position (hitting the target itself is OK!)
        if (hitResult.getType() == HitResult.Type.BLOCK || hitResult2.getType() == HitResult.Type.BLOCK) {
            BlockPos hitPos = BlockPos.containing(hitResult.getLocation());
            BlockPos hitPos2 = BlockPos.containing(hitResult2.getLocation());

            // Allow hits at or very close to the target position (target entity/block itself)
            return hitPos.equals(this.targetPos) || 
                   hitPos.closerThan(this.targetPos, 1.5) ||
                    hitPos2.equals(this.targetPos) ||
                    hitPos2.closerThan(this.targetPos, 1.5);
        }
        
        return false;
    }
}
