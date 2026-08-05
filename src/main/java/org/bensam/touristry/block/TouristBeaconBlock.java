package org.bensam.touristry.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.bensam.touristry.block.entity.TouristBeaconBlockEntity;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class TouristBeaconBlock extends BaseEntityBlock {
    public static final MapCodec<TouristBeaconBlock> CODEC = simpleCodec(TouristBeaconBlock::new);
    public static final BooleanProperty OPEN_FOR_BUSINESS = BooleanProperty.create("open_for_business");

	public TouristBeaconBlock(Properties properties)
	{
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(OPEN_FOR_BUSINESS, false));
    }

    @Override
    protected @NonNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void affectNeighborsAfterRemoval(@NonNull BlockState blockState, @NonNull ServerLevel serverLevel, @NonNull BlockPos blockPos, boolean bl) {
        Containers.updateNeighboursAfterDestroy(blockState, serverLevel, blockPos);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OPEN_FOR_BUSINESS);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        if (context.getLevel().dimension() != Level.OVERWORLD) {
            return null;
        }
        return super.getStateForPlacement(context);
    }

    @Override
    protected int getAnalogOutputSignal(@NonNull BlockState blockState, Level level, @NonNull BlockPos blockPos, @NonNull Direction direction) {
        if (level.getBlockEntity(blockPos) instanceof TouristBeaconBlockEntity touristBeaconBlockEntity) {
            return touristBeaconBlockEntity.isOpenForBusiness() ? 15 : 0;
        }
        return 0;
    }

    @Override
    protected boolean hasAnalogOutputSignal(@NonNull BlockState blockState) {
        return true;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NonNull BlockPos blockPos, @NonNull BlockState blockState) {
        return new TouristBeaconBlockEntity(blockPos, blockState);
    }

    @Override
    protected @NonNull InteractionResult useWithoutItem(@NonNull BlockState blockState, Level level, @NonNull BlockPos blockPos, @NonNull Player player, @NonNull BlockHitResult blockHitResult) {
        if (!level.isClientSide()) {
            if (level.dimension() != Level.OVERWORLD) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("Tourism blocks can only be used in the overworld"), true);
                return InteractionResult.FAIL;
            }
            if (level.getBlockEntity(blockPos) instanceof TouristBeaconBlockEntity beaconBlockEntity) {
                player.openMenu(beaconBlockEntity);
            }
        }

        return InteractionResult.SUCCESS;
    }
}
