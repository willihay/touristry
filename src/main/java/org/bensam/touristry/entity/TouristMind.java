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
    private static final double MIN_MOOD = -3.0;
    private static final double MAX_MOOD = 4.0;
    private static final int MIN_ACTIVITY_INTERVAL_TICKS = 500;
    private static final int MAX_ACTIVITY_INTERVAL_TICKS = 2000;
    private static final int CHECK_MOOD_INTERVAL_TICKS = 250;

    private final TouristEntity tourist;
    private final List<Goal> injectedExperienceGoals = new ArrayList<>();
    private ExperienceTarget currentExperienceTarget;

    // persisted fields
    private TouristState state;
    private List<UUID> availableExperienceUUIDs = new ArrayList<>(); // experiences at current beacon (cached on arrival)
    private Set<UUID> visitedExperienceUUIDs = new HashSet<>(); // set of experiences already visited at current beacon
    private Deque<ExperienceVisit> experienceTracker = new ArrayDeque<>(); // target tracker for current experience
    private BlockPos beaconPos;
    private BlockPos experiencePos;
    private double closestDistanceToDestination;
    private int consecutiveFailedProgressChecks;
    private boolean reportedHurtEnRoute;
    private boolean reportedHurtOnPremises;
    private double mood;
    private int goodExperiencesToday;
    private int eveningDespawnTimeTicks;
    private boolean isHungry;
    private boolean isStayingOvernight;
    private int currentTargetIndex;
    private int ticksAtCurrentExperience;
    private int ticksAtCurrentTarget;

    private int nextChooseActivityTicks;
    private int nextMoodCheckTicks;

    public TouristMind(TouristEntity tourist) {
        this.tourist = tourist;
        this.state = TouristState.IDLE;
        this.beaconPos = null;
        this.experiencePos = null;
        this.resetBeaconJourneyStats();
        this.mood = this.chooseStartingMood();
        this.goodExperiencesToday = 0;
        this.nextChooseActivityTicks = this.chooseNextChooseActivityTicks();
        this.nextMoodCheckTicks = this.random().nextInt(CHECK_MOOD_INTERVAL_TICKS);
        this.eveningDespawnTimeTicks = this.chooseEveningDespawnTime();
        this.isHungry = false;
        this.isStayingOvernight = false;
        this.currentTargetIndex = -1;
        this.ticksAtCurrentExperience = 0;
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
       }
    }

    private void reconstructExperienceGoals(ServerLevel serverLevel) {
        if (this.experienceTracker.isEmpty()) {
            // Inconsistent state - should not be EXPERIENCING_TARGET with empty stack.
            TouristEntity.logActivity(Verbosity.GAMEPLAY_WARNINGS, "[TouristMind] Load warning: State is EXPERIENCING_TARGET but experience tracker is empty");
            this.transitionTo(TouristState.PLANNING_NEXT_MOVE);
            return;
        }

        if (this.currentExperienceTarget == null) {
            // Inconsistent state - should have a target.
            TouristEntity.logActivity(Verbosity.GAMEPLAY_WARNINGS, "[TouristMind] Load warning: State is EXPERIENCING_TARGET but current experience target is null");
            this.transitionTo(TouristState.CHOOSING_EXPERIENCE_ACTIVITY);
            return;
        }

        ExperienceVisit currentVisit = this.experienceTracker.peekFirst();
        TouristExperience experience = TourismManager.getTouristExperienceById(currentVisit.experienceUUID());

        if (experience == null) {
            // Experience was unloaded / removed - exit gracefully.
            TouristEntity.logActivity(Verbosity.GAMEPLAY_WARNINGS, "[TouristMind] Load warning: Experience {} no longer exists", currentVisit.experienceUUID());
            this.exitCurrentExperience(serverLevel, false);
            return;
        }

        // Recreate and inject the goal for the current target.
        Goal goal = experience.createGoalForTarget(this.tourist, this.currentExperienceTarget);
        if (goal != null) {
            this.injectExperienceGoal(goal);
            TouristEntity.logActivity(Verbosity.LEVEL_1_DIAGNOSTICS, "[TouristMind] Load: Reconstructed experience goal {}", goal.getClass().getSimpleName());
        }
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

    public @Nullable BlockPos getExperiencePos() {
        return this.experiencePos;
    }

    public @Nullable BlockPos getMoveToTarget() {
        if (this.state == TouristState.TRAVELING_TO_BEACON) {
            return this.beaconPos;
        } else if (this.state == TouristState.TRAVELING_TO_EXPERIENCE) {
            return this.experiencePos;
        }
        return null;
    }

    public String getMoveToTargetName() {
        if (this.state == TouristState.TRAVELING_TO_BEACON) {
            TouristBeaconBlockEntity beaconBlockEntity = TourismManager.getBeaconBlockEntity(this.tourist.level(), this.beaconPos);
            if (beaconBlockEntity != null) {
                return beaconBlockEntity.getPlainTextName();
            }
        } else if (this.state == TouristState.TRAVELING_TO_EXPERIENCE) {
            TouristExperience experience = TourismManager.getTouristExperienceByPos(this.experiencePos);
            if (experience != null) {
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

    public boolean isCurrentActivityAtBeacon() {
        return this.state == TouristState.CHOOSING_EXPERIENCE
                || this.state == TouristState.WANDERING_AT_BEACON;
    }

    public boolean isCurrentActivityAtExperience() {
        return this.state == TouristState.CHOOSING_EXPERIENCE_ACTIVITY ||
                this.state == TouristState.TRAVELING_TO_EXPERIENCE_TARGET ||
                this.state == TouristState.EXPERIENCING_TARGET;
    }

    public boolean isPlanningActivity() {
        return this.state == TouristState.PLANNING_NEXT_MOVE || this.state == TouristState.CHOOSING_EXPERIENCE;
    }

    public boolean isTravelingToBlock() {
        return this.state == TouristState.TRAVELING_TO_BEACON ||
                this.state == TouristState.TRAVELING_TO_EXPERIENCE;
    }

    public boolean isWandering() {
        return this.state == TouristState.WANDERING_AT_BEACON || this.state == TouristState.WANDERING_WORLD;
    }

    public void recordExperience(ServerLevel serverLevel, TouristReview review) {
        SoundEvent soundEvent = null;

        switch (review.result()) {
            case ARRIVED, GOOD, GREAT -> soundEvent = SoundEvents.VILLAGER_CELEBRATE;
            case LOST, CLOSED_EARLY, UNFAVORABLE -> soundEvent = SoundEvents.VILLAGER_NO;
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
            case UNFAVORABLE, FAILED_SPAWN, LOST, CLOSED_EARLY, HURT_EN_ROUTE, HURT_ON_PREMISES, KILLED_EN_ROUTE, KILLED_ON_PREMISES ->
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
            if (this.isCurrentActivityAtBeacon() && !this.isInMoodToDespawn()) {
                this.recordGoodExperience(serverLevel);
            }
            this.transitionTo(TouristState.DESPAWNING);
            return;
        }

        // Mood check
        if (this.isTimeToCheckMood() && this.nextMoodCheckTicks <= 0) {
            if (this.isInMoodToDespawn()) {
                this.clearBeaconSession();

                Component moodMessage;
                boolean appendTargetName = true;
                if (this.isCurrentActivityAtBeacon() || this.isCurrentActivityAtExperience()) {
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
                return;
            }
            this.nextMoodCheckTicks = CHECK_MOOD_INTERVAL_TICKS;
        } else {
            this.nextMoodCheckTicks--;
        }

        // State-specific tick handlers
        switch (this.state) {
            case EXPERIENCING_TARGET -> this.tickExperiencingTarget(serverLevel);

            case WANDERING_AT_EXPERIENCE -> {
                if (this.nextChooseActivityTicks <= 0) {
                    this.transitionTo(TouristState.CHOOSING_EXPERIENCE);
                } else {
                    this.nextChooseActivityTicks--;
                }
            }

            case WANDERING_AT_BEACON, WANDERING_WORLD -> {
                if (this.nextChooseActivityTicks <= 0) {
                    this.transitionTo(TouristState.PLANNING_NEXT_MOVE);
                } else {
                    this.nextChooseActivityTicks--;
                }
            }

            // Other states are transition-driven and therefore don't need per-tick updates.
            default -> {}
        }
    }

    private void tickExperiencingTarget(ServerLevel serverLevel) {
        if (this.experienceTracker.isEmpty()) {
            this.transitionTo(TouristState.PLANNING_NEXT_MOVE);
            return;
        }

        ExperienceVisit currentVisit = this.experienceTracker.peekFirst();
        TouristExperience experience = TourismManager.getTouristExperienceById(currentVisit.experienceUUID());

        if (experience == null) {
            this.exitCurrentExperience(serverLevel, false);
            return;
        }

        this.ticksAtCurrentExperience++;

        // Call experience tick - returns true when target is complete.
        boolean targetComplete = experience.tick(this.tourist, serverLevel);

        if (targetComplete) {
            // Remove experience-specific goals.
            this.clearInjectedGoals();

            // Mark target as complete.
            this.markCurrentTargetComplete(serverLevel);

            // Choose next activity (or exit if experience is complete).
            this.transitionTo(TouristState.CHOOSING_EXPERIENCE_ACTIVITY);
        }
    }

    private int chooseEveningDespawnTime() {
        return 12000 + this.random().nextInt(1000);
    }

    private int chooseNextChooseActivityTicks() {
        return this.random().nextIntBetweenInclusive(MIN_ACTIVITY_INTERVAL_TICKS, MAX_ACTIVITY_INTERVAL_TICKS);
    }

    private double chooseStartingMood() {
        return 1.0 + this.random().nextDouble();
    }

    private void clearBeaconSession() {
        this.availableExperienceUUIDs.clear();
        this.visitedExperienceUUIDs.clear();

        // Exit any active experiences.
        ServerLevel serverLevel = (ServerLevel) this.tourist.level();
        while (!this.experienceTracker.isEmpty()) {
            this.exitCurrentExperience(serverLevel, false);
        }
    }

    private void clearInjectedGoals() {
        for (Goal goal : this.injectedExperienceGoals) {
            this.tourist.removeExperienceGoal(goal);
        }
        this.injectedExperienceGoals.clear();
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

    private void resetBeaconJourneyStats() {
        this.closestDistanceToDestination = Double.MAX_VALUE;
        this.consecutiveFailedProgressChecks = 0;
        this.reportedHurtEnRoute = false;
        this.reportedHurtOnPremises = false;
    }

    private void resetDailyStats() {
        this.mood = this.chooseStartingMood();
        this.goodExperiencesToday = 0;
        this.nextMoodCheckTicks = this.random().nextInt(CHECK_MOOD_INTERVAL_TICKS);
        this.eveningDespawnTimeTicks = 12000 + this.random().nextInt(1000);
        this.isStayingOvernight = false;
        this.ticksAtCurrentTarget = 0;
    }

    private void resetExperienceJourneyStats() {
        this.closestDistanceToDestination = Double.MAX_VALUE;
        this.consecutiveFailedProgressChecks = 0;
    }

    //region State Transition Methods
    private void transitionTo(TouristState newState) {
        if (this.state == newState) {
            TouristEntity.logActivity(Verbosity.LEVEL_1_DIAGNOSTICS, "[TouristMind] Transition called for same state ({}) - no transition performed", newState);
            return;
        }

        // Precompute common prerequisites.
        if (!(this.tourist.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        TouristBeaconBlockEntity beaconBlockEntity = this.beaconPos != null
                ? TourismManager.getBeaconBlockEntity(serverLevel, this.beaconPos)
                : null;

        // Validate prerequisites before transition.
        if (newState.requiresBeacon() && beaconBlockEntity == null) {
            TouristEntity.logActivity(Verbosity.GAMEPLAY_WARNINGS, "[TouristMind] Cannot transition to {} - beacon not available", newState);
            this.state = TouristState.WANDERING_WORLD; // fallback
        } else {
            this.state = newState;
        }

        // Call state transition handlers, if applicable.
        TouristEntity.logActivity(Verbosity.MAJOR_EVENTS, "Transitioning state to " + newState);
        switch (newState) {
            case PLANNING_NEXT_MOVE -> this.planNextMove(serverLevel);
            case CHOOSING_EXPERIENCE -> this.chooseExperienceAtBeacon(serverLevel, beaconBlockEntity);
            case TRAVELING_TO_EXPERIENCE -> this.resetExperienceJourneyStats();
            case WANDERING_WORLD -> this.beginWanderingWorld();
            case WANDERING_AT_BEACON -> this.beginWanderingAtBeacon();
            case DESPAWNING, LOST -> this.deSpawn();
            default -> {}
        }
    }

    public void arriveAtBlock() {
        if (!this.isTravelingToBlock()) {
            return;
        }

        this.tourist.stopNavigation();

        if (!(this.tourist.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        this.tourist.clearHeldItem();

        if (this.state == TouristState.TRAVELING_TO_EXPERIENCE) {
            this.arriveAtExperience(serverLevel);
        } else {
            this.arriveAtBeacon(serverLevel);
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
            this.transitionTo(TouristState.CHOOSING_EXPERIENCE);
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
        TouristExperience experience = TourismManager.getTouristExperienceByPos(this.experiencePos);
        if (experience == null) {
            // No experience block found at experiencePos!
            this.updateMood(VisitResult.LOST);
            Component experienceMessage = Component.literal("did not find any tourist experience at " + this.experiencePos.toShortString());
            this.recordExperience(serverLevel, new TouristReview(
                    this.state.reviewTarget(),
                    VisitResult.LOST,
                    true,
                    true,
                    experienceMessage,
                    true,
                    false));
            this.transitionTo(TouristState.PLANNING_NEXT_MOVE); // TODO: Choose a different experience at beacon, if available.
            return;
        }

        Component experienceMessage = Component.literal("arrived at ")
                .append(experience.getDisplayName());
        this.recordExperience(serverLevel, new TouristReview(
                this.state.reviewTarget(),
                VisitResult.ARRIVED,
                false,
                false,
                experienceMessage,
                true,
                false));
        this.transitionTo(TouristState.ENJOYING_EXPERIENCE);
    }

    private void arriveAtExperienceTarget(ServerLevel serverLevel) {
        if (this.experienceTracker.isEmpty() || this.currentExperienceTarget == null) {
            this.transitionTo(TouristState.CHOOSING_EXPERIENCE_ACTIVITY);
            return;
        }

        ExperienceVisit currentVisit = this.experienceTracker.peekFirst();
        TouristExperience experience = TourismManager.getTouristExperienceById(currentVisit.experienceUUID());

        if (experience == null) {
            this.exitCurrentExperience(serverLevel, false);
            return;
        }

        // Stop navigation.
        this.tourist.stopNavigation();
        this.tourist.clearHeldItem();

        // Ask experience to create goal for this specific target.
        Goal experienceGoal = experience.createGoalForTarget(this.tourist, this.currentExperienceTarget);

        if (experienceGoal != null) {
            this.injectExperienceGoal(experienceGoal);
        }

        // Reset tick counter for this target.
        this.ticksAtCurrentTarget = 0;

        this.transitionTo(TouristState.EXPERIENCING_TARGET);
    }

    private void beginWanderingAtBeacon() {
        this.tourist.clearHeldItem();
        this.nextChooseActivityTicks = this.chooseNextChooseActivityTicks();
    }

    private void beginWanderingWorld() {
        this.beaconPos = null;
        this.resetBeaconJourneyStats();
        this.tourist.clearHeldItem();
        this.nextChooseActivityTicks = this.chooseNextChooseActivityTicks();
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
            this.transitionTo(TouristState.CHOOSING_EXPERIENCE);
            return;
        }

        this.experiencePos = experience.getBlockPos().immutable();
        this.transitionTo(TouristState.TRAVELING_TO_EXPERIENCE);
    }

    private void chooseExperienceActivity(ServerLevel serverLevel) {
        if (this.experienceTracker.isEmpty()) {
            // Error state - no experience on stack.
            this.transitionTo(TouristState.PLANNING_NEXT_MOVE);
            return;
        }

        ExperienceVisit currentVisit = this.experienceTracker.peekFirst();
        TouristExperience experience = TourismManager.getTouristExperienceById(currentVisit.experienceUUID());

        if (experience == null) {
            // Experience unloaded - exit gracefully.
            this.exitCurrentExperience(serverLevel, false);
            return;
        }

        // Check if this is a parent experience with child experience as targets.
        List<ExperienceTarget> remainingTargets = currentVisit.remainingTargets();

        if (remainingTargets.isEmpty()) {
            // All targets completed - exit experience.
            this.exitCurrentExperience(serverLevel, true);
            return;
        }

        this.currentExperienceTarget = remainingTargets.getFirst();

        // Is this target a child experience?
        if (this.currentExperienceTarget.isChildExperience()) {
            UUID childUUID = this.currentExperienceTarget.childExperienceUUID();
            TouristExperience childExperience = TourismManager.getTouristExperienceById(childUUID);

            if (childExperience != null && childExperience.isOpenForBusiness()) {
                // Navigate to child experience block position.
                this.experiencePos = this.currentExperienceTarget.pos().immutable();
                this.transitionTo(TouristState.TRAVELING_TO_EXPERIENCE);
            } else {
                // Child experience unavailable - skip this target.
                this.markCurrentTargetComplete(serverLevel);
                this.transitionTo(TouristState.CHOOSING_EXPERIENCE_ACTIVITY);
            }

            return;
        }

        // Regular target (block or entity) - navigate to it.
        this.experiencePos = this.currentExperienceTarget.pos().immutable();
        this.transitionTo(TouristState.TRAVELING_TO_EXPERIENCE_TARGET);
    }

    private void deSpawn() {
        this.tourist.onDespawn();
        this.transitionTo(TouristState.FINISHED);
    }

    private void enterExperience(ServerLevel serverLevel) {
        TouristExperience experience = TourismManager.getTouristExperienceByPos(this.experiencePos);

        if (experience == null || !experience.isOpenForBusiness()) {
            // Experience closed or removed.
            this.updateMood(VisitResult.CLOSED_EARLY);
            Component experienceMessage;
            if (experience == null) {
                experienceMessage = Component.literal("did not find any tourist experience at");
            } else {
                experienceMessage = Component.literal("found experience closed at");
            }
            this.recordExperience(serverLevel, new TouristReview(
                    this.state.reviewTarget(),
                    VisitResult.CLOSED_EARLY,
                    true,
                    true,
                    experienceMessage,
                    true,
                    true
            ));
            this.transitionTo(TouristState.CHOOSING_EXPERIENCE); // Try next experience.
            return;
        }

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
                0
        );
        this.experienceTracker.push(visit);

        // Mark as visited.
        this.visitedExperienceUUIDs.add(experience.getUUID());

        // Reset experience-specific tracking.
        this.ticksAtCurrentExperience = 0;
        this.currentTargetIndex = 0;

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

        this.transitionTo(TouristState.CHOOSING_EXPERIENCE_ACTIVITY);
    }

    private void exitCurrentExperience(ServerLevel serverLevel, boolean completed) {
        if (this.experienceTracker.isEmpty()) {
            this.transitionTo(TouristState.PLANNING_NEXT_MOVE);
            return;
        }

        ExperienceVisit currentVisit = this.experienceTracker.pollFirst();
        TouristExperience experience = TourismManager.getTouristExperienceById(currentVisit.experienceUUID());

        if (experience != null) {
            // Call experience lifecycle method.
            experience.onTouristDeparture(this.tourist, serverLevel, completed);

            // Update experience statistics.
            VisitResult result = completed ? VisitResult.GOOD : VisitResult.UNFAVORABLE;
            experience.rateVisit(result, serverLevel.getDayTime());

            // Record experience for tourist.
            this.updateMood(result);
            this.recordExperience(serverLevel, new TouristReview(
                    this.state.reviewTarget(),
                    result,
                    true,
                    false,
                    Component.literal("is exiting experience"),
                    true,
                    true
            ));
        }

        // Clear experience-specific goals.
        this.clearInjectedGoals();

        // Reset tracking.
        this.ticksAtCurrentExperience = 0;
        this.currentTargetIndex = 0;
        this.currentExperienceTarget = null;

        // Are we in child experience? If so, return to parent.
        if (!this.experienceTracker.isEmpty()) {
            this.transitionTo(TouristState.CHOOSING_EXPERIENCE_ACTIVITY);
        } else {
            // No parent experience - choose next experience at beacon.
            this.transitionTo(TouristState.CHOOSING_EXPERIENCE);
        }
    }

    private void markCurrentTargetComplete(ServerLevel serverLevel) {
        if (this.experienceTracker.isEmpty()) {
            return;
        }

        // Pop current visit, update it, push it back.
        ExperienceVisit currentVisit = this.experienceTracker.pollFirst();

        List<ExperienceTarget> remainingTargets = new ArrayList<>(currentVisit.remainingTargets());
        if (!remainingTargets.isEmpty()) {
            remainingTargets.removeFirst(); // remove completed target
        }

        ExperienceVisit updatedVisit = new ExperienceVisit(
                currentVisit.experienceUUID(),
                remainingTargets,
                currentVisit.targetsCompleted() + 1
        );

        this.experienceTracker.push(updatedVisit);
        this.currentTargetIndex++;
    }

    public void onForcedDespawn() {
        this.transitionTo(TouristState.DESPAWNING);
    }

    public void onLost() {
        if (!this.isTravelingToBlock()) {
            return;
        }

        this.tourist.stopNavigation();

        if (!(this.tourist.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        this.updateMood(VisitResult.LOST);

        if (this.state == TouristState.TRAVELING_TO_EXPERIENCE) {
            TouristExperience experience = TourismManager.getTouristExperienceByPos(this.experiencePos);
            Component experienceMessage;
            if (experience == null) {
                experienceMessage = Component.literal("got lost travelling to experience at " + this.experiencePos.toShortString());
            } else {
                experienceMessage = Component.literal("got lost travelling to ")
                        .append(experience.getDisplayName());
            }
            this.recordExperience(serverLevel, new TouristReview(
                    this.state.reviewTarget(),
                    VisitResult.LOST,
                    true,
                    true,
                    experienceMessage,
                    true,
                    false));
            this.transitionTo(TouristState.PLANNING_NEXT_MOVE); // TODO: Choose a different experience near beacon, if available.
        } else {
            Component experienceMessage = Component.literal("got lost travelling to");
            this.recordExperience(serverLevel, new TouristReview(
                    this.state.reviewTarget(),
                    VisitResult.LOST,
                    true,
                    true,
                    experienceMessage,
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
            this.transitionTo(TouristState.CHOOSING_EXPERIENCE);
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
            this.prepareForJourney(beaconBlockEntity.getBlockPos());
            return;
        }

        this.transitionTo(TouristState.WANDERING_WORLD);
    }

    public void prepareForJourney(@NonNull BlockPos beaconTarget) {
        this.beaconPos = beaconTarget.immutable();
        this.resetBeaconJourneyStats();
        this.tourist.giveItemToHold(new ItemStack(Items.MAP));
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
        if (!this.experienceTracker.isEmpty()) {
            valueOutput.store("ExperienceTracker", ExperienceVisit.CODEC.listOf(),
                    List.copyOf(this.experienceTracker));
        }
        
        if (this.beaconPos != null) {
            valueOutput.store("BeaconPos", BlockPos.CODEC, this.beaconPos);
        }
        if (this.experiencePos != null) {
            valueOutput.store("ExperiencePos", BlockPos.CODEC, this.experiencePos);
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
        List<ExperienceVisit> trackerList = valueInput.read("ExperienceTracker", ExperienceVisit.CODEC.listOf())
                .orElse(List.of());
        this.experienceTracker = new ArrayDeque<>(trackerList);
        
        this.experiencePos = valueInput.read("ExperiencePos", BlockPos.CODEC).orElse(null);
        this.closestDistanceToDestination = valueInput.getDoubleOr("ClosestDistanceToDestination", Double.MAX_VALUE);
        this.consecutiveFailedProgressChecks = valueInput.getIntOr("FailedProgressChecks", 0);
        this.reportedHurtEnRoute = valueInput.getBooleanOr("ReportedHurtEnRoute", false);
        this.reportedHurtOnPremises = valueInput.getBooleanOr("ReportedHurtOnPremises", false);
        this.mood = valueInput.getDoubleOr("Mood", this.chooseStartingMood());
        this.goodExperiencesToday = valueInput.getIntOr("GoodExperiencesToday", 0);
        this.eveningDespawnTimeTicks = valueInput.getIntOr("DespawnTimeTicks", this.chooseEveningDespawnTime());
        this.isHungry = valueInput.getBooleanOr("IsHungry", false);
        this.isStayingOvernight = valueInput.getBooleanOr("IsStayingOvernight", false);
        this.currentTargetIndex = valueInput.getIntOr("CurrentTargetIndex", 0);
        this.ticksAtCurrentExperience = valueInput.getIntOr("TicksAtCurrentExperience", 0);
        this.ticksAtCurrentTarget = valueInput.getIntOr("TicksAtCurrentTarget", 0);

        // Reconstruct currentExperienceTarget from experienceTracker.
        if (!this.experienceTracker.isEmpty()) {
            ExperienceVisit currentVisit = this.experienceTracker.peekFirst();
            List<ExperienceTarget> remaining = currentVisit.remainingTargets();
            if (!remaining.isEmpty()) {
                this.currentExperienceTarget = remaining.getFirst();
            }
        }
    }
}
