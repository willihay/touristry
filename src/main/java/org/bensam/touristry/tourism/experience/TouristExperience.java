package org.bensam.touristry.tourism.experience;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import org.bensam.touristry.entity.TouristEntity;
import org.bensam.touristry.tourism.VisitResult;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public interface TouristExperience extends Nameable {
    UUID getUUID();
    BlockPos getBlockPos();
    Component getDefaultName();

    boolean addBlockTarget(ServerLevel serverLevel, BlockPos blockPos, Direction playerFacing);
    boolean addEntityTarget(ServerLevel serverLevel, BlockPos entityPos, Direction playerFacing, UUID entityUUID);
    boolean canSpendBudgetHere(); // return true if there are ways for a tourist to spend their budget here, NOT including cost of beds
    @Nullable Goal createGoalForTarget(TouristEntity tourist, ServerLevel serverLevel, ExperienceTarget target);
    Direction getApproachFromDirection();
    ItemStack getEntryFee();
    int getMaxRangeToTarget();
    TouristLocationStats getStatistics();
    int getTargetIdealApproachDistance();
    int getTargetMaxApproachDistance();
    List<TargetOverlayView> getTargetOverlayViews(ServerLevel serverLevel);
    List<ExperienceTarget> getTargetsForVisit(TouristEntity tourist, ServerLevel serverLevel);
    boolean hasAvailableBeds(); // return true if this experience has available beds where the tourists can spend the night
    boolean hasEntryFee();
    boolean hasTarget(ServerLevel serverLevel, BlockPos blockPos);
    boolean isLodgingExperience();
    boolean isOpenForBusiness();
    ExperienceVisit onLostTravelingToTarget(@NonNull ExperienceVisit visit);
    void onTouristArrival(TouristEntity tourist, ServerLevel serverLevel);
    void onTouristDeparture(TouristEntity tourist, ServerLevel serverLevel, boolean completed);
    ExperienceVisit prepareToLeaveEarly(@NonNull ExperienceVisit visit);
    void rateVisit(VisitResult result, long currentTimeTicks);
    boolean removeTarget(ServerLevel serverLevel, BlockPos pos);
    boolean removeEntityTargetById(ServerLevel serverLevel, UUID entityUUID);
    boolean tryDepositPayment(ItemStack itemStack);
}
