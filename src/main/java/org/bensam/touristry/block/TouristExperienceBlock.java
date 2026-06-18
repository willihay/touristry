package org.bensam.touristry.block;

import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public abstract class TouristExperienceBlock extends BaseEntityBlock {
    public static final BooleanProperty OPEN_FOR_BUSINESS = BooleanProperty.create("open_for_business");

    public TouristExperienceBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(OPEN_FOR_BUSINESS, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OPEN_FOR_BUSINESS);
    }
}
