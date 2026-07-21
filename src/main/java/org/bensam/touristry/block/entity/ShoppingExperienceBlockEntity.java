package org.bensam.touristry.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ShelfBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.bensam.touristry.ModBlockEntities;
import org.bensam.touristry.Touristry;
import org.bensam.touristry.entity.TouristEntity;
import org.bensam.touristry.entity.goal.LookAtTargetPosGoal;
import org.bensam.touristry.menu.ShoppingExperienceMenu;
import org.bensam.touristry.tourism.experience.ExperienceTarget;
import org.jspecify.annotations.Nullable;

public class ShoppingExperienceBlockEntity extends AbstractExperienceBlockEntity {
    public static final int IDEAL_APPROACH_DISTANCE = 2; // Tourist should try to stand this far away for shopping targets
    public static final int MAX_APPROACH_DISTANCE = 4; // Skip target if tourist can't get closer than this distance
    public static final int MAX_RANGE_TO_TARGET = 100;
    public static final int PAYMENT_SLOT_SIZE = 9;
    public static final int TOTAL_INVENTORY_SIZE = PAYMENT_SLOT_SIZE + 2;

    public ShoppingExperienceBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(ModBlockEntities.SHOPPING_EXPERIENCE.get(), blockPos, blockState, TOTAL_INVENTORY_SIZE);
    }

    // Lifecycle
    @Override
    public void onTouristArrival(TouristEntity tourist, ServerLevel serverLevel) {

    }

    @Override
    public boolean tickAtTarget(TouristEntity tourist, ServerLevel serverLevel, ExperienceTarget target) {
        if (target == null) {
            return true;
        }

        return tourist.getTicksAtCurrentTarget() >= 100; // minimum 5 game seconds at a shopping target
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

        return new LookAtTargetPosGoal(tourist, target.pos());
    }

    @Override
    protected AbstractContainerMenu createMenu(int i, Inventory inventory) {
        return new ShoppingExperienceMenu(i, inventory, this, this.data, ContainerLevelAccess.create(this.level, this.getBlockPos()));
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block." + Touristry.MOD_ID + ".shopping_experience");
    }

    @Override
    public int getIdealApproachDistance() {
        return IDEAL_APPROACH_DISTANCE;
    }

    @Override
    public int getMaxApproachDistance() {
        return MAX_APPROACH_DISTANCE;
    }

    @Override
    public int getMaxRangeToTarget() {
        return MAX_RANGE_TO_TARGET;
    }

    @Override
    public int getPaymentSlotSize() {
        return PAYMENT_SLOT_SIZE;
    }

    @Override
    protected int getTargetKeySlotIndex() {
        return PAYMENT_SLOT_SIZE;
    }

    private boolean isShoppingBlock(BlockEntity blockEntity) {
        return blockEntity instanceof BarrelBlockEntity ||
                blockEntity instanceof ChestBlockEntity ||
                blockEntity instanceof ShelfBlockEntity;
    }

    private boolean isShoppingEntity(Entity entity) {
        return entity instanceof Painting ||
                entity instanceof ItemFrame ||
                entity instanceof GlowItemFrame;
    }

    @Override
    public boolean isTargetValid(ServerLevel serverLevel, ExperienceTarget target) {
        // Check child experiences.
        if (target.isChildExperience()) {
            return this.isTargetChildExperienceValid(target.childExperienceUUID());
        }

        // Check entity targets (paintings, item frames).
        if (target.isEntity()) {
            Entity entity = serverLevel.getEntity(target.entityUUID());
            return entity != null && this.isShoppingEntity(entity);
        }

        // Check if block still exists and is valid for shopping.
        BlockEntity blockEntity = serverLevel.getBlockEntity(target.pos());
        BlockState blockState = serverLevel.getBlockState(target.pos());
        return !blockState.isAir() && this.isShoppingBlock(blockEntity);
    }
}
