package org.bensam.touristry.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.bensam.touristry.block.entity.AbstractExperienceBlockEntity;
import org.bensam.touristry.block.entity.TouristBeaconBlockEntity;
import org.bensam.touristry.config.ModServerConfigManager;
import org.bensam.touristry.config.Verbosity;
import org.bensam.touristry.entity.goal.PositionForViewingGoal;
import org.bensam.touristry.tourism.*;
import org.bensam.touristry.tourism.experience.ExperienceTarget;
import org.bensam.touristry.tourism.experience.ExperienceVisit;
import org.bensam.touristry.tourism.experience.TouristExperience;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public final class TouristMind {
    //region Constants
    private static final float BUDGET_MIN_EMERALDS = 4.0F;
    private static final float BUDGET_MAX_EMERALDS = 1000.0F;
    private static final double BUDGET_MEAN_EMERALDS = 8.0F;
    private static final double BUDGET_STD_DEV_EMERALDS = 2.0D;
    private static final int CHECK_MOOD_INTERVAL_TICKS = 250;
    private static final double MIN_MOOD = -3.0;
    private static final double MAX_MOOD = 4.0;
    private static final int MIN_ACTIVITY_INTERVAL_TICKS = 500;
    private static final int MAX_ACTIVITY_INTERVAL_TICKS = 2000;
    private static final long MIN_TICKS_BEFORE_MAP_TOGGLE = 20;
    private static final int MIN_WAIT_AT_BEACON_AFTER_ARRIVAL_TICKS = 40;
    private static final int MAX_WAIT_AT_BEACON_AFTER_ARRIVAL_TICKS = 80;
    private static final int MAX_WAVE_COUNT = 3;
    private static final int MIN_WAVE_AT_ENTITY_INTERVAL_TICKS = 20 * 30; // 30 game seconds
    //endregion

    //region Fields
    // Tourist entity-related
    private final TouristEntity tourist;
    private TouristState state;
    private double closestDistanceToDestination;
    private int consecutiveFailedProgressChecks;
    private float dailyBudgetEmeralds; // does not include budget for overnight accommodations, which is handled separately
    private int eveningDespawnTimeTicks;
    private int goodExperiencesToday;
    private List<TouristItemInterest> interests = new ArrayList<>();
    private boolean isHungry;
    private boolean isStayingOvernight;
    private long lastMapToggleTicks; // (not persisted)
    private double mood;
    private int nextMoodCheckTicks; // (not persisted)
    private float remainingBudgetEmeralds;
    private boolean reportedHurtEnRoute;
    private boolean reportedHurtOnPremises;
    private int waitTicks;
    private final Map<UUID, WaveRecord> waveMemory = new HashMap<>(); // (not persisted)

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

    private record WaveRecord(int count, int lastWaveTick) {}

    public TouristMind(TouristEntity tourist) {
        this.tourist = tourist;
        this.beaconPos = null;
        this.closestDistanceToDestination = Double.MAX_VALUE;
        this.dailyBudgetEmeralds = this.generateDailyBudget();
        this.eveningDespawnTimeTicks = this.generateRandomDespawnTime();
        this.experienceBlockPos = null;
        this.mood = this.generateRandomStartingMood();
        this.nextMoodCheckTicks = this.random().nextInt(CHECK_MOOD_INTERVAL_TICKS);
        this.remainingBudgetEmeralds = this.dailyBudgetEmeralds;
        this.state = TouristState.IDLE;
        this.targetPos = null;
        this.generateInterests();
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
        this.mood = this.generateRandomStartingMood();
        this.nextMoodCheckTicks = this.random().nextInt(CHECK_MOOD_INTERVAL_TICKS);
        this.remainingBudgetEmeralds = this.dailyBudgetEmeralds;
        this.reportedHurtEnRoute = false;
        this.reportedHurtOnPremises = false;
        this.ticksAtCurrentTarget = 0;
        this.tourist.setUnhappyCounter(0);
        this.waveMemory.clear();
    }

    /**
     * Called after entity and mind data have been fully loaded.
     * Reconstructs runtime state that cannot be serialized (e.g., Goals).
     *
     * <p>CRITICAL: Must be called from TouristEntity.readAdditionalSaveData()
     * after mind.readAdditionalSaveData() completes.</p>
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
        Goal goal = experience.createGoalForTarget(this.tourist, serverLevel, this.currentExperienceTarget);
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

    public @Nullable BlockPos getExperiencePos() {
        return this.experienceBlockPos;
    }

    public @Nullable ExperienceVisit getExperienceVisit() {
        return this.experienceTargetTracker.peekFirst();
    }

    public List<TouristItemInterest> getInterests() {
        return List.copyOf(this.interests);
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

    public TouristState getState() {
        return this.state;
    }

    public String getStateForLogging() {
        return this.getStateForLogging(this.state);
    }

    public String getStateForLogging(TouristState state) {
        String targetName = this.getStateTargetName(state);
        if (targetName.isEmpty()) {
            targetName = "(unknown)";
        }

        String logMessageSuffix = switch (state) {
            case TRAVELING_TO_BEACON, WAIT_AT_BEACON, CHOOSING_EXPERIENCE_AT_BEACON, WANDERING_AT_BEACON,
                 TRAVELING_TO_EXPERIENCE, WAIT_AT_EXPERIENCE, ENTERING_EXPERIENCE, WANDERING_AT_EXPERIENCE,
                 TRAVELING_TO_EXPERIENCE_TARGET, POSITIONING_AT_TARGET, EXPERIENCING_TARGET -> " " + targetName;
            case CHOOSING_EXPERIENCE_TARGET, SLEEPING -> " at " + targetName;
            default -> "";
        };

        return state + logMessageSuffix;
    }

    public String getStateTargetName() {
        return this.getStateTargetName(this.state);
    }

    public String getStateTargetName(TouristState state) {
        switch (state) {
            case TRAVELING_TO_BEACON, WAIT_AT_BEACON, CHOOSING_EXPERIENCE_AT_BEACON, WANDERING_AT_BEACON -> {
                TouristBeaconBlockEntity beaconBlockEntity = TourismManager.getBeaconBlockEntity(this.tourist.level(), this.beaconPos);
                if (beaconBlockEntity != null) {
                    return beaconBlockEntity.getPlainTextName();
                } else {
                    return "";
                }
            }
            case TRAVELING_TO_EXPERIENCE, WAIT_AT_EXPERIENCE, ENTERING_EXPERIENCE, WANDERING_AT_EXPERIENCE,
                 CHOOSING_EXPERIENCE_TARGET, SLEEPING -> {
                TouristExperience experience = TourismManager.getTouristExperienceByPos(this.experienceBlockPos);
                if (experience != null) {
                    return experience.getDisplayName().getString();
                } else {
                    return "";
                }
            }
            case TRAVELING_TO_EXPERIENCE_TARGET, POSITIONING_AT_TARGET, EXPERIENCING_TARGET -> {
                if (this.targetPos == null) {
                    return "";
                }
                TouristExperience experience = TourismManager.getTouristExperienceByPos(this.experienceBlockPos);
                if (experience != null) {
                    return this.targetPos.toShortString() + " from " + experience.getDisplayName().getString();
                } else {
                    return this.targetPos.toShortString();
                }
            }
            default -> { return ""; }
        }
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

    public boolean isItemOfInterest(ItemStack itemStack) {
        for (TouristItemInterest interest : this.interests) {
            if (interest.isAMatch(itemStack)) {
                TouristEntity.logActivity(Verbosity.LEVEL_2_DIAGNOSTICS, "[TouristMind] Tourist found {} matching interest {}", itemStack.getItem().getName().getString(), interest);
                return true;
            }
        }
        return false;
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

    public void setWavingAtEntity(Entity entity, boolean wave) {
        if (!wave) {
            this.tourist.setWaving(false);
            TouristEntity.logActivity(Verbosity.LEVEL_1_DIAGNOSTICS, "[TouristMind] Stopped waving at {}, UUID={}",
                    entity == null ? "no one" : entity.getDisplayName().getString(),
                    entity == null ? "N/A" : entity.getUUID().toString());
            return;
        }

        if (TouristEntity.wouldWaveAt(entity)) {
            WaveRecord waveRecord = this.waveMemory.getOrDefault(entity.getUUID(), new WaveRecord(0, -1));
            int tickTimeOfDay = (int) (entity.level().getDayTime() % 24000L);
            if (waveRecord.count() < MAX_WAVE_COUNT &&
                    tickTimeOfDay > (waveRecord.lastWaveTick() + MIN_WAVE_AT_ENTITY_INTERVAL_TICKS)
            ) {
                waveRecord = new WaveRecord(waveRecord.count() + 1, tickTimeOfDay);
                this.waveMemory.put(entity.getUUID(), waveRecord);
                this.tourist.setWaving(true);
                TouristEntity.logActivity(Verbosity.LEVEL_2_DIAGNOSTICS, "[TouristMind] Waving at {}, count={}, UUID={}", entity.getDisplayName().getString(), waveRecord.count(), entity.getUUID().toString());
            }
        }
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
            case UNFAVORABLE, FAILED_SPAWN, LOST, CLOSED_EARLY, UNAFFORDABLE, PAYMENT_FAILED, HURT_EN_ROUTE, HURT_ON_PREMISES, KILLED_EN_ROUTE, KILLED_ON_PREMISES ->
                    result.moodDelta() * (0.75 + 0.5 * positiveNormalized);
        };

        this.mood = Mth.clamp(this.mood + change, MIN_MOOD, MAX_MOOD);
        this.tourist.setUnhappyCounter(this.mood < 0 ? 1 : 0);
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
        // Perform checks while the target Goal created by the experience runs until it considers the visit complete.
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

    // Prerequisite: entry fee for current experience has already been paid.
    private float generateAllowanceForExperience(TouristExperience experience) {
        if (!experience.canSpendBudgetHere()) {
            return 0;
        }

        float remainingBudget = this.remainingBudgetEmeralds;
        if (remainingBudget <= 1.0F) {
            return remainingBudget;
        }

        int remainingPlacesToSpendBudget = 0;

        for (UUID availableExperienceUUID : this.availableExperienceUUIDs) {
            TouristExperience availableExperience = TourismManager.getTouristExperienceById(availableExperienceUUID);
            if (availableExperience != null &&
                    availableExperience.canSpendBudgetHere() &&
                    TouristEconomy.getEmeraldEquivalent(availableExperience.getEntryFee()) < remainingBudget
            ) {
                remainingPlacesToSpendBudget++;
            }
        }

        if (remainingPlacesToSpendBudget <= 1) {
            // The current experience is the only place remaining in the list of available experiences. They can spend it all here!
            return remainingBudget;
        }

        // Determine the number of experiences at the moment where the tourist wants to spend their remaining daily budget.
        // This number will usually be less than the total number of remaining experiences, centered around half of them,
        // resulting in a larger allowance for the current experience than simply dividing their budget by the number of remaining experiences.
        double medianPlacesToSpendBudget = remainingPlacesToSpendBudget / 2.0D;
        int placesTouristWantsToSpendBudget = (int) Math.clamp(medianPlacesToSpendBudget + (this.random().nextGaussian() * 2.0D), 1, remainingPlacesToSpendBudget);
        return remainingBudget / placesTouristWantsToSpendBudget;
    }

    private float generateDailyBudget() {
        double budget = BUDGET_MEAN_EMERALDS + (this.random().nextGaussian() * BUDGET_STD_DEV_EMERALDS);
        return Math.clamp(Math.round(budget), BUDGET_MIN_EMERALDS, BUDGET_MAX_EMERALDS);
    }

    private void generateInterests() {
        for (TouristItemInterest interest : TouristItemInterest.values()) {
            if (this.random().nextFloat() <= interest.probability()) {
                this.interests.add(interest);
                if (interest == TouristItemInterest.EPIC_ITEMS) {
                    this.dailyBudgetEmeralds += 100;
                } else if (interest == TouristItemInterest.RARE_ITEMS) {
                    this.dailyBudgetEmeralds += 25;
                }
            }
        }
    }

    private int generateRandomDespawnTime() {
        return 12000 + this.random().nextInt(1000);
    }

    private double generateRandomStartingMood() {
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

    public void spendBudget(float amount) {
        this.remainingBudgetEmeralds -= amount;
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

    public void updateExperienceVisitAllowance(float newAllowance) {
        if (this.experienceTargetTracker.isEmpty()) {
            return;
        }

        // Pop current visit, update it, push it back.
        ExperienceVisit currentVisit = this.experienceTargetTracker.pollFirst();

        ExperienceVisit updatedVisit = new ExperienceVisit(
                currentVisit.experienceUUID(),
                newAllowance,
                currentVisit.remainingTargets(),
                currentVisit.targetsCompleted(),
                currentVisit.totalTargets(),
                currentVisit.result()
        );

        this.experienceTargetTracker.push(updatedVisit);
    }

    public void updateExperienceVisitResult(VisitResult newResult) {
        if (this.experienceTargetTracker.isEmpty()) {
            return;
        }

        // Pop current visit, update it, push it back.
        ExperienceVisit currentVisit = this.experienceTargetTracker.pollFirst();

        ExperienceVisit updatedVisit = new ExperienceVisit(
                currentVisit.experienceUUID(),
                currentVisit.budgetRemaining(),
                currentVisit.remainingTargets(),
                currentVisit.targetsCompleted(),
                currentVisit.totalTargets(),
                newResult
        );

        this.experienceTargetTracker.push(updatedVisit);
    }

    //region State Transition Methods
    private void transitionTo(TouristState newState) {
        if (!(this.tourist.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        // Validate prerequisites before transition.
        TouristBeaconBlockEntity beaconBlockEntity = null;
        if (newState.requiresBeaconPos()) {
            if (this.beaconPos != null) {
                beaconBlockEntity = TourismManager.getBeaconBlockEntity(serverLevel, this.beaconPos);
            }

            if (beaconBlockEntity == null) {
                beaconBlockEntity = TourismManager.findClosestBeaconEntity(this.tourist.blockPosition());
                if (beaconBlockEntity != null) {
                    this.beaconPos = beaconBlockEntity.getBlockPos();
                    TouristEntity.logActivity(Verbosity.LEVEL_2_DIAGNOSTICS, "[TouristMind] Set beacon position to {}", this.beaconPos.toShortString());
                } else {
                    TouristEntity.logActivity(Verbosity.GAMEPLAY_WARNINGS, "[TouristMind] Cannot transition to {} - no beacon available", newState);
                    this.transitionTo(TouristState.WANDERING_WORLD); // fallback
                    return;
                }
            }
        }

        TouristExperience experience = null;
        if (newState.requiresExperiencePos()) {
            if (this.experienceBlockPos != null) {
                experience = TourismManager.getTouristExperienceByPos(this.experienceBlockPos);
            }

            if (experience == null) {
                TouristEntity.logActivity(Verbosity.GAMEPLAY_WARNINGS, "[TouristMind] Cannot transition to {} - experience block not available", newState);
                this.transitionTo(TouristState.PLANNING_NEXT_MOVE); // fallback
                return;
            }
        }

        if (newState == TouristState.TRAVELING_TO_EXPERIENCE_TARGET ||
                newState == TouristState.POSITIONING_AT_TARGET ||
                newState == TouristState.EXPERIENCING_TARGET) {
            if (this.targetPos == null) {
                TouristEntity.logActivity(Verbosity.GAMEPLAY_WARNINGS, "[TouristMind] Cannot transition to {} - target BlockPos not available", newState);
                this.removeCurrentTarget(false);
                this.transitionTo(TouristState.CHOOSING_EXPERIENCE_TARGET); // fallback
                return;
            }
        }

        String stateForLogging = this.getStateForLogging(newState);
        if (this.state == newState) {
            TouristEntity.logActivity(Verbosity.MAJOR_EVENTS, "[TouristMind] Re-entering state {}", stateForLogging);
        } else {
            TouristEntity.logActivity(Verbosity.MAJOR_EVENTS, "[TouristMind] Transitioning state to {}", stateForLogging);
        }

        this.state = newState;

        // Call state transition handlers, if applicable.
        switch (newState) {
            case PLANNING_NEXT_MOVE -> this.planNextMove(serverLevel);
            case TRAVELING_TO_BEACON -> this.beginTravelingToBeacon();
            case WAIT_AT_BEACON, WAIT_AT_EXPERIENCE -> this.beginWaitAtBlock(serverLevel);
            case CHOOSING_EXPERIENCE_AT_BEACON -> this.chooseExperienceAtBeacon(serverLevel, beaconBlockEntity);
            case CHOOSING_EXPERIENCE_TARGET -> this.chooseExperienceTarget(serverLevel);
            case TRAVELING_TO_EXPERIENCE -> this.beginTravelingToExperienceBlock();
            case TRAVELING_TO_EXPERIENCE_TARGET -> this.beginTravelingToExperienceTarget();
            case ENTERING_EXPERIENCE -> this.enterExperience(serverLevel);
            case POSITIONING_AT_TARGET -> this.positionAtTarget(serverLevel);
            case WANDERING_WORLD -> this.beginWanderingWorld();
            case WANDERING_AT_BEACON, WANDERING_AT_EXPERIENCE -> this.beginWanderingAtBlock();
            case DESPAWNING, LOST -> this.deSpawn();
            default -> { /* not applicable or post-transition handled by tick() handler */ }
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

            case TRAVELING_TO_EXPERIENCE -> this.arriveAtExperience();

            case TRAVELING_TO_EXPERIENCE_TARGET -> this.arriveAtExperienceTarget();
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
                    true,
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

    private void arriveAtExperience() {
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

    private void arriveAtExperienceTarget() {
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

    private void beginWaitAtBlock(ServerLevel serverLevel) {
        this.lastMapToggleTicks = this.tourist.level().getDayTime();

        if (this.state == TouristState.WAIT_AT_EXPERIENCE &&
                serverLevel.getBlockEntity(this.experienceBlockPos) instanceof AbstractExperienceBlockEntity experienceBlockEntity
        ) {
            this.waitTicks = experienceBlockEntity.getPostArrivalWaitTicks(this.random());
        } else {
            this.waitTicks = this.getRandomWaitTicks(MIN_WAIT_AT_BEACON_AFTER_ARRIVAL_TICKS, MAX_WAIT_AT_BEACON_AFTER_ARRIVAL_TICKS);
        }
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
            VisitResult result = currentVisit.result();
            if (result == VisitResult.ARRIVED) {
                // No updates since tourist arrived. Use target completion as a default result.
                result = currentVisit.allTargetsCompleted() ? VisitResult.GOOD : VisitResult.UNFAVORABLE;
            }
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
                this.removeCurrentTarget(false);
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
        if (!entryFee.isEmpty()) {
            float feeValue = (int) TouristEconomy.getEmeraldEquivalent(entryFee);
            // TODO Use TouristEconomy to determine if entry fee is reasonable.
            if (feeValue > this.remainingBudgetEmeralds && this.remainingBudgetEmeralds >= 1.0F) {
                this.updateMood(VisitResult.UNAFFORDABLE);
                this.recordExperience(serverLevel, new TouristReview(
                        this.state.reviewTarget(),
                        VisitResult.UNAFFORDABLE,
                        true,
                        false,
                        Component.literal("found experience too expensive at"),
                        true,
                        true
                ));
                this.transitionTo(TouristState.CHOOSING_EXPERIENCE_AT_BEACON); // Try next experience.
                return;
            }

            if (experience.tryDepositPayment(entryFee)) {
                this.spendBudget(feeValue);
            } else {
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
                this.generateAllowanceForExperience(experience),
                new ArrayList<>(targets),
                0,
                targets.size(),
                VisitResult.ARRIVED
        );
        this.experienceTargetTracker.push(visit);

        // Record arrival.
        this.updateMood(VisitResult.ARRIVED);
        this.recordExperience(serverLevel, new TouristReview(
                this.state.reviewTarget(),
                VisitResult.ARRIVED,
                true,
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
        Goal experienceGoal = experience.createGoalForTarget(this.tourist, serverLevel, this.currentExperienceTarget);

        if (experienceGoal != null) {
            this.injectExperienceGoal(experienceGoal);
            TouristEntity.logActivity(Verbosity.LEVEL_2_DIAGNOSTICS,
                    "[TouristMind] Injected experience goal: {}", experienceGoal.getClass().getSimpleName());
        }

        this.transitionTo(TouristState.EXPERIENCING_TARGET);
    }

    public void finishTargetGoal() {
        // Remove experience-specific goals.
        this.clearInjectedGoals();

        // Mark target as complete.
        this.removeCurrentTarget(true);

        // Choose next activity (or exit if experience is complete).
        this.transitionTo(TouristState.CHOOSING_EXPERIENCE_TARGET);
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
            this.removeCurrentTarget(false);
            this.transitionTo(TouristState.CHOOSING_EXPERIENCE_TARGET);
        } else if (this.state == TouristState.TRAVELING_TO_EXPERIENCE) {
            Component experienceMessage = Component.literal("got lost travelling to");
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
        Goal positioningGoal = new PositionForViewingGoal(
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

    public void prepareForJourney(ServerLevel serverLevel, @NonNull BlockPos target) {
        BlockEntity blockEntity = serverLevel.getBlockEntity(target);
        if (blockEntity instanceof TouristBeaconBlockEntity) {
            this.beaconPos = target.immutable();
            this.transitionTo(TouristState.TRAVELING_TO_BEACON);
        } else if (blockEntity instanceof AbstractExperienceBlockEntity) {
            this.beaconPos = null;
            this.experienceBlockPos = target.immutable();
            this.transitionTo(TouristState.TRAVELING_TO_EXPERIENCE);
        }
    }

    private void removeCurrentTarget(boolean markCompleted) {
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
                currentVisit.budgetRemaining(),
                remainingTargets,
                markCompleted ? currentVisit.targetsCompleted() + 1 : currentVisit.targetsCompleted(),
                currentVisit.totalTargets(),
                currentVisit.result()
        );

        this.experienceTargetTracker.push(updatedVisit);
        //this.currentTargetIndex++; (not currently used - completed targets are removed from list)
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

        if (!this.interests.isEmpty()) {
            valueOutput.store("Interests", TouristItemInterest.CODEC.listOf(), List.copyOf(this.interests));
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
        valueOutput.putFloat("DailyBudget", this.dailyBudgetEmeralds);
        valueOutput.putFloat("RemainingBudget", this.remainingBudgetEmeralds);
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

        List<TouristItemInterest> interests = new ArrayList<>(
                valueInput.read("Interests", TouristItemInterest.CODEC.listOf()).orElse(List.of())
        );
        if (!interests.isEmpty()) {
            this.interests = List.copyOf(interests);
        }

        this.experienceBlockPos = valueInput.read("ExperienceBlockPos", BlockPos.CODEC).orElse(null);
        this.targetPos = valueInput.read("TargetPos", BlockPos.CODEC).orElse(null);
        this.closestDistanceToDestination = valueInput.getDoubleOr("ClosestDistanceToDestination", Double.MAX_VALUE);
        this.consecutiveFailedProgressChecks = valueInput.getIntOr("FailedProgressChecks", 0);
        this.reportedHurtEnRoute = valueInput.getBooleanOr("ReportedHurtEnRoute", false);
        this.reportedHurtOnPremises = valueInput.getBooleanOr("ReportedHurtOnPremises", false);
        this.mood = valueInput.getDoubleOr("Mood", this.mood);
        this.tourist.setUnhappyCounter(this.mood < 0 ? 1 : 0);
        this.goodExperiencesToday = valueInput.getIntOr("GoodExperiencesToday", 0);
        this.eveningDespawnTimeTicks = valueInput.getIntOr("DespawnTimeTicks", this.eveningDespawnTimeTicks);
        this.isHungry = valueInput.getBooleanOr("IsHungry", false);
        this.isStayingOvernight = valueInput.getBooleanOr("IsStayingOvernight", false);
        this.currentTargetIndex = valueInput.getIntOr("CurrentTargetIndex", 0);
        this.ticksAtCurrentExperience = valueInput.getIntOr("TicksAtCurrentExperience", 0);
        this.ticksAtCurrentTarget = valueInput.getIntOr("TicksAtCurrentTarget", 0);
        this.waitTicks = valueInput.getIntOr("WaitTicks", 0);
        this.dailyBudgetEmeralds = valueInput.getFloatOr("DailyBudgetEmeralds", this.dailyBudgetEmeralds);
        this.remainingBudgetEmeralds = valueInput.getFloatOr("RemainingBudgetEmeralds", this.remainingBudgetEmeralds);

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
