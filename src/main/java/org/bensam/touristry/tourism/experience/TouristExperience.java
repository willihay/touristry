package org.bensam.touristry.tourism.experience;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import org.bensam.touristry.entity.TouristEntity;
import org.bensam.touristry.tourism.VisitResult;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public interface TouristExperience {
    UUID getUUID();
    BlockPos getBlockPos();
    Component getDisplayName();

    @Nullable UUID getParentExperienceUUID();
    List<UUID> getChildExperienceUUIDs();

    boolean addBlockTarget(ServerLevel serverLevel, BlockPos blockPos, Direction playerFacing);
    boolean addChildExperienceTarget(ServerLevel serverLevel, BlockPos blockPos, Direction playerFacing, UUID childUUID);
    boolean addEntityTarget(ServerLevel serverLevel, BlockPos entityPos, Direction playerFacing, UUID entityUUID);
    boolean canSpendBudgetHere(); // return true if there are ways for a tourist to spend their budget here, NOT including cost of beds or child experiences
    ItemStack getEntryFee();
    int getIdealApproachDistance();
    int getMaxApproachDistance();
    int getMaxRangeToTarget();
    List<ExperienceTarget> getTargets(ServerLevel serverLevel);
    List<TargetOverlayView> getTargetOverlayViews(ServerLevel serverLevel);
    boolean hasBeds(); // return true if this experience (not including child experiences) has beds where the tourists can spend the night (does not guarantee availability of beds)
    boolean hasEntryFee();
    boolean hasTarget(BlockPos blockPos);
    boolean isOpenForBusiness();
    boolean removeTarget(ServerLevel serverLevel, BlockPos pos);
    boolean removeEntityTargetById(ServerLevel serverLevel, UUID entityUUID);
    boolean tryDepositPayment(ItemStack itemStack);

    TouristLocationStats getStatistics();
    void rateVisit(VisitResult result, long currentTimeTicks);

    // Lifecycle
    void onTouristArrival(TouristEntity tourist, ServerLevel serverLevel);
    void onTouristDeparture(TouristEntity tourist, ServerLevel serverLevel, boolean completed);
    @Nullable Goal createGoalForTarget(TouristEntity tourist, ServerLevel serverLevel, ExperienceTarget target);
}
