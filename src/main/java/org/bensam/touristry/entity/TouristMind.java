package org.bensam.touristry.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.bensam.touristry.block.entity.TouristBeaconBlockEntity;
import org.bensam.touristry.config.ModServerConfigManager;
import org.bensam.touristry.config.Verbosity;
import org.bensam.touristry.tourism.TourismManager;
import org.bensam.touristry.tourism.TouristLocation;
import org.bensam.touristry.tourism.TouristReview;
import org.bensam.touristry.tourism.VisitResult;
import org.bensam.touristry.tourism.experience.ExperienceTarget;
import org.bensam.touristry.tourism.experience.ExperienceVisit;
import org.bensam.touristry.tourism.experience.TouristExperience;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public final class TouristMind {
    //region Constants
    private static final int BUDGET_MIN_EMERALDS = 3;
    private static final int BUDGET_MEAN_EMERALDS = 7;
    private static final double BUDGET_STD_DEV_EMERALDS = 2.0;
    private static final int CHECK_MOOD_INTERVAL_TICKS = 250;
    private static final double MIN_MOOD = -3.0;
    private static final double MAX_MOOD = 4.0;
    private static final int MIN_ACTIVITY_INTERVAL_TICKS = 500;
    private static final int MAX_ACTIVITY_INTERVAL_TICKS = 2000;
    private static final long MIN_TICKS_BEFORE_MAP_TOGGLE = 20;
    private static final int MIN_WAIT_AFTER_ARRIVAL_TICKS = 40;
    private static final int MAX_WAIT_AFTER_ARRIVAL_TICKS = 80;
    //endregion

    //region Fields
    // Tourist entity-related
    private final TouristEntity tourist;
    private TouristState state;
    private double closestDistanceToDestination;
    private int consecutiveFailedProgressChecks;
    private int dailyBudgetEmeralds;
    private int eveningDespawnTimeTicks;
    private int goodExperiencesToday;
    private boolean isHungry;
    private boolean isStayingOvernight;
    private long lastMapToggleTicks; // (not persisted)
    private double mood;
    private int nextMoodCheckTicks; // (not persisted)
    private int remainingBudgetEmeralds;
    private boolean reportedHurtEnRoute;
    private boolean reportedHurtOnPremises;
    private int waitTicks;

    // Beacon-related
    private BlockPos beaconPos;

    // Experience-related
    private BlockPos experienceBlockPos; // position of the current experience block
    private List<UUID> availableExperienceUUIDs = new ArrayList<>(); // experiences at current beacon (cached on arrival)
    private Set<UUID> visitedExperienceUUIDs = new HashSet<>(); // set of experiences already visited at current beacon
    private int ticksAtCurrentExperience;

    // Experience target-related
    private BlockPos targetPos; // position of the specific target within an experience (only for TRAVELING_TO_EXPERIENCE_TARGET)
    private Deque<ExperienceVisit> experienceTargetTracker = new ArrayDeque<>(); // target tracker for current experience
    private ExperienceTarget currentExperienceTarget; // (not persisted)
    private final List<Goal> injectedExperienceGoals = new ArrayList<>(); // (not persisted)
    private int currentTargetIndex;
    private int ticksAtCurrentTarget;
    //endregion

    public TouristMind(TouristEntity tourist) {
        this.tourist = tourist;
        this.beaconPos = null;
        this.closestDistanceToDestination = Double.MAX_VALUE;
        this.dailyBudgetEmeralds = this.getDailyBudget();
        this.eveningDespawnTimeTicks = this.getRandomDespawnTime();
        this.experienceBlockPos = null;
        this.mood = this.getRandomStartingMood();
        this.nextMoodCheckTicks = this.random().nextInt(CHECK_MOOD_INTERVAL_TICKS);
        this.remainingBudgetEmeralds = this.dailyBudgetEmeralds;
        this.state = TouristState.IDLE;
        this.targetPos = null;
    }

    public void postInitialize() {
        TourismManager.recordTouristBudget(this.dailyBudgetEmeralds);
    }

    private void resetBeaconJourneyStats() {
        this.closestDistanceToDestination = Double.MAX_VALUE;
        this.consecutiveFailedProgressChecks = 0;
        this.reportedHurtEnRoute = false;
        this.reportedHurtOnPremises = false;
    }

    private void resetExperienceJourneyStats() {
        this.closestDistanceToDestination = Double.MAX_VALUE;
        this.consecutiveFailedProgressChecks = 0;
    }

    private void resetDailyStats() {
        this.eveningDespawnTimeTicks = 12000 + this.random().nextInt(1000);
        this.goodExperiencesToday = 0;
        this.isStayingOvernight = false;
        this.lastMapToggleTicks = 0;
        this.mood = this.getRandomStartingMood();
        this.nextMoodCheckTicks = this.random().nextInt(CHECK_MOOD_INTERVAL_TICKS);
        this.reportedHurtEnRoute = false;
        this.reportedHurtOnPremises = false;
        this.ticksAtCurrentTarget = 0;
    }

    /**
     * Called after entity and mind data have been fully loaded.
     * Reconstructs runtime state that cannot be serialized (e.g., Goals).
     *
     * CRITICAL: Must be called from TouristEntity.readAdditionalSaveData()
     * after mind.readAdditionalSaveData() completes.
     */
    public void onEntityLoaded(ServerLevel serverLevel) {
       if (this.state == TouristState.EXPERIENCING_TARGET) {
           this.reconstructExperienceGoals(serverLevel);
       } else if (this.state == TouristState.POSITIONING_AT_TARGET) {
           this.reconstructPositioningGoal(serverLevel);
       }
    }

    private void reconstructExperienceGoals(ServerLevel serverLevel) {
        if (this.experienceTargetTracker.isEmpty()) {
            // Inconsistent state - should not be EXPERIENCING_TARGET with empty stack.
            TouristEntity.logActivity(Verbosity.GAMEPLAY_WARNINGS, "[TouristMind] Load warning: State is EXPERIENCING_TARGET but experience tracker is empty");
            this.transitionTo(TouristState.PLANNING_NEXT_MOVE);
            return;
        }

        if (this.currentExperienceTarget == null) {
            // Inconsistent state - should have a target.
            TouristEntity.logActivity(Verbosity.GAMEPLAY_WARNINGS, "[TouristMind] Load warning: State is EXPERIENCING_TARGET but current experience target is null");
            this.transitionTo(TouristState.CHOOSING_EXPERIENCE_TARGET);
            return;
        }

        ExperienceVisit currentVisit = this.experienceTargetTracker.peekFirst();
        TouristExperience experience = TourismManager.getTouristExperienceById(currentVisit.experienceUUID());

        if (experience == null) {
            // Experience was unloaded / removed - exit gracefully.
            TouristEntity.logActivity(Verbosity.GAMEPLAY_WARNINGS, "[TouristMind] Load warning: Experience {} no longer exists", currentVisit.experienceUUID());
            this.exitCurrentExperience(serverLevel, VisitResult.UNFAVORABLE, false, null);
            return;
        }

        // Recreate and inject the goal for the current target.
        Goal goal = experience.createGoalForTarget(this.tourist, this.currentExperienceTarget);
        if (goal != null) {
            this.injectExperienceGoal(goal);
            TouristEntity.logActivity(Verbosity.LEVEL_1_DIAGNOSTICS, "[TouristMind] Load: Reconstructed experience goal {}", goal.getClass().getSimpleName());
        }
    }

    private void reconstructPositioningGoal(ServerLevel serverLevel) {
        if (this.experienceTargetTracker.isEmpty()) {
            // Inconsistent state - should not be POSITIONING_AT_TARGET with empty stack.
            TouristEntity.logActivity(Verbosity.GAMEPLAY_WARNINGS, "[TouristMind] Load warning: State is POSITIONING_AT_TARGET but experience tracker is empty");
            this.transitionTo(TouristState.PLANNING_NEXT_MOVE);
            return;
        }

        if (this.currentExperienceTarget == null) {
            // Inconsistent state - should have a target.
            TouristEntity.logActivity(Verbosity.GAMEPLAY_WARNINGS, "[TouristMind] Load warning: State is POSITIONING_AT_TARGET but current experience target is null");
            this.transitionTo(TouristState.CHOOSING_EXPERIENCE_TARGET);
            return;
        }

        ExperienceVisit currentVisit = this.experienceTargetTracker.peekFirst();
        TouristExperience experience = TourismManager.getTouristExperienceById(currentVisit.experienceUUID());

        if (experience == null) {
            // Experience was unloaded / removed - exit gracefully.
            TouristEntity.logActivity(Verbosity.GAMEPLAY_WARNINGS, "[TouristMind] Load warning: Experience {} no longer exists", currentVisit.experienceUUID());
            this.exitCurrentExperience(serverLevel, VisitResult.UNFAVORABLE, false, null);
            return;
        }

        // Recreate and inject the positioning goal for the current target.
        int idealDistance = experience.getIdealApproachDistance();
        Goal positioningGoal = new org.bensam.touristry.entity.goal.PositionForViewingGoal(
                this.tourist,
                this.currentExperienceTarget.pos(),
                this.currentExperienceTarget.playerFacing(),
                idealDistance
        );
        this.injectExperienceGoal(positioningGoal);
        TouristEntity.logActivity(Verbosity.LEVEL_1_DIAGNOSTICS, "[TouristMind] Load: Reconstructed PositionForViewingGoal (distance: {})", idealDistance);
    }

    public boolean avoidWater() {
        return this.state == TouristState.WANDERING_AT_BEACON;
    }

    public @Nullable BlockPos getBeaconPos() {
        return this.beaconPos;
    }

    public double getClosestDistanceToDestination() {
        return this.closestDistanceToDestination;
    }

    public int getConsecutiveFailedProgressChecks() {
        return this.consecutiveFailedProgressChecks;
    }

    private int getDailyBudget() {
        double budget = 0;
        for (int i = 1; i <= 20; i++) {
            budget = this.random().nextGaussian() * BUDGET_STD_DEV_EMERALDS;
        }
        return BUDGET_MEAN_EMERALDS + Math.clamp(Math.round(budget), BUDGET_MIN_EMERALDS - BUDGET_MEAN_EMERALDS, Integer.MAX_VALUE);
    }

    public @Nullable BlockPos getExperiencePos() {
        return this.experienceBlockPos;
    }

    public String getLocationNameOrPos() {
        TouristLocation currentLocation = this.state.touristLocation();
        switch (currentLocation) {
            case BEACON -> {
                return TourismManager.getTouristBlockNameOrPos(this.tourist.level(), currentLocation, this.beaconPos).getString();
            }

            case EXPERIENCE -> {
                return TourismManager.getTouristBlockNameOrPos(this.tourist.level(), currentLocation, this.experienceBlockPos).getString();
            }

            default -> {
                return "";
            }
        }
    }

    public int getMaxDistanceAwayFromTarget() {
        if (this.state == TouristState.TRAVELING_TO_EXPERIENCE_TARGET && !this.experienceTargetTracker.isEmpty()) {
            ExperienceVisit currentVisit = this.experienceTargetTracker.peekFirst();
            TouristExperience experience = TourismManager.getTouristExperienceById(currentVisit.experienceUUID());
            if (experience != null) {
                return experience.getMaxApproachDistance();
            }
        }
        return 3;
    }

    public @Nullable BlockPos getMoveToTarget() {
        if (this.state == TouristState.TRAVELING_TO_BEACON) {
            return this.beaconPos;
        } else if (this.state == TouristState.TRAVELING_TO_EXPERIENCE) {
            return this.experienceBlockPos;
        } else if (this.state == TouristState.TRAVELING_TO_EXPERIENCE_TARGET) {
            return this.targetPos;
        }
        return null;
    }

    public String getMoveToTargetName() {
        if (this.state == TouristState.TRAVELING_TO_BEACON) {
            TouristBeaconBlockEntity beaconBlockEntity = TourismManager.getBeaconBlockEntity(this.tourist.level(), this.beaconPos);
            if (beaconBlockEntity != null) {
                return beaconBlockEntity.getPlainTextName();
            }
        } else if (this.state == TouristState.TRAVELING_TO_EXPERIENCE || 
                   this.state == TouristState.TRAVELING_TO_EXPERIENCE_TARGET) {
            TouristExperience experience = TourismManager.getTouristExperienceByPos(this.experienceBlockPos);
            if (experience != null) {
                if (this.state == TouristState.TRAVELING_TO_EXPERIENCE_TARGET && this.targetPos != null) {
                    return experience.getDisplayName().getString() + " target at " + this.targetPos.toShortString();
                }
                return experience.getDisplayName().getString();
            }
        }
        return "";
    }

    public TouristState getState() {
        return this.state;
    }

    public int getTicksAtCurrentTarget() {
        return this.ticksAtCurrentTarget;
    }

    public boolean hasReportedHurtEnRoute() {
        return this.reportedHurtEnRoute;
    }

    public boolean hasReportedHurtOnPremises() {
        return this.reportedHurtOnPremises;
    }

    public void recordExperience(ServerLevel serverLevel, TouristReview review) {
        SoundEvent soundEvent = null;

        switch (review.result()) {
            case ARRIVED, GOOD, GREAT -> soundEvent = SoundEvents.VILLAGER_CELEBRATE;
            case LOST, CLOSED_EARLY, PAYMENT_FAILED, UNFAVORABLE -> soundEvent = SoundEvents.VILLAGER_NO;
            case HURT_EN_ROUTE -> this.reportedHurtEnRoute = true;
            case HURT_ON_PREMISES -> this.reportedHurtOnPremises = true;
        }

        this.tourist.applyExperienceToWorld(serverLevel, review, soundEvent);
    }

    public void recordProgressTowardsTarget(double closestDistanceToTarget, int consecutiveFailedProgressChecks) {
        this.closestDistanceToDestination = closestDistanceToTarget;
        this.consecutiveFailedProgressChecks = consecutiveFailedProgressChecks;
    }

    public void updateMood(VisitResult result) {
        double positiveNormalized = Math.max(0.0, this.mood) / (MAX_MOOD + 1.0);
        double negativeNormalized = Math.max(0.0, -this.mood) / MAX_MOOD;

        double change = switch (result) {
            case ARRIVED, GOOD, GREAT -> {
                this.goodExperiencesToday++;
                VisitResult modifiedResult = this.goodExperiencesToday % 3 == 0 ? VisitResult.GREAT : result;
                yield modifiedResult.moodDelta() * (1.0 - positiveNormalized) * (1.0 + 0.5 * negativeNormalized);
            }
            case UNFAVORABLE, FAILED_SPAWN, LOST, CLOSED_EARLY, PAYMENT_FAILED, HURT_EN_ROUTE, HURT_ON_PREMISES, KILLED_EN_ROUTE, KILLED_ON_PREMISES ->
                    result.moodDelta() * (0.75 + 0.5 * positiveNormalized);
        };

        this.mood = Mth.clamp(this.mood + change, MIN_MOOD, MAX_MOOD);
    }

    public void tick(ServerLevel serverLevel) {
        if (this.state == TouristState.FINISHED || this.state == TouristState.SLEEPING) {
            return;
        }

        // Time-based despawn check
        if (this.isTimeToDespawn()) {
            this.clearBeaconSession();
            this.transitionTo(TouristState.DESPAWNING);
            return;
        }

        // Mood check
        if (this.isTimeToCheckMood() && this.nextMoodCheckTicks <= 0) {
            if (this.isInMoodToDespawn()) {
                this.leaveEarly(serverLevel);
                return;
            }
            this.nextMoodCheckTicks = CHECK_MOOD_INTERVAL_TICKS;
        } else {
            this.nextMoodCheckTicks--;
        }

        // State-specific tick handlers
        if (this.state.isAtExperience()) {
            this.ticksAtCurrentExperience++;
        }

        switch (this.state) {
            case WAIT_AT_BEACON, WANDERING_AT_EXPERIENCE -> {
                if (this.waitTicks <= 0) {
                    this.transitionTo(TouristState.CHOOSING_EXPERIENCE_AT_BEACON);
                } else {
                    this.waitTicks--;
                    if (this.random().nextFloat() < 0.1f) {
                        this.toggleHeldMapWhileWaiting(serverLevel);
                    }
                }
            }

            case WAIT_AT_EXPERIENCE -> {
                if (this.waitTicks <= 0) {
                    this.tourist.clearHeldItem();
                    this.transitionTo(TouristState.ENTERING_EXPERIENCE);
                } else {
                    this.waitTicks--;
                    if (this.random().nextFloat() < 0.1f) {
                        this.toggleHeldMapWhileWaiting(serverLevel);
                    }
                }
            }

            case EXPERIENCING_TARGET -> this.tickExperiencingTarget(serverLevel);

            case WANDERING_AT_BEACON, WANDERING_WORLD -> {
                if (this.waitTicks <= 0) {
                    this.transitionTo(TouristState.PLANNING_NEXT_MOVE);
                } else {
                    this.waitTicks--;
                }
            }

            // Other states don't need per-tick updates.
            default -> {}
        }
    }

    private void tickExperiencingTarget(ServerLevel serverLevel) {
        if (this.experienceTargetTracker.isEmpty()) {
            this.transitionTo(TouristState.PLANNING_NEXT_MOVE);
            return;
        }

        ExperienceVisit currentVisit = this.experienceTargetTracker.peekFirst();
        TouristExperience experience = TourismManager.getTouristExperienceById(currentVisit.experienceUUID());

        if (experience == null) {
            this.exitCurrentExperience(serverLevel, VisitResult.UNFAVORABLE, false, null);
            return;
        }

        if (!experience.isOpenForBusiness()) {
            this.updateMood(VisitResult.CLOSED_EARLY);
            this.recordExperience(serverLevel, new TouristReview(
                    this.state.reviewTarget(),
                    VisitResult.CLOSED_EARLY,
                    true,
                    true,
                    Component.literal("left experience that closed early at"),
                    true,
                    true
            ));
            this.exitCurrentExperience(serverLevel, VisitResult.UNFAVORABLE, false, null);
            return;
        }

        this.ticksAtCurrentTarget++;

        // Call experience tick - returns true when target is complete.
        boolean targetComplete = experience.tickAtTarget(this.tourist, serverLevel, this.currentExperienceTarget);

        if (targetComplete) {
            this.tourist.playSound(SoundEvents.VILLAGER_CELEBRATE);

            // Remove experience-specific goals.
            this.clearInjectedGoals();

            // Mark target as complete.
            this.removeCurrentTarget(serverLevel, true);

            // Choose next activity (or exit if experience is complete).
            this.transitionTo(TouristState.CHOOSING_EXPERIENCE_TARGET);
        }
    }

    private void clearBeaconSession() {
        this.resetBeaconJourneyStats();
        this.availableExperienceUUIDs.clear();
        this.visitedExperienceUUIDs.clear();
        this.experienceBlockPos = null;
        this.targetPos = null;
    }

    private void clearExperienceSession() {
        this.resetExperienceJourneyStats();

        // Exit any active experiences.
        ServerLevel serverLevel = (ServerLevel) this.tourist.level();
        while (!this.experienceTargetTracker.isEmpty()) {
            this.exitCurrentExperience(serverLevel, VisitResult.UNFAVORABLE, false, null);
        }

        this.targetPos = null;
    }

    private void clearInjectedGoals() {
        for (Goal goal : this.injectedExperienceGoals) {
            this.tourist.removeExperienceGoal(goal);
        }
        this.injectedExperienceGoals.clear();
    }

    private int getRandomDespawnTime() {
        return 12000 + this.random().nextInt(1000);
    }

    private double getRandomStartingMood() {
        return 1.0 + this.random().nextDouble();
    }

    private int getRandomWaitTicks(int min, int max) {
        return this.random().nextIntBetweenInclusive(min, max);
    }

    private void injectExperienceGoal(Goal goal) {
        this.tourist.addExperienceGoal(goal);
        this.injectedExperienceGoals.add(goal);
    }

    private boolean isInMoodToDespawn() {
        if (mood >= 0) {
            return false;
        }

        if (mood <= MIN_MOOD) {
            return true;
        }

        return this.random().nextDouble() < (mood / MIN_MOOD);
    }

    private boolean isTimeToCheckMood() {
        long dayTime = this.tourist.level().getDayTime();
        int tickTimeOfDay = (int) (dayTime % 24000L);
        return !this.tourist.isSleeping() && tickTimeOfDay >= 6000;
    }

    private boolean isTimeToDespawn() {
        long dayTime = this.tourist.level().getDayTime();
        int tickTimeOfDay = (int) (dayTime % 24000L);
        return !this.isStayingOvernight && tickTimeOfDay >= this.eveningDespawnTimeTicks;
    }

    private RandomSource random() {
        return this.tourist.getRandom();
    }

    private void recordGoodExperience(ServerLevel serverLevel) {
        Component moodMessage;
        VisitResult result = VisitResult.GOOD;

        this.updateMood(result);

        if (this.mood >= MAX_MOOD) {
            moodMessage = Component.literal("had a great time at");
            result = VisitResult.GREAT;
        } else {
            moodMessage = Component.literal("had a good time at");
        }

        TouristReview review = new TouristReview(
                this.state.reviewTarget(),
                result,
                true,
                false,
                moodMessage,
                true,
                true
        );
        this.recordExperience(serverLevel, review);
    }

    private void toggleHeldMap() {
        if (this.tourist.hasHeldItem()) {
            this.tourist.clearHeldItem();
        } else {
            this.tourist.giveItemToHold(new ItemStack(Items.MAP));
        }
    }

    private void toggleHeldMapWhileWaiting(ServerLevel serverLevel) {
        if (serverLevel.getDayTime() >= (this.lastMapToggleTicks + MIN_TICKS_BEFORE_MAP_TOGGLE)) {
            if (this.tourist.hasHeldItem() && this.waitTicks < 20) {
                return; // don't put away the map near the end of the wait cycle
            }
            this.toggleHeldMap();
            this.lastMapToggleTicks = serverLevel.getDayTime();
        }
    }

    //region State Transition Methods
    private void transitionTo(TouristState newState) {
        if (!(this.tourist.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        // Validate prerequisites before transition.
        TouristBeaconBlockEntity beaconBlockEntity = null;
        String beaconName = "";
        if (newState.requiresBeaconPos()) {
            if (this.beaconPos != null) {
                beaconBlockEntity = TourismManager.getBeaconBlockEntity(serverLevel, this.beaconPos);
            }

            if (beaconBlockEntity == null) {
                TouristEntity.logActivity(Verbosity.GAMEPLAY_WARNINGS, "[TouristMind] Cannot transition to {} - beacon not available", newState);
                this.transitionTo(TouristState.WANDERING_WORLD); // fallback
                return;
            }

            beaconName = beaconBlockEntity.getDisplayName().getString();
        }

        TouristExperience experience = null;
        String experienceName = "";
        if (newState.requiresExperiencePos()) {
            if (this.experienceBlockPos != null) {
                experience = TourismManager.getTouristExperienceByPos(this.experienceBlockPos);
            }

            if (experience == null) {
                TouristEntity.logActivity(Verbosity.GAMEPLAY_WARNINGS, "[TouristMind] Cannot transition to {} - experience block not available", newState);
                this.transitionTo(TouristState.PLANNING_NEXT_MOVE); // fallback
                return;
            }

            experienceName = experience.getDisplayName().getString();
        }

        if (newState == TouristState.TRAVELING_TO_EXPERIENCE_TARGET ||
                newState == TouristState.POSITIONING_AT_TARGET ||
                newState == TouristState.EXPERIENCING_TARGET) {
            if (this.targetPos == null) {
                TouristEntity.logActivity(Verbosity.GAMEPLAY_WARNINGS, "[TouristMind] Cannot transition to {} - target BlockPos not available", newState);
                this.removeCurrentTarget(serverLevel, false);
                this.transitionTo(TouristState.CHOOSING_EXPERIENCE_TARGET); // fallback
                return;
            }
        }

        String logMessageSuffix = switch (newState) {
            case TRAVELING_TO_BEACON, WAIT_AT_BEACON, CHOOSING_EXPERIENCE_AT_BEACON, WANDERING_AT_BEACON -> " " + beaconName;
            case TRAVELING_TO_EXPERIENCE, WAIT_AT_EXPERIENCE, ENTERING_EXPERIENCE, WANDERING_AT_EXPERIENCE -> " " + experienceName;
            case CHOOSING_EXPERIENCE_TARGET, SLEEPING -> " at " + experienceName;
            case TRAVELING_TO_EXPERIENCE_TARGET, POSITIONING_AT_TARGET, EXPERIENCING_TARGET -> " at " + this.targetPos.toShortString();
            default -> "";
        };
        if (this.state == newState) {
            TouristEntity.logActivity(Verbosity.MAJOR_EVENTS, "[TouristMind] Re-entering state {}{}", newState, logMessageSuffix);
        } else {
            TouristEntity.logActivity(Verbosity.MAJOR_EVENTS, "[TouristMind] Transitioning state to {}{}", newState, logMessageSuffix);
        }

        this.state = newState;

        // Call state transition handlers, if applicable.
        switch (newState) {
            case PLANNING_NEXT_MOVE -> this.planNextMove(serverLevel);
            case TRAVELING_TO_BEACON -> this.beginTravelingToBeacon();
            case WAIT_AT_BEACON, WAIT_AT_EXPERIENCE -> this.beginWaitAtBlock();
            case CHOOSING_EXPERIENCE_AT_BEACON -> this.chooseExperienceAtBeacon(serverLevel, beaconBlockEntity);
            case CHOOSING_EXPERIENCE_TARGET -> this.chooseExperienceTarget(serverLevel);
            case TRAVELING_TO_EXPERIENCE -> this.beginTravelingToExperienceBlock();
            case TRAVELING_TO_EXPERIENCE_TARGET -> this.beginTravelingToExperienceTarget();
            case ENTERING_EXPERIENCE -> this.enterExperience(serverLevel);
            case POSITIONING_AT_TARGET -> this.positionAtTarget(serverLevel);
            case WANDERING_WORLD -> this.beginWanderingWorld();
            case WANDERING_AT_BEACON, WANDERING_AT_EXPERIENCE -> this.beginWanderingAtBlock();
            case DESPAWNING, LOST -> this.deSpawn();
            default -> { /* not applicable or transition handled by tick() handler */ }
        }
    }

    public void arriveAtDestination() {
        if (!this.state.isTraveling()) {
            return;
        }

        if (!(this.tourist.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        // Stop navigation and clear held item.
        this.tourist.stopNavigation();
        this.tourist.clearHeldItem();

        // Call the appropriate arrival helper.
        switch (this.state) {
            case TRAVELING_TO_BEACON -> this.arriveAtBeacon(serverLevel);

            case TRAVELING_TO_EXPERIENCE -> this.arriveAtExperience(serverLevel);

            case TRAVELING_TO_EXPERIENCE_TARGET -> this.arriveAtExperienceTarget(serverLevel);
        }
    }

    private void arriveAtBeacon(ServerLevel serverLevel) {
        TouristBeaconBlockEntity beaconBlockEntity = TourismManager.getBeaconBlockEntity(tourist.level(), this.beaconPos);

        if (beaconBlockEntity == null) {
            // No beacon found at beaconPos!
            this.updateMood(VisitResult.LOST);
            Component experienceMessage = Component.literal("did not find a beacon at");
            this.recordExperience(serverLevel, new TouristReview(
                    this.state.reviewTarget(),
                    VisitResult.LOST,
                    true,
                    true,
                    experienceMessage,
                    true,
                    true));
            this.transitionTo(TouristState.PLANNING_NEXT_MOVE);
            return;
        }

        if (beaconBlockEntity.isOpenForBusiness()) {
            this.updateMood(VisitResult.ARRIVED);
            Component experienceMessage = Component.literal("arrived at");
            this.recordExperience(serverLevel, new TouristReview(
                    this.state.reviewTarget(),
                    VisitResult.ARRIVED,
                    false,
                    false,
                    experienceMessage,
                    true,
                    true));
            // Begin short pause before choosing experience.
            this.transitionTo(TouristState.WAIT_AT_BEACON);
        } else {
            this.updateMood(VisitResult.CLOSED_EARLY);
            Component experienceMessage = Component.literal("found beacon closed at");
            this.recordExperience(serverLevel, new TouristReview(
                    this.state.reviewTarget(),
                    VisitResult.CLOSED_EARLY,
                    true,
                    true,
                    experienceMessage,
                    true,
                    true));
            this.transitionTo(TouristState.PLANNING_NEXT_MOVE);
        }
    }

    private void arriveAtExperience(ServerLevel serverLevel) {
        // If already in this experience (stack top matches), skip re-entry.
        if (!this.experienceTargetTracker.isEmpty()) {
            ExperienceVisit topVisit = this.experienceTargetTracker.peekFirst();
            TouristExperience experience = TourismManager.getTouristExperienceByPos(this.experienceBlockPos);

            if (experience != null && experience.getUUID().equals(topVisit.experienceUUID())) {
                // Just transition to activity selection.
                this.transitionTo(TouristState.CHOOSING_EXPERIENCE_TARGET);
                return;
            }
        }

        // New experience - begin short pause before choosing experience target.
        this.ticksAtCurrentExperience = 0;
        this.transitionTo(TouristState.WAIT_AT_EXPERIENCE);
    }

    private void arriveAtExperienceTarget(ServerLevel serverLevel) {
        if (this.experienceTargetTracker.isEmpty() || this.currentExperienceTarget == null) {
            this.transitionTo(TouristState.CHOOSING_EXPERIENCE_TARGET);
            return;
        }

        // Reset tick counter for this target.
        this.ticksAtCurrentTarget = 0;

        // Transition to positioning state (fine-tune position and orientation)
        this.transitionTo(TouristState.POSITIONING_AT_TARGET);
    }

    private void beginTravelingToBeacon() {
        this.clearBeaconSession();
    }

    private void beginTravelingToExperienceBlock() {
        this.clearExperienceSession();
        this.tourist.giveItemToHold(new ItemStack(Items.MAP));
    }

    private void beginTravelingToExperienceTarget() {
        this.resetExperienceJourneyStats();
    }

    private void beginWaitAtBlock() {
        this.lastMapToggleTicks = this.tourist.level().getDayTime();
        this.waitTicks = this.getRandomWaitTicks(MIN_WAIT_AFTER_ARRIVAL_TICKS, MAX_WAIT_AFTER_ARRIVAL_TICKS);
    }

    private void beginWanderingAtBlock() {
        this.tourist.clearHeldItem();
        this.lastMapToggleTicks = this.tourist.level().getDayTime();
        this.waitTicks = this.getRandomWaitTicks(MIN_ACTIVITY_INTERVAL_TICKS, MAX_ACTIVITY_INTERVAL_TICKS);
    }

    private void beginWanderingWorld() {
        this.beaconPos = null;
        this.clearBeaconSession();
        this.tourist.clearHeldItem();
        this.waitTicks = this.getRandomWaitTicks(MIN_ACTIVITY_INTERVAL_TICKS, MAX_ACTIVITY_INTERVAL_TICKS);
    }

    private void chooseExperienceAtBeacon(ServerLevel serverLevel, TouristBeaconBlockEntity beaconBlockEntity) {
        if (this.availableExperienceUUIDs.isEmpty()) {
            List<TouristExperience> experiences = TourismManager.getTouristExperiencesNearBeacon(serverLevel, beaconBlockEntity);

            // Filter by open-for-business and not already visited.
            this.availableExperienceUUIDs = experiences.stream()
                    .filter(TouristExperience::isOpenForBusiness)
                    .map(TouristExperience::getUUID)
                    .collect(Collectors.toList());

            // Shuffle for random order.
            Collections.shuffle(this.availableExperienceUUIDs);
        }

        // Remove already-visited experiences from available list.
        this.availableExperienceUUIDs.removeAll(this.visitedExperienceUUIDs);

        if (this.availableExperienceUUIDs.isEmpty()) {
            // No more experiences to visit at this beacon.
            this.transitionTo(TouristState.PLANNING_NEXT_MOVE);
            return;
        }

        // Choose first available experience.
        UUID nextExperienceUUID = this.availableExperienceUUIDs.getFirst();
        TouristExperience experience = TourismManager.getTouristExperienceById(nextExperienceUUID);

        if (experience == null) {
            // Experience was unloaded/removed - remove from list and try again.
            this.availableExperienceUUIDs.removeFirst();
            this.transitionTo(TouristState.CHOOSING_EXPERIENCE_AT_BEACON);
            return;
        }

        this.experienceBlockPos = experience.getBlockPos().immutable();
        this.transitionTo(TouristState.TRAVELING_TO_EXPERIENCE);
    }

    private void chooseExperienceTarget(ServerLevel serverLevel) {
        if (this.experienceTargetTracker.isEmpty()) {
            // Error state - no experience on stack.
            this.transitionTo(TouristState.PLANNING_NEXT_MOVE);
            return;
        }

        ExperienceVisit currentVisit = this.experienceTargetTracker.peekFirst();
        TouristExperience experience = TourismManager.getTouristExperienceById(currentVisit.experienceUUID());

        if (experience == null) {
            // Experience unloaded - exit gracefully.
            this.exitCurrentExperience(serverLevel, VisitResult.UNFAVORABLE, false, null);
            return;
        }

        // Check if this is a parent experience with child experience as targets.
        List<ExperienceTarget> remainingTargets = currentVisit.remainingTargets();

        if (remainingTargets.isEmpty()) {
            // All targets attempted - exit experience.
            VisitResult result = currentVisit.allTargetsCompleted() ? VisitResult.GOOD : VisitResult.UNFAVORABLE;
            this.exitCurrentExperience(serverLevel, result, true, null);
            return;
        }

        this.currentExperienceTarget = remainingTargets.getFirst();

        // Is this target a child experience?
        if (this.currentExperienceTarget.isChildExperience()) {
            UUID childUUID = this.currentExperienceTarget.childExperienceUUID();
            TouristExperience childExperience = TourismManager.getTouristExperienceById(childUUID);

            if (childExperience != null && childExperience.isOpenForBusiness()) {
                // Navigate to child experience block position.
                this.experienceBlockPos = this.currentExperienceTarget.pos().immutable();
                this.transitionTo(TouristState.TRAVELING_TO_EXPERIENCE);
            } else {
                // Child experience unavailable - skip this target.
                this.removeCurrentTarget(serverLevel, false);
                this.transitionTo(TouristState.CHOOSING_EXPERIENCE_TARGET);
            }

            return;
        }

        // Regular target (block or entity) - navigate to it.
        this.targetPos = this.currentExperienceTarget.pos().immutable();
        this.transitionTo(TouristState.TRAVELING_TO_EXPERIENCE_TARGET);
    }

    private void deSpawn() {
        this.tourist.onDespawn();
        this.transitionTo(TouristState.FINISHED);
    }

    private void enterExperience(ServerLevel serverLevel) {
        TouristExperience experience = TourismManager.getTouristExperienceByPos(this.experienceBlockPos);

        if (experience == null) {
            this.transitionTo(TouristState.CHOOSING_EXPERIENCE_AT_BEACON); // Try next experience.
            return;
        }

        // Mark as visited.
        this.visitedExperienceUUIDs.add(experience.getUUID());

        // Check if the experience is open for business.
        if (!experience.isOpenForBusiness()) {
            this.updateMood(VisitResult.CLOSED_EARLY);
            this.recordExperience(serverLevel, new TouristReview(
                    this.state.reviewTarget(),
                    VisitResult.CLOSED_EARLY,
                    true,
                    true,
                    Component.literal("found experience closed at"),
                    true,
                    true
            ));
            this.transitionTo(TouristState.CHOOSING_EXPERIENCE_AT_BEACON); // Try next experience.
            return;
        }

        // Pay entry fee, if applicable.
        ItemStack entryFee = experience.getEntryFee();
        if (entryFee != null && entryFee.getItem() != Items.AIR) {
            if (!experience.tryDepositPayment(entryFee)) {
                // Payment failed. Assume that we cannot enter this experience.
                this.updateMood(VisitResult.PAYMENT_FAILED);
                this.recordExperience(serverLevel, new TouristReview(
                        this.state.reviewTarget(),
                        VisitResult.PAYMENT_FAILED,
                        true,
                        true,
                        Component.literal("could not pay entry fee for"),
                        true,
                        true
                ));
                this.transitionTo(TouristState.CHOOSING_EXPERIENCE_AT_BEACON); // Try next experience.
                return;
            }
        }

        // Reset experience-specific tracking.
        this.currentTargetIndex = 0;

        // Get experience targets.
        List<ExperienceTarget> targets = experience.getTargets(serverLevel);

        if (targets.isEmpty()) {
            // Experience has no targets. Just wander here briefly.
            this.transitionTo(TouristState.WANDERING_AT_EXPERIENCE);
            return;
        }

        // Call experience lifecycle method.
        experience.onTouristArrival(this.tourist, serverLevel);

        // Create ExperienceVisit and push onto stack.
        ExperienceVisit visit = new ExperienceVisit(
                experience.getUUID(),
                new ArrayList<>(targets),
                0,
                targets.size()
        );
        this.experienceTargetTracker.push(visit);

        // Record arrival.
        this.updateMood(VisitResult.ARRIVED);
        this.recordExperience(serverLevel, new TouristReview(
                this.state.reviewTarget(),
                VisitResult.ARRIVED,
                false,
                false,
                Component.literal("arrived at"),
                true,
                true));

        this.transitionTo(TouristState.CHOOSING_EXPERIENCE_TARGET);
    }

    private void exitCurrentExperience(ServerLevel serverLevel, VisitResult result, boolean completed, @Nullable Component customMessage) {
        if (this.experienceTargetTracker.isEmpty()) {
            this.transitionTo(TouristState.PLANNING_NEXT_MOVE);
            return;
        }

        ExperienceVisit currentVisit = this.experienceTargetTracker.pollFirst();
        TouristExperience experience = TourismManager.getTouristExperienceById(currentVisit.experienceUUID());

        if (experience != null) {
            // Call experience lifecycle method.
            experience.onTouristDeparture(this.tourist, serverLevel, completed);

            // Record experience for tourist.
            this.updateMood(result);
            this.recordExperience(serverLevel, new TouristReview(
                    this.state.reviewTarget(),
                    result,
                    true,
                    false,
                    customMessage != null ? customMessage : Component.literal("is exiting experience"),
                    true,
                    true
            ));
        }

        // Clear experience-specific goals.
        this.clearInjectedGoals();

        // Reset tracking.
        this.currentExperienceTarget = null;
        this.currentTargetIndex = 0;

        // Are we in child experience? If so, return to parent.
        if (!this.experienceTargetTracker.isEmpty()) {
            this.transitionTo(TouristState.CHOOSING_EXPERIENCE_TARGET);
        } else {
            // No parent experience - choose next experience at beacon.
            this.transitionTo(TouristState.CHOOSING_EXPERIENCE_AT_BEACON);
        }
    }

    public void finishPositioning(ServerLevel serverLevel) {
        if (this.experienceTargetTracker.isEmpty() || this.currentExperienceTarget == null) {
            this.transitionTo(TouristState.CHOOSING_EXPERIENCE_TARGET);
            return;
        }

        // Clear positioning goal.
        this.clearInjectedGoals();

        ExperienceVisit currentVisit = this.experienceTargetTracker.peekFirst();
        TouristExperience experience = TourismManager.getTouristExperienceById(currentVisit.experienceUUID());

        if (experience == null) {
            this.exitCurrentExperience(serverLevel, VisitResult.UNFAVORABLE, false, null);
            return;
        }

        // Now inject experience-specific goals.
        Goal experienceGoal = experience.createGoalForTarget(this.tourist, this.currentExperienceTarget);

        if (experienceGoal != null) {
            this.injectExperienceGoal(experienceGoal);
            TouristEntity.logActivity(Verbosity.LEVEL_2_DIAGNOSTICS,
                    "[TouristMind] Injected experience goal: {}", experienceGoal.getClass().getSimpleName());
        }

        this.transitionTo(TouristState.EXPERIENCING_TARGET);
    }

    private void leaveEarly(ServerLevel serverLevel) {
        this.clearBeaconSession();

        Component moodMessage;
        boolean appendTargetName = true;
        if (this.state.isAtTouristLocation()) {
            moodMessage = Component.literal("is in a bad mood and is leaving early from");
        } else {
            moodMessage = Component.literal("is in a bad mood and is leaving early");
            appendTargetName = false;
        }

        // Impl note: applyRatingToTarget is false because rating has likely already been applied.
        TouristReview review = new TouristReview(
                this.state.reviewTarget(),
                VisitResult.UNFAVORABLE,
                false,
                true,
                moodMessage,
                true,
                appendTargetName
        );
        this.recordExperience(serverLevel, review);

        this.transitionTo(TouristState.DESPAWNING);
    }

    private void removeCurrentTarget(ServerLevel serverLevel, boolean markCompleted) {
        if (this.experienceTargetTracker.isEmpty()) {
            return;
        }

        // Pop current visit, update it, push it back.
        ExperienceVisit currentVisit = this.experienceTargetTracker.pollFirst();

        List<ExperienceTarget> remainingTargets = new ArrayList<>(currentVisit.remainingTargets());
        if (!remainingTargets.isEmpty()) {
            remainingTargets.removeFirst(); // remove target
        }

        ExperienceVisit updatedVisit = new ExperienceVisit(
                currentVisit.experienceUUID(),
                remainingTargets,
                markCompleted ? currentVisit.targetsCompleted() + 1 : currentVisit.targetsCompleted(),
                currentVisit.totalTargets()
        );

        this.experienceTargetTracker.push(updatedVisit);
        //this.currentTargetIndex++; (not currently used - completed targets are removed from list)
    }

    public void onForcedDespawn() {
        this.transitionTo(TouristState.DESPAWNING);
    }

    public void onLost() {
        if (!this.state.isTraveling()) {
            return;
        }

        this.tourist.stopNavigation();

        if (!(this.tourist.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (this.state == TouristState.TRAVELING_TO_EXPERIENCE_TARGET) {
            this.updateMood(VisitResult.LOST);
            this.removeCurrentTarget(serverLevel, false);
            this.transitionTo(TouristState.CHOOSING_EXPERIENCE_TARGET);
        } else if (this.state == TouristState.TRAVELING_TO_EXPERIENCE) {
            TouristExperience experience = TourismManager.getTouristExperienceByPos(this.experienceBlockPos);
            Component experienceMessage;
            if (experience == null) {
                experienceMessage = Component.literal("got lost travelling to experience at " + this.experienceBlockPos.toShortString());
            } else {
                String targetInfo = this.state == TouristState.TRAVELING_TO_EXPERIENCE_TARGET && this.targetPos != null
                        ? " target at " + this.targetPos.toShortString() /* TODO: use TouristExperience::getTargetDisplayName */
                        : "";
                experienceMessage = Component.literal("got lost travelling to ")
                        .append(experience.getDisplayName())
                        .append(targetInfo);
            }
            this.exitCurrentExperience(serverLevel, VisitResult.LOST, false, experienceMessage);
        } else {
            this.updateMood(VisitResult.LOST);
            this.recordExperience(serverLevel, new TouristReview(
                    this.state.reviewTarget(),
                    VisitResult.LOST,
                    true,
                    true,
                    Component.literal("got lost travelling to"),
                    true,
                    true));
            this.transitionTo(TouristState.LOST);
        }
    }

    public void onStoppedSleeping(ServerLevel serverLevel) {
        this.resetDailyStats();
        this.isHungry = true;

        int tickTimeOfDay = (int) (serverLevel.getDayTime() % 24000L);
        if (tickTimeOfDay <= 1000 && !this.reportedHurtOnPremises) {
            Component experienceMessage = Component.literal("woke up in a good mood at"); // resetDailyStats() determines how good of a mood
            this.recordExperience(serverLevel, new TouristReview(
                    this.state.reviewTarget(),
                    VisitResult.GOOD,
                    true,
                    false,
                    experienceMessage,
                    true,
                    true));
            this.transitionTo(TouristState.CHOOSING_EXPERIENCE_AT_BEACON);
        } else {
            this.updateMood(VisitResult.UNFAVORABLE);
            Component experienceMessage = Component.literal("woke up abruptly at");
            this.recordExperience(serverLevel, new TouristReview(
                    this.state.reviewTarget(),
                    VisitResult.UNFAVORABLE,
                    true,
                    false,
                    experienceMessage,
                    true,
                    true));
            this.transitionTo(TouristState.DESPAWNING);
        }
    }

    private void planNextMove(ServerLevel serverLevel) {
        List<TouristBeaconBlockEntity> closestBeacons;

        double maxTravelDistanceToNextBeacon = ModServerConfigManager.getConfig().touristEntityConfig().getMaxTravelDistanceToNextBeacon();
        double maxTravelDistanceToNextBeaconSqr = maxTravelDistanceToNextBeacon * maxTravelDistanceToNextBeacon;
        if (this.beaconPos == null) {
            closestBeacons = TourismManager.getTouristBeaconsByDistance(
                    serverLevel,
                    this.tourist.blockPosition(),
                    beaconBlockEntity ->
                            beaconBlockEntity.isOpenForBusiness()
                                    && this.tourist.blockPosition().distSqr(beaconBlockEntity.getBlockPos()) <= maxTravelDistanceToNextBeaconSqr
            );
        } else {
            closestBeacons = TourismManager.getTouristBeaconsByDistance(
                    serverLevel,
                    this.beaconPos,
                    beaconBlockEntity ->
                            !beaconBlockEntity.getBlockPos().equals(this.beaconPos)
                                    && beaconBlockEntity.isOpenForBusiness()
                                    && this.tourist.blockPosition().distSqr(beaconBlockEntity.getBlockPos()) <= maxTravelDistanceToNextBeaconSqr
            );
        }

        int numBeaconsCloseBy = closestBeacons.size();
        if (numBeaconsCloseBy > 0) {
            int beaconIndex = 0;
            if (numBeaconsCloseBy > 1) {
                beaconIndex = this.random().nextInt(numBeaconsCloseBy);
            }

            TouristBeaconBlockEntity beaconBlockEntity = closestBeacons.get(beaconIndex);
            this.beaconPos = beaconBlockEntity.getBlockPos().immutable();
            this.transitionTo(TouristState.TRAVELING_TO_BEACON);
            return;
        }

        this.transitionTo(TouristState.WANDERING_WORLD);
    }

    private void positionAtTarget(ServerLevel serverLevel) {
        if (this.experienceTargetTracker.isEmpty() || this.currentExperienceTarget == null) {
            this.transitionTo(TouristState.CHOOSING_EXPERIENCE_TARGET);
            return;
        }

        ExperienceVisit currentVisit = this.experienceTargetTracker.peekFirst();
        TouristExperience experience = TourismManager.getTouristExperienceById(currentVisit.experienceUUID());

        if (experience == null) {
            this.exitCurrentExperience(serverLevel, VisitResult.UNFAVORABLE, false, null);
            return;
        }

        // Inject positioning goal to fine-tune position and orientation.
        int idealDistance = experience.getIdealApproachDistance();
        Goal positioningGoal = new org.bensam.touristry.entity.goal.PositionForViewingGoal(
                this.tourist,
                this.currentExperienceTarget.pos(),
                this.currentExperienceTarget.playerFacing(),
                idealDistance
        );
        this.injectExperienceGoal(positioningGoal);

        TouristEntity.logActivity(Verbosity.LEVEL_2_DIAGNOSTICS,
                "[TouristMind] Injected PositionForViewingGoal for target at {} (ideal distance: {})",
                this.currentExperienceTarget.pos().toShortString(), idealDistance);
    }

    public void prepareForJourney(@NonNull BlockPos beaconTarget) {
        this.beaconPos = beaconTarget.immutable();
        this.transitionTo(TouristState.TRAVELING_TO_BEACON);
    }
    //endregion

    public void addAdditionalSaveData(ValueOutput valueOutput) {
        valueOutput.store("State", TouristState.CODEC, this.state);

        if (!this.availableExperienceUUIDs.isEmpty()) {
            valueOutput.store("AvailableExperiences", UUIDUtil.CODEC.listOf(), this.availableExperienceUUIDs);
        }

        // Serialize visited experience set of UUIDs as a list.
        if (!this.visitedExperienceUUIDs.isEmpty()) {
            valueOutput.store("VisitedExperiences", UUIDUtil.CODEC.listOf(), List.copyOf(this.visitedExperienceUUIDs));
        }

        // Serialize experience tracker stack as a list (bottom to top order).
        if (!this.experienceTargetTracker.isEmpty()) {
            valueOutput.store("ExperienceTargetTracker", ExperienceVisit.CODEC.listOf(),
                    List.copyOf(this.experienceTargetTracker));
        }
        
        if (this.beaconPos != null) {
            valueOutput.store("BeaconPos", BlockPos.CODEC, this.beaconPos);
        }
        if (this.experienceBlockPos != null) {
            valueOutput.store("ExperienceBlockPos", BlockPos.CODEC, this.experienceBlockPos);
        }
        if (this.targetPos != null) {
            valueOutput.store("TargetPos", BlockPos.CODEC, this.targetPos);
        }

        valueOutput.putDouble("ClosestDistanceToDestination", this.closestDistanceToDestination);
        valueOutput.putInt("FailedProgressChecks", this.consecutiveFailedProgressChecks);
        valueOutput.putBoolean("ReportedHurtEnRoute", this.reportedHurtEnRoute);
        valueOutput.putBoolean("ReportedHurtOnPremises", this.reportedHurtOnPremises);
        valueOutput.putDouble("Mood", this.mood);
        valueOutput.putInt("GoodExperiencesToday", this.goodExperiencesToday);
        valueOutput.putInt("DespawnTimeTicks", this.eveningDespawnTimeTicks);
        valueOutput.putBoolean("IsHungry", this.isHungry);
        valueOutput.putBoolean("IsStayingOvernight", this.isStayingOvernight);
        valueOutput.putInt("CurrentTargetIndex", this.currentTargetIndex);
        valueOutput.putInt("TicksAtCurrentExperience", this.ticksAtCurrentExperience);
        valueOutput.putInt("TicksAtCurrentTarget", this.ticksAtCurrentTarget);
        valueOutput.putInt("WaitTicks", this.waitTicks);
        valueOutput.putInt("DailyBudget", this.dailyBudgetEmeralds);
        valueOutput.putInt("RemainingBudget", this.remainingBudgetEmeralds);
    }

    public void readAdditionalSaveData(ValueInput valueInput) {
        this.beaconPos = valueInput.read("BeaconPos", BlockPos.CODEC).orElse(null);
        this.state = valueInput.read("State", TouristState.CODEC).orElse(
                (this.beaconPos != null ? TouristState.TRAVELING_TO_BEACON : TouristState.IDLE));

        this.availableExperienceUUIDs = new ArrayList<>(
                valueInput.read("AvailableExperiences", UUIDUtil.CODEC.listOf()).orElse(List.of())
        );

        this.visitedExperienceUUIDs = new HashSet<>(
                valueInput.read("VisitedExperiences", UUIDUtil.CODEC.listOf()).orElse(List.of())
        );

        // Deserialize experience tracker (restore as ArrayDeque).
        List<ExperienceVisit> trackerList = valueInput.read("ExperienceTargetTracker", ExperienceVisit.CODEC.listOf())
                .orElse(List.of());
        this.experienceTargetTracker = new ArrayDeque<>(trackerList);
        
        this.experienceBlockPos = valueInput.read("ExperienceBlockPos", BlockPos.CODEC).orElse(null);
        this.targetPos = valueInput.read("TargetPos", BlockPos.CODEC).orElse(null);
        this.closestDistanceToDestination = valueInput.getDoubleOr("ClosestDistanceToDestination", Double.MAX_VALUE);
        this.consecutiveFailedProgressChecks = valueInput.getIntOr("FailedProgressChecks", 0);
        this.reportedHurtEnRoute = valueInput.getBooleanOr("ReportedHurtEnRoute", false);
        this.reportedHurtOnPremises = valueInput.getBooleanOr("ReportedHurtOnPremises", false);
        this.mood = valueInput.getDoubleOr("Mood", this.mood);
        this.goodExperiencesToday = valueInput.getIntOr("GoodExperiencesToday", 0);
        this.eveningDespawnTimeTicks = valueInput.getIntOr("DespawnTimeTicks", this.eveningDespawnTimeTicks);
        this.isHungry = valueInput.getBooleanOr("IsHungry", false);
        this.isStayingOvernight = valueInput.getBooleanOr("IsStayingOvernight", false);
        this.currentTargetIndex = valueInput.getIntOr("CurrentTargetIndex", 0);
        this.ticksAtCurrentExperience = valueInput.getIntOr("TicksAtCurrentExperience", 0);
        this.ticksAtCurrentTarget = valueInput.getIntOr("TicksAtCurrentTarget", 0);
        this.waitTicks = valueInput.getIntOr("WaitTicks", 0);
        this.dailyBudgetEmeralds = valueInput.getIntOr("DailyBudgetEmeralds", this.dailyBudgetEmeralds);
        this.remainingBudgetEmeralds = valueInput.getIntOr("RemainingBudgetEmeralds", this.remainingBudgetEmeralds);

        // Reconstruct currentExperienceTarget from experienceTargetTracker.
        if (!this.experienceTargetTracker.isEmpty()) {
            ExperienceVisit currentVisit = this.experienceTargetTracker.peekFirst();
            List<ExperienceTarget> remaining = currentVisit.remainingTargets();
            if (!remaining.isEmpty()) {
                this.currentExperienceTarget = remaining.getFirst();
            }
        }

        // Run post-initialization steps.
        this.postInitialize();
    }
}
