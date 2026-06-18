package org.bensam.touristry.tourism.experience;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import org.bensam.touristry.entity.TouristEntity;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public interface TouristExperience {
    UUID getUUID();
    BlockPos getBlockPos();

    @Nullable UUID getParentExperienceUUID();
    List<UUID> getChildExperienceUUIDs();
    List<ExperienceTarget> getTargets(ServerLevel serverLevel);

    ExperienceStatistics getStatistics();

    // Lifecycle
    void onTouristArrival(TouristEntity tourist, ServerLevel serverLevel);
    boolean tick(TouristEntity tourist, ServerLevel serverLevel);
    void onTouristDeparture(TouristEntity tourist, ServerLevel serverLevel, boolean completed);

    @Nullable Goal createGoalForTarget(TouristEntity tourist, ExperienceTarget target);
}
