package org.bensam.touristry.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
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
import org.bensam.touristry.entity.goal.SightseeingExperienceGoal;
import org.bensam.touristry.menu.SightseeingExperienceMenu;
import org.bensam.touristry.tourism.experience.ExperienceTarget;
import org.jspecify.annotations.Nullable;

public class SightseeingExperienceBlockEntity extends AbstractExperienceBlockEntity {
    public static final int IDEAL_TARGET_APPROACH_DISTANCE = 2; // Tourist should try to stand this far away for sightseeing targets
    public static final int MAX_APPROACH_DISTANCE = 6; // Skip target if tourist can't get closer than this distance
    public static final int MAX_RANGE_TO_TARGET = 100;
    public static final int MIN_TICKS_AT_TARGET = 100;
    public static final int PAYMENT_SLOT_SIZE = 9;
    public static final int TARGET_KEY_INDEX = PAYMENT_SLOT_SIZE;
    public static final int ENTRY_FEE_INDEX = TARGET_KEY_INDEX + 1;
    public static final int TOTAL_INVENTORY_SIZE = PAYMENT_SLOT_SIZE + 2;

    public SightseeingExperienceBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(ModBlockEntities.SIGHTSEEING_EXPERIENCE.get(), blockPos, blockState, TOTAL_INVENTORY_SIZE);
    }

    @Override
    public @Nullable Goal createGoalForTarget(TouristEntity tourist, ServerLevel serverLevel, ExperienceTarget target) {
        // Calculate duration modifier as needed.
        int durationModifier = 0;
        if (target.isBlock()) {
            BlockEntity blockEntity = serverLevel.getBlockEntity(target.pos());
            // Extend duration of lectern targets that have books by the number of written pages in the book.
            if (blockEntity instanceof LecternBlockEntity lectern) {
                ItemStack book = lectern.getBook();
                if (!book.isEmpty() && book.has(DataComponents.WRITTEN_BOOK_CONTENT)) {
                    WrittenBookContent content = book.get(DataComponents.WRITTEN_BOOK_CONTENT);
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
                durationModifier = area * 5; // extend duration by 5 ticks per unit of area
            }
        }

        return new SightseeingExperienceGoal(
                tourist,
                target.pos(),
                tourist.getTicksAtCurrentTarget(),
                MIN_TICKS_AT_TARGET + durationModifier);
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
    public ItemStack getEntryFee() {
        return this.inventory.get(ENTRY_FEE_INDEX).copy();
    }

    @Override
    public int getIdealApproachDistance() {
        return IDEAL_TARGET_APPROACH_DISTANCE;
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

    @Override
    public boolean hasEntryFee() {
        return !this.inventory.get(ENTRY_FEE_INDEX).isEmpty();
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
    protected boolean isTargetValid(ServerLevel serverLevel, ExperienceTarget target) {
        // Check entity targets (paintings, item frames).
        if (target.isEntity()) {
            Entity entity = serverLevel.getEntity(target.entityUUID());
            return this.isSightseeingEntity(entity);
        }

        // Check if block still exists and is valid for sightseeing.
        BlockState blockState = serverLevel.getBlockState(target.pos());
        return !blockState.isAir() && this.isSightseeingBlock(blockState);
    }
}
