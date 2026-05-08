package org.bensam.touristry.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.bensam.touristry.Touristry;
import org.bensam.touristry.block.entity.TouristBeaconBlockEntity;
import org.bensam.touristry.menu.TouristBeaconMenu;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class TouristBeaconBlock extends BaseEntityBlock {
    public static final MapCodec<TouristBeaconBlock> CODEC = simpleCodec(TouristBeaconBlock::new);
    private static final Component CONTAINER_TITLE = Component.translatable("container." + Touristry.MOD_ID + ".tourist_beacon.title");

	public TouristBeaconBlock(Properties properties)
	{
        super(properties);
    }

    @Override
    protected @NonNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NonNull BlockPos blockPos, @NonNull BlockState blockState) {
        return new TouristBeaconBlockEntity(blockPos, blockState);
    }

    @Override
    protected @NonNull InteractionResult useWithoutItem(@NonNull BlockState blockState, Level level, @NonNull BlockPos blockPos, @NonNull Player player, @NonNull BlockHitResult blockHitResult) {
        if (!level.isClientSide()) {
            player.openMenu(blockState.getMenuProvider(level, blockPos));
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    protected @Nullable MenuProvider getMenuProvider(@NonNull BlockState blockState, @NonNull Level level, @NonNull BlockPos blockPos) {
        if (level.getBlockEntity(blockPos) instanceof TouristBeaconBlockEntity touristBeaconBlockEntity) {
            return new SimpleMenuProvider(
                    (containerId, inventory, player) -> new TouristBeaconMenu(
                            containerId,
                            inventory,
                            touristBeaconBlockEntity,
                            ContainerLevelAccess.create(level, blockPos)
                    ),
                    CONTAINER_TITLE
            );
        }

        return null;
    }
}
