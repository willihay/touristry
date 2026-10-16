package org.bensam.touristry.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.bensam.touristry.block.entity.LodgingExperienceBlockEntity;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;

public class LodgingExperienceBlock extends AbstractExperienceBlock {
    private static final VoxelShape BASE = Block.box(-7, 0, 1, 23, 1, 15);
    private static final VoxelShape SHELVES = Block.box(-5, 1, 2, 21, 15, 14);
    private static final VoxelShape DESK = Block.box(-7, 15, 1, 23, 20, 15);
    private static final VoxelShape SHAPE = Shapes.or(BASE, SHELVES, DESK);
    private static final Map<Direction, VoxelShape> SHAPE_BY_DIRECTION = Shapes.rotateHorizontal(SHAPE);

    public static final MapCodec<LodgingExperienceBlock> CODEC = simpleCodec(LodgingExperienceBlock::new);

    public LodgingExperienceBlock(Properties properties) {
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
        if (level.getBlockEntity(blockPos) instanceof LodgingExperienceBlockEntity lodgingExperienceBlockEntity) {
            return lodgingExperienceBlockEntity.isOpenForBusiness() ? 15 : 0;
        }
        return 0;
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState blockState) {
        return SHAPE_BY_DIRECTION.get(blockState.getValue(FACING));
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
        return new LodgingExperienceBlockEntity(blockPos, blockState);
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState blockState) {
        return true;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState blockState, Level level, BlockPos blockPos, Player player, BlockHitResult blockHitResult) {
        if (!level.isClientSide()) {
            if (level.dimension() != Level.OVERWORLD) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("Tourism blocks can only be used in the overworld"), true);
                return InteractionResult.FAIL;
            }

            if (level.getBlockEntity(blockPos) instanceof LodgingExperienceBlockEntity lodgingExperienceBlockEntity) {
                player.openMenu(lodgingExperienceBlockEntity);
            }
        }

        return InteractionResult.SUCCESS;
    }
}
