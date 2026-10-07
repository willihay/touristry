package org.bensam.touristry.entity.navigation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

public class TouristWalkNodeEvaluator extends WalkNodeEvaluator {
    @Override
    public PathType getPathType(PathfindingContext pathfindingContext, int i, int j, int k) {
        BlockPos blockPos = new BlockPos(i, j, k);
        BlockState blockState = pathfindingContext.level().getBlockState(blockPos);
        if (blockState.getBlock() instanceof FenceGateBlock && !blockState.getValue(FenceGateBlock.OPEN)) {
            return PathType.DOOR_WOOD_CLOSED; // treat closed gate like a wooden door
        }

        return super.getPathType(pathfindingContext, i, j, k);
    }
}
