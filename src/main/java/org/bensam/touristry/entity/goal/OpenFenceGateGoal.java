package org.bensam.touristry.entity.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;

public class OpenFenceGateGoal extends Goal {
    private final Mob mob;
    protected BlockPos gatePos = BlockPos.ZERO;
    protected boolean hasGate;
    protected int closeTimer;

    public OpenFenceGateGoal(Mob mob) {
        this.mob = mob;
    }

    @Override
    public boolean canUse() {
        if (!this.mob.horizontalCollision) {
            return false;
        }

        Path path = this.mob.getNavigation().getPath();
        if (path != null && !path.isDone()) {
            for (int i = 0; i < Math.min(path.getNextNodeIndex() + 2, path.getNodeCount()); i++) {
                Node node = path.getNode(i);
                this.gatePos = new BlockPos(node.x, node.y, node.z);
                if (!(this.mob.distanceToSqr(this.gatePos.getX(), this.mob.getY(), this.gatePos.getZ()) > 2.25)) {
                    BlockState blockState = this.mob.level().getBlockState(this.gatePos);
                    this.hasGate = blockState.getBlock() instanceof FenceGateBlock;
                    if (this.hasGate) {
                        return true;
                    }
                }
            }

            this.gatePos = this.mob.blockPosition().above();
            BlockState blockState = this.mob.level().getBlockState(this.gatePos);
            this.hasGate = blockState.getBlock() instanceof FenceGateBlock;
            return this.hasGate;
        } else {
            return false;
        }
    }

    @Override
    public boolean canContinueToUse() {
        return this.closeTimer > 0;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        this.closeTimer = 15;
        this.setOpen(true);
    }

    @Override
    public void stop() {
        this.setOpen(false);
    }

    @Override
    public void tick() {
        this.closeTimer--;
    }

    protected void setOpen(boolean setOpen) {
        if (this.hasGate) {
            BlockState blockState = this.mob.level().getBlockState(this.gatePos);
            if (blockState.getBlock() instanceof FenceGateBlock) {
                this.setOpen(setOpen, this.gatePos);
            }

            // Handle double-height gates.
            blockState = this.mob.level().getBlockState(this.gatePos.above());
            if (blockState.getBlock() instanceof FenceGateBlock) {
                this.setOpen(setOpen, this.gatePos.above());
            }
        }
    }

    private void setOpen(boolean setOpen, BlockPos gatePos) {
        BlockState blockState = this.mob.level().getBlockState(gatePos);
        if (blockState.getValue(FenceGateBlock.OPEN) != setOpen) {
            blockState = blockState.setValue(FenceGateBlock.OPEN, setOpen);

            if (setOpen) {
                Direction direction = this.mob.getDirection();
                if (blockState.getValue(FenceGateBlock.FACING) == direction.getOpposite()) {
                    blockState = blockState.setValue(FenceGateBlock.FACING, direction);
                }
                this.mob.level().playSound(null, gatePos, SoundEvents.FENCE_GATE_OPEN, SoundSource.BLOCKS, 1.0F, this.mob.level().getRandom().nextFloat() * 0.1F + 0.9F);
                this.mob.level().gameEvent(this.mob, GameEvent.BLOCK_OPEN, gatePos);
            } else {
                this.mob.level().playSound(null, gatePos, SoundEvents.FENCE_GATE_CLOSE, SoundSource.BLOCKS, 1.0F, this.mob.level().getRandom().nextFloat() * 0.1F + 0.9F);
                this.mob.level().gameEvent(this.mob, GameEvent.BLOCK_CLOSE, gatePos);
            }

            this.mob.level().setBlock(gatePos, blockState, 10);
        }
    }
}
