package org.bensam.touristry.tourism.experience;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
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
    int getIdealApproachDistance();
    int getMaxApproachDistance();
    int getMaxRangeToTarget();
    List<ExperienceTarget> getTargets(ServerLevel serverLevel);
    boolean hasTarget(BlockPos blockPos);
    boolean isOpenForBusiness();
    boolean isTargetValid(ServerLevel serverLevel, ExperienceTarget target);
    boolean removeTarget(ServerLevel serverLevel, BlockPos pos);
    boolean removeEntityTargetById(ServerLevel serverLevel, UUID entityUUID);

    TouristLocationStats getStatistics();
    void rateVisit(VisitResult result, long currentTimeTicks);

    // Lifecycle
    void onTouristArrival(TouristEntity tourist, ServerLevel serverLevel);
    boolean tickAtTarget(TouristEntity tourist, ServerLevel serverLevel, ExperienceTarget target);
    void onTouristDeparture(TouristEntity tourist, ServerLevel serverLevel, boolean completed);

    @Nullable Goal createGoalForTarget(TouristEntity tourist, ExperienceTarget target);
}
