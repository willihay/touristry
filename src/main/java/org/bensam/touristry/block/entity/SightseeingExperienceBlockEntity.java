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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.bensam.touristry.ModBlockEntities;
import org.bensam.touristry.Touristry;
import org.bensam.touristry.entity.TouristEntity;
import org.bensam.touristry.entity.goal.LookAtTargetPosGoal;
import org.bensam.touristry.menu.SightseeingExperienceMenu;
import org.bensam.touristry.tourism.experience.ExperienceTarget;
import org.jspecify.annotations.Nullable;

public class SightseeingExperienceBlockEntity extends AbstractExperienceBlockEntity {
    public static final int IDEAL_APPROACH_DISTANCE = 2; // Tourist should try to stand this far away for sightseeing targets
    public static final int MAX_APPROACH_DISTANCE = 6; // Skip target if tourist can't get closer than this distance
    public static final int MAX_RANGE_TO_TARGET = 100;
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
    public boolean tickAtTarget(TouristEntity tourist, ServerLevel serverLevel, ExperienceTarget target) {
        if (target == null) {
            return true;
        }

        // Calculate duration modifiers as needed.
        int durationModifier = 0;
        if (target.isBlock()) {
            BlockEntity blockEntity = this.level.getBlockEntity(target.pos());
            // Extend duration of lectern targets that have books by the number of written pages in the book.
            if (blockEntity instanceof LecternBlockEntity lectern) {
                ItemStack book = lectern.getBook();
                if (!book.isEmpty() && book.has(net.minecraft.core.component.DataComponents.WRITTEN_BOOK_CONTENT)) {
                    WrittenBookContent content = book.get(net.minecraft.core.component.DataComponents.WRITTEN_BOOK_CONTENT);
                    if (content != null) {
                        durationModifier = Math.min(content.pages().size() * 10, 160); // extend duration by 10 ticks per written page (max 160)
                    }
                }
            }
        } else if (target.isEntity()) {
            Entity entity = serverLevel.getEntity(target.entityUUID());
            // Extend duration of painting targets by the size of the painting.
            if (entity instanceof Painting painting) {
                int area = painting.getVariant().value().area();
                durationModifier = area * 5; // extend duration by 5 ticks per sq. unit of area
            }
        }

        return tourist.getTicksAtCurrentTarget() >= (160 + durationModifier); // minimum 8 game seconds at a sightseeing target
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

    public Component getTargetDisplayName(ExperienceTarget target) {
        BlockEntity blockEntity = this.level.getBlockEntity(target.pos());
        if (!(blockEntity instanceof LecternBlockEntity lectern)) {
            return Component.literal("Unknown");
        }

        // Priority 1: Check if the lectern block itself has a custom name
        ItemStack lecternItem = new ItemStack(blockEntity.getBlockState().getBlock());
        lecternItem.applyComponents(blockEntity.collectComponents());
        if (lecternItem.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME)) {
            return lecternItem.getHoverName().copy();
        }

        // Priority 2: Check if lectern has a book with a custom name
        ItemStack book = lectern.getBook();
        if (!book.isEmpty() && book.has(net.minecraft.core.component.DataComponents.WRITTEN_BOOK_CONTENT)) {
            WrittenBookContent content = book.get(net.minecraft.core.component.DataComponents.WRITTEN_BOOK_CONTENT);
            if (content != null) {
                return Component.literal(content.title().get(true));
            }
        }

        // Priority 3: Use translated block name (e.g., "Lectern")
        return blockEntity.getBlockState().getBlock().getName();
    }

    private boolean isSightseeingBlock(BlockState blockState) {
        return blockState.is(Blocks.LECTERN);
    }

    private boolean isSightseeingEntity(Entity entity) {
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
            return entity != null && this.isSightseeingEntity(entity);
        }

        // Check if block still exists and is valid for sightseeing.
        BlockState blockState = serverLevel.getBlockState(target.pos());
        return !blockState.isAir() && this.isSightseeingBlock(blockState);
    }
}
