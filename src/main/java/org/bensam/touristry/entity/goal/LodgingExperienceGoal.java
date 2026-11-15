package org.bensam.touristry.entity.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.bensam.touristry.block.entity.LodgingExperienceBlockEntity;
import org.bensam.touristry.config.Verbosity;
import org.bensam.touristry.entity.TouristEntity;
import org.bensam.touristry.tourism.VisitResult;
import org.bensam.touristry.tourism.experience.ExperienceVisit;

public class LodgingExperienceGoal extends LookAtTargetPosGoal {
    private final GoalType goalType;
    private final TouristEntity tourist;
    private final BlockPos targetPos;
    private final int durationTicks;
    private final int minimumTimeAtTarget;

    private int tickCount;
    private boolean startedSleeping;

    public LodgingExperienceGoal(TouristEntity tourist, GoalType goalType, BlockPos targetPos, int startingTickCount, int timeAtTarget) {
        super(tourist, targetPos, false, true);
        this.goalType = goalType;
        this.tourist = tourist;
        this.targetPos = targetPos;
        this.tickCount = this.adjustedTickDelay(startingTickCount);
        this.startedSleeping = this.tourist.isSleeping();
        this.minimumTimeAtTarget = this.startedSleeping ? this.tickCount : this.adjustedTickDelay(timeAtTarget);
        this.durationTicks = Math.max(0, timeAtTarget - startingTickCount);
    }

    @Override
    public void start() {
        super.start();

        ExperienceVisit visit = this.tourist.getMind().getExperienceVisit();
        String targetName = "target";

        if (visit != null && !visit.remainingTargets().isEmpty()) {
            targetName = visit.remainingTargets().getFirst().getDisplayName(getServerLevel(this.tourist)).getString();
        }

        if (this.goalType == GoalType.SLEEP) {
            if (this.startedSleeping) {
                TouristEntity.logActivity(Verbosity.LEVEL_2_DIAGNOSTICS, "[{}] Sleeping in {} at {}",
                        this.getClass().getSimpleName(),
                        targetName,
                        this.targetPos.toShortString());
            } else {
                TouristEntity.logActivity(Verbosity.LEVEL_2_DIAGNOSTICS, "[{}] Sleeping in {} at {} after looking around for {} ticks",
                        this.getClass().getSimpleName(),
                        targetName,
                        this.targetPos.toShortString(),
                        this.durationTicks);
            }
        } else if (this.goalType == GoalType.PAY) {
            TouristEntity.logActivity(Verbosity.LEVEL_2_DIAGNOSTICS, "[{}] Paying for stay at {} for {} ticks",
                    this.getClass().getSimpleName(),
                    targetName,
                    this.durationTicks);
        } else {
            TouristEntity.logActivity(Verbosity.GAMEPLAY_WARNINGS, "[{}] Unexpected goal type: {}",
                    this.getClass().getSimpleName(),
                    this.goalType);
            this.tickCount = this.minimumTimeAtTarget;
        }
    }

    @Override
    public void tick() {
        super.tick();
        this.tickCount++;
        ServerLevel serverLevel = getServerLevel(this.tourist);

        if (this.tickCount >= this.minimumTimeAtTarget) {
            if (this.goalType == GoalType.SLEEP) {
                if (!this.tourist.isSleeping()) {
                    if (!this.startedSleeping) {
                        this.tourist.startSleeping(this.targetPos);
                        this.tourist.getMind().setSleepTime(serverLevel.getDayTime());
                        this.startedSleeping = true;
                    } else {
                        this.tourist.getMind().finishTargetGoal();
                    }
                }
            } else if (this.goalType == GoalType.PAY) {
                this.payForStay(serverLevel);
                this.tourist.getMind().finishTargetGoal();
            }
        }
    }

    private void payForStay(ServerLevel serverLevel) {
        boolean paymentCompleted = false;
        if (serverLevel.getBlockEntity(this.targetPos) instanceof LodgingExperienceBlockEntity lodgingBlockEntity) {
            ItemStack cost = lodgingBlockEntity.getDefaultCost();
            if (lodgingBlockEntity.tryDepositPayment(cost)) {
                paymentCompleted = true; // note: lodging cost does not come out of tourist's daily spending budget
            }
        }

        if (paymentCompleted) {
            this.tourist.getMind().updateExperienceVisitResult(VisitResult.GOOD);
        } else {
            this.tourist.getMind().updateExperienceVisitResult(VisitResult.PAYMENT_FAILED);
        }
    }

}
