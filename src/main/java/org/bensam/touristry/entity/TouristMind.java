package org.bensam.touristry.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.bensam.touristry.block.entity.TouristBeaconBlockEntity;
import org.bensam.touristry.config.ModServerConfigManager;
import org.bensam.touristry.config.Verbosity;
import org.bensam.touristry.tourism.TouristLocation;
import org.bensam.touristry.tourism.TourismManager;
import org.bensam.touristry.tourism.TouristReview;
import org.bensam.touristry.tourism.VisitResult;
import org.bensam.touristry.tourism.experience.ExperienceVisit;
import org.bensam.touristry.tourism.experience.TouristExperience;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public final class TouristMind {
    private static final double MIN_MOOD = -3.0;
    private static final double MAX_MOOD = 4.0;
    private static final int MIN_ACTIVITY_INTERVAL_TICKS = 500;
    private static final int MAX_ACTIVITY_INTERVAL_TICKS = 2000;
    private static final int CHECK_MOOD_INTERVAL_TICKS = 250;

    private final TouristEntity tourist;

    // persisted data
    private TouristLocation currentLocation; // TODO: do we still need this?
    private TouristState state;
    private Deque<ExperienceVisit> experienceStack = new ArrayDeque<>();
    private BlockPos beaconTarget;
    private BlockPos experienceTarget;
    private double closestDistanceToTarget;
    private int consecutiveFailedProgressChecks;
    private boolean reportedHurtEnRoute;
    private boolean reportedHurtOnPremises;
    private double mood;
    private int goodExperiencesToday;
    private int eveningDespawnTimeTicks;
    private boolean isHungry;
    private boolean isStayingOvernight;
    private int ticksAtCurrentTarget;

    private int nextChooseActivityTicks;
    private int nextMoodCheckTicks;

    public TouristMind(TouristEntity tourist) {
        this.tourist = tourist;
        this.currentLocation = TouristLocation.WORLD;
        this.state = TouristState.IDLE;
        this.beaconTarget = null;
        this.experienceTarget = null;
        this.resetBeaconJourneyStats();
        this.mood = this.chooseStartingMood();
        this.goodExperiencesToday = 0;
        this.nextChooseActivityTicks = this.chooseNextChooseActivityTicks();
        this.nextMoodCheckTicks = this.random().nextInt(CHECK_MOOD_INTERVAL_TICKS);
        this.eveningDespawnTimeTicks = this.chooseEveningDespawnTime();
        this.isHungry = false;
        this.isStayingOvernight = false;
        this.ticksAtCurrentTarget = 0;
    }

    public boolean avoidWater() {
        return this.state == TouristState.WANDERING_AT_BEACON;
    }

    public @Nullable BlockPos getBeaconTarget() {
        return this.beaconTarget;
    }

    public double getClosestDistanceToTarget() {
        return this.closestDistanceToTarget;
    }

    public int getConsecutiveFailedProgressChecks() {
        return this.consecutiveFailedProgressChecks;
    }

    public @Nullable BlockPos getExperienceTarget() {
        return this.experienceTarget;
    }

    public @Nullable BlockPos getMoveToTarget() {
        if (this.state == TouristState.TRAVELING_TO_BEACON) {
            return this.beaconTarget;
        } else if (this.state == TouristState.TRAVELING_TO_EXPERIENCE) {
            return this.experienceTarget;
        }
        return null;
    }

    public String getMoveToTargetName() {
        if (this.state == TouristState.TRAVELING_TO_BEACON) {
            TouristBeaconBlockEntity beaconBlockEntity = TourismManager.getBeaconBlockEntity(this.tourist.level(), this.beaconTarget);
            if (beaconBlockEntity != null) {
                return beaconBlockEntity.getPlainTextName();
            }
        } else if (this.state == TouristState.TRAVELING_TO_EXPERIENCE) {
            TouristExperience experience = TourismManager.getTouristExperienceByPos(this.experienceTarget);
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
        this.closestDistanceToTarget = closestDistanceToTarget;
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

        if (this.isTimeToDespawn()) {
            if (this.isCurrentActivityAtBeacon() && !this.isInMoodToDespawn()) {
                this.recordGoodExperience(serverLevel);
            }
            this.transitionTo(TouristState.DESPAWNING);
            return;
        }

        if (this.isTimeToCheckMood()) {
            if (this.nextMoodCheckTicks > 0) {
                this.nextMoodCheckTicks--;
            }

            if (this.nextMoodCheckTicks <= 0) {
                if (this.isInMoodToDespawn()) {
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
            }
        }

        if (this.state == TouristState.ENJOYING_EXPERIENCE) {
            // TODO: Execute Experience tick.
            // TODO: Consider adding substates that a TouristExperience will manage.
            // EXPERIENCING_TARGET might be a good candidate for moving to a substate.
            //     In this state, you'd advance ticksAtCurrentTarget, among other things.
            // TODO: Determine which class is responsible for persisting ticksAtCurrentTarget.
            this.transitionTo(TouristState.PLANNING_NEXT_MOVE);
        } else if (!this.isTravelingToBlock() && !this.isPlanningActivity()) {
            if (this.nextChooseActivityTicks > 0) {
                this.nextChooseActivityTicks--;
            }

            if (this.nextChooseActivityTicks <= 0) {
                if (this.isTimeToCheckMood() && this.isInMoodToDespawn()) {
                    this.nextMoodCheckTicks = 0;
                    return;
                }

                if (this.isCurrentActivityAtBeacon()) {
                    this.recordGoodExperience(serverLevel);
                }

                this.transitionTo(TouristState.PLANNING_NEXT_MOVE);
            }
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
        this.closestDistanceToTarget = Double.MAX_VALUE;
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
        this.closestDistanceToTarget = Double.MAX_VALUE;
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

        TouristBeaconBlockEntity beaconBlockEntity = this.beaconTarget != null
                ? TourismManager.getBeaconBlockEntity(serverLevel, this.beaconTarget)
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
            case PLANNING_NEXT_MOVE -> this.planNextTarget(serverLevel);
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
        TouristBeaconBlockEntity beaconBlockEntity = TourismManager.getBeaconBlockEntity(tourist.level(), this.beaconTarget);

        if (beaconBlockEntity == null) {
            // No beacon found at beaconTarget!
            this.updateMood(VisitResult.LOST);
            Component experienceMessage = Component.literal("did not find a beacon at " + this.beaconTarget.toShortString());
            this.recordExperience(serverLevel, new TouristReview(
                    this.state.reviewTarget(),
                    VisitResult.LOST,
                    true,
                    true,
                    experienceMessage,
                    true,
                    false));
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
        TouristExperience experience = TourismManager.getTouristExperienceByPos(this.experienceTarget);
        if (experience == null) {
            // No experience block found at experienceTarget!
            this.updateMood(VisitResult.LOST);
            Component experienceMessage = Component.literal("did not find any tourist experience at " + this.experienceTarget.toShortString());
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

    private void beginWanderingAtBeacon() {
        this.tourist.clearHeldItem();
        this.nextChooseActivityTicks = this.chooseNextChooseActivityTicks();
    }

    private void beginWanderingWorld() {
        this.beaconTarget = null;
        this.resetBeaconJourneyStats();
        this.tourist.clearHeldItem();
        this.nextChooseActivityTicks = this.chooseNextChooseActivityTicks();
    }

    private void chooseExperienceAtBeacon(ServerLevel serverLevel, TouristBeaconBlockEntity beaconBlockEntity) {
        List<TouristExperience> experiences = TourismManager.getTouristExperiencesNearBeacon(serverLevel, beaconBlockEntity);

        if (experiences.isEmpty()) {
            this.transitionTo(TouristState.WANDERING_AT_BEACON);
            return;
        }

        int index = this.random().nextInt(experiences.size());
        TouristExperience experience = experiences.get(index);
        this.experienceTarget = experience.getBlockPos().immutable();
        this.transitionTo(TouristState.TRAVELING_TO_EXPERIENCE);
    }

    private void deSpawn() {
        this.tourist.onDespawn();
        this.transitionTo(TouristState.FINISHED);
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
            TouristExperience experience = TourismManager.getTouristExperienceByPos(this.experienceTarget);
            Component experienceMessage;
            if (experience == null) {
                experienceMessage = Component.literal("got lost travelling to experience at " + this.experienceTarget.toShortString());
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

    private void planNextTarget(ServerLevel serverLevel) {
        List<TouristBeaconBlockEntity> closestBeacons;

        double maxTravelDistanceToNextBeacon = ModServerConfigManager.getConfig().touristEntityConfig().getMaxTravelDistanceToNextBeacon();
        double maxTravelDistanceToNextBeaconSqr = maxTravelDistanceToNextBeacon * maxTravelDistanceToNextBeacon;
        if (this.beaconTarget == null) {
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
                    this.beaconTarget,
                    beaconBlockEntity ->
                            !beaconBlockEntity.getBlockPos().equals(this.beaconTarget)
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
        this.beaconTarget = beaconTarget.immutable();
        this.resetBeaconJourneyStats();
        this.tourist.giveItemToHold(new ItemStack(Items.MAP));
        this.transitionTo(TouristState.TRAVELING_TO_BEACON);
    }
    //endregion

    public void addAdditionalSaveData(ValueOutput valueOutput) {
        valueOutput.store("CurrentLocation", TouristLocation.CODEC, this.currentLocation);
        valueOutput.store("State", TouristState.CODEC, this.state);
        
        // Serialize experience stack as a list (bottom to top order).
        if (!this.experienceStack.isEmpty()) {
            valueOutput.store("ExperienceStack", ExperienceVisit.CODEC.listOf(), 
                    List.copyOf(this.experienceStack));
        }
        
        if (this.beaconTarget != null) {
            valueOutput.store("BeaconTarget", BlockPos.CODEC, this.beaconTarget);
        }
        if (this.experienceTarget != null) {
            valueOutput.store("ExperienceTarget", BlockPos.CODEC, this.experienceTarget);
        }
        valueOutput.putDouble("ClosestDistanceToBeacon", this.closestDistanceToTarget);
        valueOutput.putInt("FailedProgressChecks", this.consecutiveFailedProgressChecks);
        valueOutput.putBoolean("ReportedHurtEnRoute", this.reportedHurtEnRoute);
        valueOutput.putBoolean("ReportedHurtOnPremises", this.reportedHurtOnPremises);
        valueOutput.putDouble("Mood", this.mood);
        valueOutput.putInt("GoodExperiencesToday", this.goodExperiencesToday);
        valueOutput.putInt("DespawnTimeTicks", this.eveningDespawnTimeTicks);
        valueOutput.putBoolean("IsHungry", this.isHungry);
        valueOutput.putBoolean("IsStayingOvernight", this.isStayingOvernight);
    }

    public void readAdditionalSaveData(ValueInput valueInput) {
        this.currentLocation = valueInput.read("CurrentLocation", TouristLocation.CODEC).orElse(TouristLocation.WORLD);
        this.beaconTarget = valueInput.read("BeaconTarget", BlockPos.CODEC).orElse(null);
        this.state = valueInput.read("State", TouristState.CODEC).orElse(
                (this.beaconTarget != null ? TouristState.TRAVELING_TO_BEACON : TouristState.IDLE));
        
        // Deserialize experience stack (restore as ArrayDeque).
        List<ExperienceVisit> stackList = valueInput.read("ExperienceStack", ExperienceVisit.CODEC.listOf())
                .orElse(List.of());
        this.experienceStack = new ArrayDeque<>(stackList);
        
        this.experienceTarget = valueInput.read("ExperienceTarget", BlockPos.CODEC).orElse(null);
        this.closestDistanceToTarget = valueInput.getDoubleOr("ClosestDistanceToBeacon", Double.MAX_VALUE);
        this.consecutiveFailedProgressChecks = valueInput.getIntOr("FailedProgressChecks", 0);
        this.reportedHurtEnRoute = valueInput.getBooleanOr("ReportedHurtEnRoute", false);
        this.reportedHurtOnPremises = valueInput.getBooleanOr("ReportedHurtOnPremises", false);
        this.mood = valueInput.getDoubleOr("Mood", this.chooseStartingMood());
        this.goodExperiencesToday = valueInput.getIntOr("GoodExperiencesToday", 0);
        this.eveningDespawnTimeTicks = valueInput.getIntOr("DespawnTimeTicks", this.chooseEveningDespawnTime());
        this.isHungry = valueInput.getBooleanOr("IsHungry", false);
        this.isStayingOvernight = valueInput.getBooleanOr("IsStayingOvernight", false);
    }
}
