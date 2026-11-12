package org.bensam.touristry.entity.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.bensam.touristry.block.entity.LodgingExperienceBlockEntity;
import org.bensam.touristry.config.Verbosity;
import org.bensam.touristry.entity.TouristEntity;
import org.bensam.touristry.tourism.TouristEconomy;
import org.bensam.touristry.tourism.VisitResult;
import org.bensam.touristry.tourism.experience.ExperienceVisit;

public class LodgingExperienceGoal extends LookAtTargetPosGoal {
    private final GoalType goalType;
    private final TouristEntity tourist;
    private final BlockPos targetPos;
    private final int durationTicks;
    private final int adjustedTimeAtTarget;

    private int tickCount;

    public LodgingExperienceGoal(TouristEntity tourist, GoalType goalType, BlockPos targetPos, int startingTickCount, int timeAtTarget) {
        super(tourist, targetPos, false, true);
        this.goalType = goalType;
        this.tourist = tourist;
        this.targetPos = targetPos;
        this.tickCount = this.adjustedTickDelay(startingTickCount);
        this.adjustedTimeAtTarget = this.adjustedTickDelay(timeAtTarget);
        this.durationTicks = Math.max(0, timeAtTarget - startingTickCount);
    }

    @Override
    public void start() {
        super.start();

        ExperienceVisit visit = this.tourist.getMind().getExperienceVisit();
        float allowance = 0;
        if (visit != null) {
            allowance = visit.budgetRemaining();
        }

        if (this.goalType == GoalType.SLEEP) {
            TouristEntity.logActivity(Verbosity.LEVEL_2_DIAGNOSTICS, "[{}] Sleeping in bed at {} after looking around for {} ticks",
                    this.getClass().getSimpleName(),
                    this.targetPos.toShortString(),
                    this.durationTicks);
        } else if (this.goalType == GoalType.PAY) {
            TouristEntity.logActivity(Verbosity.LEVEL_2_DIAGNOSTICS, "[{}] Paying for stay with budget of {} for {} ticks",
                    this.getClass().getSimpleName(),
                    allowance,
                    this.durationTicks);
        } else {
            TouristEntity.logActivity(Verbosity.GAMEPLAY_WARNINGS, "[{}] Unknown goal type: {}",
                    this.getClass().getSimpleName(),
                    this.goalType);
            this.tickCount = this.adjustedTimeAtTarget;
        }
    }

    @Override
    public void tick() {
        super.tick();
        this.tickCount++;
        ServerLevel serverLevel = getServerLevel(this.tourist);

        if (this.tickCount >= this.adjustedTimeAtTarget) {
            if (this.goalType == GoalType.SLEEP) {
                this.tourist.startSleeping(this.targetPos);
            } else if (this.goalType == GoalType.PAY) {
                this.payForStay(serverLevel);
            }

            // Mark finished at this target.
            this.tourist.getMind().finishTargetGoal();
        }
    }

    private void payForStay(ServerLevel serverLevel) {
        boolean paymentCompleted = false;
        if (serverLevel.getBlockEntity(this.targetPos) instanceof LodgingExperienceBlockEntity lodgingBlockEntity) {
            ItemStack cost = lodgingBlockEntity.getDefaultCost();
            if (lodgingBlockEntity.tryDepositPayment(cost)) {
                float itemValue = (int) TouristEconomy.getEmeraldEquivalent(cost);
                this.tourist.getMind().spendBudget(itemValue);
                paymentCompleted = true;
            }
        }

        if (paymentCompleted) {
            this.tourist.getMind().updateExperienceVisitResult(VisitResult.GOOD);
        } else {
            this.tourist.getMind().updateExperienceVisitResult(VisitResult.PAYMENT_FAILED);
        }
    }

}
