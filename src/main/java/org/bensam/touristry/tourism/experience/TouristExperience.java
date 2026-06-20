package org.bensam.touristry.tourism.experience;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import org.bensam.touristry.entity.TouristEntity;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public interface TouristExperience {
    UUID getUUID();
    BlockPos getBlockPos();
    Component getDisplayName();

    @Nullable UUID getParentExperienceUUID();
    List<UUID> getChildExperienceUUIDs();

    boolean addTarget(ServerLevel serverLevel, BlockPos blockPos, Direction playerFacing, UUID childUUID);
    int getMaxDistanceToTarget();
    List<ExperienceTarget> getTargets(ServerLevel serverLevel);
    boolean isTargetValid(ServerLevel serverLevel, ExperienceTarget target);
    void removeTarget(ServerLevel serverLevel, BlockPos pos);

    ExperienceStatistics getStatistics();

    // Lifecycle
    void onTouristArrival(TouristEntity tourist, ServerLevel serverLevel);
    boolean tick(TouristEntity tourist, ServerLevel serverLevel);
    void onTouristDeparture(TouristEntity tourist, ServerLevel serverLevel, boolean completed);

    @Nullable Goal createGoalForTarget(TouristEntity tourist, ExperienceTarget target);
}
