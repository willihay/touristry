package org.bensam.touristry.entity.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.bensam.touristry.config.Verbosity;
import org.bensam.touristry.entity.TouristEntity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class OpenFenceGateGoal extends Goal {
    private static final double MAX_GATE_DISTANCE_SQ = 2.25D;
    private static final int CLOSE_DELAY_TICKS = 15;

    private final Mob mob;
    private final Map<BlockPos, OpenedGate> openedGates = new HashMap<>();

    private record OpenedGate(Block block, long closeAfter) {}

    public OpenFenceGateGoal(Mob mob) {
        this.mob = mob;
    }

    @Override
    public boolean canUse() {
        ServerLevel level = getServerLevel(this.mob);
        for (BlockPos pos : this.findGates(level).keySet()) {
            if (!level.getBlockState(pos).getValue(FenceGateBlock.OPEN)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return !this.openedGates.isEmpty();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        ServerLevel level = getServerLevel(this.mob);
        long closeAfter = level.getGameTime() + CLOSE_DELAY_TICKS;

        for (Map.Entry<BlockPos, String> candidate : this.findGates(level).entrySet()) {
            BlockPos pos = candidate.getKey();
            BlockState state = level.getBlockState(pos);
            OpenedGate opened = this.openedGates.get(pos);
            if (opened != null && state.is(opened.block()) && state.getValue(FenceGateBlock.OPEN)) {
                this.openedGates.put(pos, new OpenedGate(opened.block(), closeAfter));
            } else if (!state.getValue(FenceGateBlock.OPEN)) {
                this.setOpen(level, pos, state, true, candidate.getValue());
                this.openedGates.put(pos.immutable(), new OpenedGate(state.getBlock(), closeAfter));
            }
        }

        Iterator<Map.Entry<BlockPos, OpenedGate>> iterator = this.openedGates.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, OpenedGate> entry = iterator.next();
            BlockPos pos = entry.getKey();
            if (!level.hasChunkAt(pos)) {
                this.logGate("leaving open: chunk unloaded", pos);
                iterator.remove();
                continue;
            }

            BlockState state = level.getBlockState(pos);
            if (!state.is(entry.getValue().block()) || !state.getValue(FenceGateBlock.OPEN)) {
                this.logGate("stopped tracking: gate changed", pos);
                iterator.remove();
            } else if (state.getValue(FenceGateBlock.POWERED)) {
                this.logGate("leaving open: powered", pos);
                iterator.remove();
            } else if (level.getGameTime() >= entry.getValue().closeAfter()) {
                if (this.isDoorwayClear(level, pos)) {
                    this.setOpen(level, pos, state, false, "doorway clear");
                } else {
                    this.logGate("leaving open: doorway occupied", pos);
                }
                iterator.remove();
            }
        }
    }

    @Override
    public void stop() {
        ServerLevel level = getServerLevel(this.mob);
        for (Map.Entry<BlockPos, OpenedGate> entry : this.openedGates.entrySet()) {
            BlockPos pos = entry.getKey();
            if (!level.hasChunkAt(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.is(entry.getValue().block()) && state.getValue(FenceGateBlock.OPEN)
                    && !state.getValue(FenceGateBlock.POWERED) && this.isDoorwayClear(level, pos)) {
                this.setOpen(level, pos, state, false, "goal stopped and doorway clear");
            } else {
                this.logGate("leaving open: goal stopped or gate changed", pos);
            }
        }
        this.openedGates.clear();
    }

    private Map<BlockPos, String> findGates(ServerLevel level) {
        Map<BlockPos, String> gates = new LinkedHashMap<>();
        Path path = this.mob.getNavigation().getPath();
        if (path != null && !path.isDone()) {
            int end = Math.min(path.getNextNodeIndex() + 2, path.getNodeCount());
            for (int i = path.getNextNodeIndex(); i < end; i++) {
                Node node = path.getNode(i);
                BlockPos pos = new BlockPos(node.x, node.y, node.z);
                double dx = this.mob.getX() - (pos.getX() + 0.5D);
                double dz = this.mob.getZ() - (pos.getZ() + 0.5D);
                if (dx * dx + dz * dz <= MAX_GATE_DISTANCE_SQ
                        && Math.abs(this.mob.getY() - pos.getY()) <= 1.0D) {
                    this.addGateAndVerticalParts(level, gates, pos, "path ahead");
                    this.addGateAndVerticalParts(level, gates, pos.above(), "path ahead");
                }
            }
        }

        AABB box = this.mob.getBoundingBox();
        for (BlockPos pos : BlockPos.betweenClosed(
                Mth.floor(box.minX), Mth.floor(box.minY) - 1, Mth.floor(box.minZ),
                Mth.floor(box.maxX), Mth.floor(box.maxY), Mth.floor(box.maxZ))) {
            if (!level.hasChunkAt(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof FenceGateBlock && !state.getValue(FenceGateBlock.OPEN)
                    && state.getCollisionShape(level, pos, CollisionContext.of(this.mob)).move(pos)
                    .toAabbs().stream().anyMatch(box::intersects)) {
                this.addGateAndVerticalParts(level, gates, pos, "intersecting tourist");
            }
        }
        return gates;
    }

    private void addGateAndVerticalParts(ServerLevel level, Map<BlockPos, String> gates, BlockPos pos, String reason) {
        if (!level.hasChunkAt(pos) || !(level.getBlockState(pos).getBlock() instanceof FenceGateBlock)) {
            return;
        }
        gates.putIfAbsent(pos.immutable(), reason);
        for (BlockPos adjacent : new BlockPos[]{pos.below(), pos.above()}) {
            if (level.hasChunkAt(adjacent) && level.getBlockState(adjacent).getBlock() instanceof FenceGateBlock) {
                gates.putIfAbsent(adjacent, reason);
            }
        }
    }

    private boolean isDoorwayClear(ServerLevel level, BlockPos pos) {
        AABB doorway = new AABB(pos).expandTowards(0.0D, 0.5D, 0.0D);
        if (level.getBlockState(pos.below()).getBlock() instanceof FenceGateBlock) {
            doorway = doorway.expandTowards(0.0D, -1.0D, 0.0D);
        }
        if (level.getBlockState(pos.above()).getBlock() instanceof FenceGateBlock) {
            doorway = doorway.expandTowards(0.0D, 1.0D, 0.0D);
        }
        return level.getEntitiesOfClass(LivingEntity.class, doorway.inflate(0.1D, 0.0D, 0.1D),
                entity -> entity.isAlive() && !entity.isSpectator()).isEmpty();
    }

    private void setOpen(ServerLevel level, BlockPos pos, BlockState state, boolean open, String reason) {
        state = state.setValue(FenceGateBlock.OPEN, open);
        if (open && state.getValue(FenceGateBlock.FACING) == this.mob.getDirection().getOpposite()) {
            state = state.setValue(FenceGateBlock.FACING, this.mob.getDirection());
        }
        level.setBlock(pos, state, 10);
        level.playSound(null, pos, open ? SoundEvents.FENCE_GATE_OPEN : SoundEvents.FENCE_GATE_CLOSE,
                SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.1F + 0.9F);
        level.gameEvent(this.mob, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
        this.logGate((open ? "opened: " : "closed: ") + reason, pos);
    }

    private void logGate(String action, BlockPos pos) {
        TouristEntity.logActivity(Verbosity.LEVEL_1_DIAGNOSTICS, "[{}] {} ({}) {} gate at {}",
                this.getClass().getSimpleName(), this.mob.getDisplayName().getString(),
                this.mob.getUUID(), action, pos.toShortString());
    }
}
