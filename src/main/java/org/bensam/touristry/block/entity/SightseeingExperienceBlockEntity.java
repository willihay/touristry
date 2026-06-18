package org.bensam.touristry.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.bensam.touristry.ModBlockEntities;
import org.bensam.touristry.Touristry;
import org.bensam.touristry.entity.TouristEntity;
import org.bensam.touristry.entity.goal.LookAtTargetPosGoal;
import org.bensam.touristry.menu.SightseeingExperienceMenu;
import org.bensam.touristry.tourism.TourismManager;
import org.bensam.touristry.tourism.experience.ExperienceTarget;
import org.jspecify.annotations.Nullable;

public class SightseeingExperienceBlockEntity extends AbstractExperienceBlockEntity {
    public static final int PAYMENT_SLOT_SIZE = 9;
    public static final int TOTAL_INVENTORY_SIZE = PAYMENT_SLOT_SIZE + 1;

    public SightseeingExperienceBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(ModBlockEntities.SIGHTSEEING_EXPERIENCE.get(), blockPos, blockState, TOTAL_INVENTORY_SIZE);
    }

    // Lifecycle
    @Override
    public void onTouristArrival(TouristEntity tourist, ServerLevel serverLevel) {

    }

    @Override
    public boolean tick(TouristEntity tourist, ServerLevel serverLevel) {
        return tourist.getTicksAtCurrentTarget() >= 200; // 10 game seconds
    }

    @Override
    public void onTouristDeparture(TouristEntity tourist, ServerLevel serverLevel, boolean completed) {

    }

    // Helpers
    @Override
    public @Nullable Goal createGoalForTarget(TouristEntity tourist, ExperienceTarget target) {
        if (target.isChildExperience()) {
            return null; // just navigate to sub-experience
        }

        return new LookAtTargetPosGoal(tourist, target.pos(), target.playerFacing());
    }

    @Override
    protected AbstractContainerMenu createMenu(int i, Inventory inventory) {
        return new SightseeingExperienceMenu(i, inventory, this, this.data, ContainerLevelAccess.create(this.level, this.getBlockPos()));
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block." + Touristry.MOD_ID + ".sightseeing_experience");
    }

    @Override
    protected int getExperienceKeySlotIndex() {
        return PAYMENT_SLOT_SIZE;
    }

    @Override
    public int getPaymentSlotSize() {
        return PAYMENT_SLOT_SIZE;
    }

    private boolean isSightseeingTarget(BlockState blockState) {
        // TODO: support paintings and item frames (which are entities, not blocks).
        return blockState.is(Blocks.LECTERN);
    }

    @Override
    protected boolean isTargetValid(ServerLevel serverLevel, ExperienceTarget target) {
        if (target.isChildExperience()) {
            return TourismManager.getExperienceByUUID(target.childExperienceUUID()) != null;
        }

        // Check if block still exists and is valid for sightseeing.
        BlockState blockState = serverLevel.getBlockState(target.pos());
        return !blockState.isAir() && this.isSightseeingTarget(blockState);
    }
}
