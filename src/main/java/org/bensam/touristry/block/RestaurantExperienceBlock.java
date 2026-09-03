package org.bensam.touristry.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.bensam.touristry.block.entity.RestaurantExperienceBlockEntity;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;

public class RestaurantExperienceBlock extends TouristExperienceBlock {
    private static final VoxelShape BASE = Block.column(14.0, 0.0, 2.0);
    private static final VoxelShape SHELVES = Block.box(3.0, 2.0, 4.0, 13.0, 14.0, 13.0);
    private static final VoxelShape SHAPE_COLLISION = Shapes.or(BASE, SHELVES);
    private static final Map<Direction, VoxelShape> SHAPE_BY_DIRECTION = Shapes.rotateHorizontal(
            Shapes.or(
                    Block.boxZ(16.0, 11.0, 16.0, 1.0, 5.333333), Block.boxZ(16.0, 13.0, 18.0, 5.333333, 9.666667), Block.boxZ(16.0, 15.0, 20.0, 9.666667, 14.0), SHAPE_COLLISION
            )
    );

    public static final MapCodec<RestaurantExperienceBlock> CODEC = simpleCodec(RestaurantExperienceBlock::new);

    public RestaurantExperienceBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void affectNeighborsAfterRemoval(@NonNull BlockState blockState, @NonNull ServerLevel serverLevel, @NonNull BlockPos blockPos, boolean bl) {
        Containers.updateNeighboursAfterDestroy(blockState, serverLevel, blockPos);
    }

    @Override
    protected int getAnalogOutputSignal(@NonNull BlockState blockState, Level level, @NonNull BlockPos blockPos, @NonNull Direction direction) {
        if (level.getBlockEntity(blockPos) instanceof RestaurantExperienceBlockEntity restaurantExperienceBlockEntity) {
            return restaurantExperienceBlockEntity.isOpenForBusiness() ? 15 : 0;
        }
        return 0;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext) {
        return SHAPE_COLLISION;
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState blockState) {
        return SHAPE_COLLISION;
    }

    @Override
    protected VoxelShape getShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext) {
        return SHAPE_BY_DIRECTION.get(blockState.getValue(FACING));
    }

    @Override
    protected boolean hasAnalogOutputSignal(@NonNull BlockState blockState) {
        return true;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new RestaurantExperienceBlockEntity(blockPos, blockState);
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState blockState) {
        return true;
    }

}
