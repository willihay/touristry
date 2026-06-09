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
import org.bensam.touristry.tourism.TourismManager;
import org.bensam.touristry.tourism.VisitResult;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

public final class TouristMind {
    private static final double MIN_MOOD = -3.0;
    private static final double MAX_MOOD = 4.0;
    private static final int MIN_ACTIVITY_INTERVAL_TICKS = 500;
    private static final int MAX_ACTIVITY_INTERVAL_TICKS = 2000;
    private static final int CHECK_MOOD_INTERVAL_TICKS = 250;

    private final TouristEntity tourist;

    // persisted data
    private TouristState state;
    private BlockPos beaconTarget;
    private BlockPos experienceTarget;
    private double closestDistanceToBeacon;
    private int consecutiveFailedProgressChecks;
    private boolean reportedHurtEnRoute;
    private boolean reportedHurtOnPremises;
    private double mood;
    private int goodExperiencesToday;
    private int eveningDespawnTimeTicks;
    private boolean isHungry;
    private boolean isStayingOvernight;

    private int nextChooseActivityTicks;
    private int nextMoodCheckTicks;

    public TouristMind(TouristEntity tourist) {
        this.tourist = tourist;
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
    }

    public boolean avoidWater() {
        return this.state == TouristState.WANDERING_AT_BEACON;
    }

    public @Nullable BlockPos getBeaconTarget() {
        return this.beaconTarget;
    }

    public double getClosestDistanceToBeacon() {
        return this.closestDistanceToBeacon;
    }

    public int getConsecutiveFailedProgressChecks() {
        return this.consecutiveFailedProgressChecks;
    }

    public TouristState getState() {
        return this.state;
    }

    public boolean hasReportedHurtEnRoute() {
        return this.reportedHurtEnRoute;
    }

    public boolean hasReportedHurtOnPremises() {
        return this.reportedHurtOnPremises;
    }

    public boolean isCurrentActivityAtBeacon() {
        return this.state == TouristState.CHOOSING_EXPERIENCE || this.state == TouristState.ENJOYING_EXPERIENCE
                || this.state == TouristState.WANDERING_AT_BEACON;
    }

    public boolean isPlanningActivity() {
        return this.state == TouristState.PLANNING_NEXT_MOVE || this.state == TouristState.CHOOSING_EXPERIENCE;
    }

    public boolean isTravellingToBeacon() {
        return this.state == TouristState.TRAVELLING_TO_BEACON;
    }

    public boolean isWandering() {
        return this.state == TouristState.WANDERING_AT_BEACON || this.state == TouristState.WANDERING_WORLD;
    }

    public void recordExperience(
            ServerLevel serverLevel,
            VisitResult result,
            boolean applyRatingToBeacon,
            boolean sendToNearbyPlayers,
            Component experienceMessage,
            boolean prependTouristName,
            boolean appendBeaconTargetName
    ) {
        SoundEvent soundEvent = null;

        switch (result) {
            case ARRIVED, GOOD, GREAT -> soundEvent = SoundEvents.VILLAGER_CELEBRATE;
            case LOST, CLOSED_ON_ARRIVAL, UNFAVORABLE -> soundEvent = SoundEvents.VILLAGER_NO;
            case HURT_EN_ROUTE -> this.reportedHurtEnRoute = true;
            case HURT_ON_PREMISES -> this.reportedHurtOnPremises = true;
        }

        this.mood = this.getUpdatedMood(result);

        this.tourist.applyExperienceToWorld(serverLevel, result, applyRatingToBeacon, sendToNearbyPlayers, experienceMessage, prependTouristName, appendBeaconTargetName, soundEvent);
    }

    public void recordProgressTowardsBeaconTarget(double closestDistanceToBeacon, int consecutiveFailedProgressChecks) {
        this.closestDistanceToBeacon = closestDistanceToBeacon;
        this.consecutiveFailedProgressChecks = consecutiveFailedProgressChecks;
    }

    public void tick(ServerLevel serverLevel) {
        if (this.state == TouristState.FINISHED || this.state == TouristState.SLEEPING) {
            return;
        }

        if (this.isTimeToDespawn()) {
            this.transitionTo(TouristState.DESPAWNING);
            return;
        }

        if (this.isTimeToCheckMood()) {
            if (this.nextMoodCheckTicks > 0) {
                this.nextMoodCheckTicks--;
            }

            if (this.nextMoodCheckTicks <= 0) {
                if (this.isInMoodToDespawn()) {
                    if (this.isCurrentActivityAtBeacon()) {
                        Component moodMessage = Component.literal("is in a bad mood and is leaving early from");
                        this.recordExperience(serverLevel, VisitResult.UNFAVORABLE, false, true, moodMessage, true, true);
                    } else {
                        Component moodMessage = Component.literal("is in a bad mood and is leaving early");
                        this.recordExperience(serverLevel, VisitResult.UNFAVORABLE, false, true, moodMessage, true, false);
                    }
                    this.transitionTo(TouristState.DESPAWNING);
                    return;
                }
                this.nextMoodCheckTicks = CHECK_MOOD_INTERVAL_TICKS;
            }
        }

        if (!this.isTravellingToBeacon() && !this.isPlanningActivity()) {
            if (this.nextChooseActivityTicks > 0) {
                this.nextChooseActivityTicks--;
            }

            if (this.nextChooseActivityTicks <= 0) {
                if (this.isTimeToCheckMood() && this.isInMoodToDespawn()) {
                    this.nextMoodCheckTicks = 0;
                    return;
                }

                if (this.isCurrentActivityAtBeacon()) {
                    Component moodMessage = Component.literal("had a good time at");
                    this.recordExperience(serverLevel, VisitResult.GOOD, true, false, moodMessage, true, true);
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

    private double getUpdatedMood(VisitResult result) {
        double positiveNormalized = Math.max(0.0, this.mood) / (MAX_MOOD + 1.0);
        double negativeNormalized = Math.max(0.0, -this.mood) / MAX_MOOD;

        double change = switch (result) {
            case ARRIVED, GOOD, GREAT -> {
                this.goodExperiencesToday++;
                VisitResult modifiedResult = this.goodExperiencesToday % 3 == 0 ? VisitResult.GREAT : result;
                yield modifiedResult.moodDelta() * (1.0 - positiveNormalized) * (1.0 + 0.5 * negativeNormalized);
            }
            case UNFAVORABLE, FAILED_SPAWN, LOST, CLOSED_ON_SPAWN, CLOSED_ON_ARRIVAL, HURT_EN_ROUTE, HURT_ON_PREMISES, KILLED_EN_ROUTE, KILLED_ON_PREMISES ->
                    result.moodDelta() * (0.75 + 0.5 * positiveNormalized);
        };

        return Mth.clamp(this.mood + change, MIN_MOOD, MAX_MOOD);
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

    private void resetBeaconJourneyStats() {
        this.closestDistanceToBeacon = Double.MAX_VALUE;
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
    }

    private void transitionTo(TouristState newState) {
        if (this.tourist.level().isClientSide()) {
            return;
        }

        if (this.state == newState) {
            return;
        }

        this.state = newState;
        // TODO: Always sync to client?

        TouristEntity.logActivity(Verbosity.MAJOR_EVENTS, "Transitioning state to " + newState);

        switch (newState) {
            case PLANNING_NEXT_MOVE -> this.planNextTarget();
            case CHOOSING_EXPERIENCE -> this.chooseExperienceOrWander();
            case WANDERING_WORLD -> this.beginWanderingWorld();
            case WANDERING_AT_BEACON -> this.beginWanderingAtBeacon();
            case DESPAWNING, LOST -> this.deSpawn();
            default -> {}
        }
    }

    //region State Transition Methods
    public void arriveAtBeacon() {
        if (!this.isTravellingToBeacon()) {
            return;
        }

        this.tourist.stopNavigation();

        if (this.tourist.level() instanceof ServerLevel serverLevel) {
            this.tourist.clearHeldItem();
            TouristState nextState = TouristState.PLANNING_NEXT_MOVE;
            BlockPos beaconTarget = this.beaconTarget;

            if (beaconTarget != null) {
                TouristBeaconBlockEntity beaconBlockEntity = TourismManager.getBeaconBlockEntity(tourist.level(), beaconTarget);

                if (beaconBlockEntity != null) {
                    if (beaconBlockEntity.isOpenForBusiness()) {
                        Component experienceMessage = Component.literal("arrived at");
                        this.recordExperience(serverLevel, VisitResult.ARRIVED, false, false, experienceMessage, true, true);
                        nextState = TouristState.CHOOSING_EXPERIENCE;
                    } else {
                        Component experienceMessage = Component.literal("found beacon closed at");
                        this.recordExperience(serverLevel, VisitResult.CLOSED_ON_ARRIVAL, true, true, experienceMessage, true, true);
                    }
                } else {
                    // No beacon found at beaconTarget!
                    Component experienceMessage = Component.literal("did not find a beacon at " + beaconTarget.toShortString());
                    this.recordExperience(serverLevel, VisitResult.LOST, true, true, experienceMessage, true, false);
                }
            }

            this.transitionTo(nextState);
        }
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

    private void chooseExperienceOrWander() {
        this.transitionTo(TouristState.WANDERING_AT_BEACON);
    }

    private void deSpawn() {
        this.tourist.onDespawn();
        this.transitionTo(TouristState.FINISHED);
    }

    public void onForcedDespawn() {
        this.transitionTo(TouristState.DESPAWNING);
    }

    public void onLost() {
        if (!this.isTravellingToBeacon()) {
            return;
        }

        this.tourist.stopNavigation();

        if (this.tourist.level() instanceof ServerLevel serverLevel) {
            Component experienceMessage = Component.literal("got lost travelling to");
            this.recordExperience(serverLevel, VisitResult.LOST, true, true, experienceMessage, true, true);
            this.tourist.playSound(SoundEvents.VILLAGER_NO);
        }

        this.transitionTo(TouristState.LOST);
    }

    public void onStoppedSleeping(ServerLevel serverLevel) {
        this.resetDailyStats();
        this.isHungry = true;

        int tickTimeOfDay = (int) (serverLevel.getDayTime() % 24000L);
        if (tickTimeOfDay <= 1000 && !this.reportedHurtOnPremises) {
            Component experienceMessage = Component.literal("woke up in a good mood at");
            this.recordExperience(serverLevel, VisitResult.GOOD, true, false, experienceMessage, true, true);
            this.transitionTo(TouristState.CHOOSING_EXPERIENCE);
        } else {
            Component experienceMessage = Component.literal("woke up abruptly at");
            this.recordExperience(serverLevel, VisitResult.UNFAVORABLE, true, false, experienceMessage, true, true);
            this.transitionTo(TouristState.DESPAWNING);
        }
    }

    private void planNextTarget() {
        List<TouristBeaconBlockEntity> closestBeacons;

        if (this.tourist.level() instanceof ServerLevel serverLevel) {
            double maxTravelDistanceToNextBeacon = ModServerConfigManager.getConfig().touristEntity().getMaxTravelDistanceToNextBeacon();
            double maxTravelDistanceToNextBeaconSqr = maxTravelDistanceToNextBeacon * maxTravelDistanceToNextBeacon;
            if (this.beaconTarget == null) {
                closestBeacons = TourismManager.getLoadedTouristBeaconsByDistance(
                        serverLevel,
                        this.tourist.blockPosition(),
                        beaconBlockEntity ->
                                beaconBlockEntity.isOpenForBusiness()
                                        && this.tourist.blockPosition().distSqr(beaconBlockEntity.getBlockPos()) <= maxTravelDistanceToNextBeaconSqr
                );
            } else {
                closestBeacons = TourismManager.getLoadedTouristBeaconsByDistance(
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
    }

    public void prepareForJourney(@NonNull BlockPos beaconTarget) {
        this.beaconTarget = beaconTarget.immutable();
        this.resetBeaconJourneyStats();
        this.tourist.giveItemToHold(new ItemStack(Items.MAP));
        this.transitionTo(TouristState.TRAVELLING_TO_BEACON);
    }
    //endregion

    public void addAdditionalSaveData(ValueOutput valueOutput) {
        valueOutput.store("State", TouristState.CODEC, this.state);
        if (this.beaconTarget != null) {
            valueOutput.store("BeaconTarget", BlockPos.CODEC, this.beaconTarget);
        }
        if (this.experienceTarget != null) {
            valueOutput.store("ExperienceTarget", BlockPos.CODEC, this.experienceTarget);
        }
        valueOutput.putDouble("ClosestDistanceToBeacon", this.closestDistanceToBeacon);
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
        this.beaconTarget = valueInput.read("BeaconTarget", BlockPos.CODEC).orElse(null);
        this.state = valueInput.read("State", TouristState.CODEC).orElse(
                (this.beaconTarget != null ? TouristState.TRAVELLING_TO_BEACON : TouristState.IDLE));
        this.experienceTarget = valueInput.read("ExperienceTarget", BlockPos.CODEC).orElse(null);
        this.closestDistanceToBeacon = valueInput.getDoubleOr("ClosestDistanceToBeacon", Double.MAX_VALUE);
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
