package org.bensam.touristry.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.bensam.touristry.ModEntities;
import org.bensam.touristry.Touristry;
import org.bensam.touristry.block.entity.TouristBeaconBlockEntity;
import org.bensam.touristry.config.ModServerConfigManager;
import org.bensam.touristry.config.Verbosity;
import org.bensam.touristry.entity.goal.*;
import org.bensam.touristry.tourism.TourismManager;
import org.bensam.touristry.tourism.VisitResult;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class TouristEntity extends AbstractVillager {
    private static final double MIN_MOOD = -3.0;
    private static final double MAX_MOOD = 4.0;
    private static final int MIN_ACTIVITY_INTERVAL_TICKS = 500;
    private static final int MAX_ACTIVITY_INTERVAL_TICKS = 2000;
    private static final int CHECK_MOOD_INTERVAL_TICKS = 250;

    // persisted data
    private TouristState state;
    private BlockPos beaconTarget;
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
    private boolean registeredWithTourismManager;

    public TouristEntity(Level level) {
        this(ModEntities.TOURIST.get(), level);
    }

    public TouristEntity(EntityType<? extends TouristEntity> entityType, Level level) {
        super(entityType, level);
        this.getNavigation().setCanOpenDoors(true);
        this.getNavigation().setCanFloat(true);
        this.getNavigation().setRequiredPathLength(48.0F);
        this.state = TouristState.IDLE;
        this.beaconTarget = null;
        this.resetBeaconJourneyStats();
        this.mood = this.chooseStartingMood();
        this.goodExperiencesToday = 0;
        this.nextChooseActivityTicks = this.chooseNextChooseActivityTicks();
        this.nextMoodCheckTicks = this.random.nextInt(CHECK_MOOD_INTERVAL_TICKS);
        this.eveningDespawnTimeTicks = this.chooseEveningDespawnTime();
        this.isHungry = false;
        this.isStayingOvernight = false;
    }

    public static AttributeSupplier.Builder createTouristAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    public static void logActivity(Verbosity verbosityLevel, String message) {
        Verbosity verbosityConfig = ModServerConfigManager.getConfig().touristEntity().getVerbosityLevel();
        if (verbosityLevel == Verbosity.ERRORS) {
            Touristry.LOGGER.error("[TouristEntity] {}", message);
        } else if (verbosityLevel.ordinal() <= verbosityConfig.ordinal()) {
            Touristry.LOGGER.info("[TouristEntity] {}", message);
        } else {
            Touristry.LOGGER.debug("[TouristEntity] {}", message);
        }
    }

    public static void logActivity(Verbosity verbosityLevel, String message, Object... args) {
        Verbosity verbosityConfig = ModServerConfigManager.getConfig().touristEntity().getVerbosityLevel();
        if (verbosityLevel == Verbosity.ERRORS) {
            Touristry.LOGGER.error("[TouristEntity] " + message, args);
        } else if (verbosityLevel.ordinal() <= verbosityConfig.ordinal()) {
            Touristry.LOGGER.info("[TouristEntity] " + message, args);
        } else {
            Touristry.LOGGER.debug("[TouristEntity] " + message, args);
        }
    }

    protected int chooseEveningDespawnTime() {
        return 12000 + this.random.nextInt(1000);
    }

    protected int chooseNextChooseActivityTicks() {
        return this.random.nextIntBetweenInclusive(MIN_ACTIVITY_INTERVAL_TICKS, MAX_ACTIVITY_INTERVAL_TICKS);
    }

    protected double chooseSpeedModifier() {
        return 0.75 + (0.5 * this.random.nextDouble());
    }

    protected double chooseStartingMood() {
        return 1.0 + this.random.nextDouble();
    }

    protected void resetBeaconJourneyStats() {
        this.closestDistanceToBeacon = Double.MAX_VALUE;
        this.consecutiveFailedProgressChecks = 0;
        this.reportedHurtEnRoute = false;
        this.reportedHurtOnPremises = false;
    }

    protected void resetDailyStats() {
        this.mood = this.chooseStartingMood();
        this.goodExperiencesToday = 0;
        this.nextMoodCheckTicks = this.random.nextInt(CHECK_MOOD_INTERVAL_TICKS);
        this.eveningDespawnTimeTicks = 12000 + this.random.nextInt(1000);
        this.isStayingOvernight = false;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MoveToBeaconGoal(this)); // MOVE
        this.goalSelector.addGoal(2, new TouristRandomStrollGoal(this, 0.6)); // MOVE
        this.goalSelector.addGoal(3, new TouristLookAtPlayerGoal(this, Player.class, 12.0f, 0.02f)); // LOOK
        this.goalSelector.addGoal(4, new LookAtTargetPosGoal(this)); // LOOK
        this.goalSelector.addGoal(5, new TouristLookAtPlayerGoal(this, AbstractVillager.class, 8.0f, 0.02f)); // LOOK
        this.goalSelector.addGoal(6, new TouristLookAtPlayerGoal(this, Animal.class, 8.0f, 0.01f)); // LOOK
        this.goalSelector.addGoal(7, new TouristRandomLookAroundGoal(this)); // LOOK
    }

    @Override
    public void die(DamageSource damageSource) {
        if (this.level() instanceof ServerLevel) {
            Component deathMessage = damageSource.getLocalizedDeathMessage(this);

            if (this.isCurrentActivityAtBeacon()) {
                Component experienceMessage = deathMessage.copy().append(Component.literal(" while at"));
                this.rateExperience(VisitResult.KILLED_ON_PREMISES, true, true, experienceMessage, false, true);
            } else if (this.isTravellingToBeacon()) {
                Component experienceMessage = deathMessage.copy().append(Component.literal(" while travelling to"));
                this.rateExperience(VisitResult.KILLED_EN_ROUTE, true, true, experienceMessage, false, true);
            }
        }

        super.die(damageSource);
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(@NonNull ServerLevel serverLevel, @NonNull AgeableMob ageableMob) {
        return null; // not applicable
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.VILLAGER_DEATH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.VILLAGER_HURT;
    }

    @Override
    public boolean hurtServer(ServerLevel serverLevel, DamageSource damageSource, float f) {
        if (this.isCurrentActivityAtBeacon()) {
            if (!this.reportedHurtOnPremises) {
                Component experienceMessage = getHurtMessage(damageSource).append(Component.literal(" while at"));
                this.rateExperience(VisitResult.HURT_ON_PREMISES, true, true, experienceMessage, true, true);
                this.reportedHurtOnPremises = true;
            }
        } else if (this.isTravellingToBeacon()) {
            if (!this.reportedHurtEnRoute) {
                Component experienceMessage = getHurtMessage(damageSource).append(Component.literal(" while travelling to"));
                this.rateExperience(VisitResult.HURT_EN_ROUTE, true, true, experienceMessage, true, true);
                this.reportedHurtEnRoute = true;
            }
        }

        return super.hurtServer(serverLevel, damageSource, f);
    }

    public MutableComponent getHurtMessage(DamageSource damageSource) {
        if (damageSource.getEntity() == null && damageSource.getDirectEntity() == null) {
            return Component.literal("hurt");
        } else {
            Component hurtBy = damageSource.getEntity() == null ? damageSource.getDirectEntity().getDisplayName() : damageSource.getEntity().getDisplayName();
            return Component.literal("hurt by ").append(hurtBy);
        }
    }

    @Override
    public void onRemoval(Entity.@NonNull RemovalReason removalReason) {
        TourismManager.unregisterTourist(this);
        this.registeredWithTourismManager = false;
        super.onRemoval(removalReason);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false; // prevents entity from de-spawning
    }

    @Override
    protected void rewardTradeXp(@NonNull MerchantOffer merchantOffer) {
        // not applicable
    }

    @Override
    protected void updateTrades(@NonNull ServerLevel serverLevel) {
        // not applicable
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput valueOutput) {
        super.addAdditionalSaveData(valueOutput);

        valueOutput.store("State", TouristState.CODEC, this.state);
        if (beaconTarget != null) {
            valueOutput.store("BeaconTarget", BlockPos.CODEC, this.beaconTarget);
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

    @Override
    protected void readAdditionalSaveData(ValueInput valueInput) {
        super.readAdditionalSaveData(valueInput);

        this.beaconTarget = valueInput.read("BeaconTarget", BlockPos.CODEC).orElse(null);
        this.state = valueInput.read("State", TouristState.CODEC).orElse(
                (this.beaconTarget != null ? TouristState.TRAVELLING_TO_BEACON : TouristState.IDLE));
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

    //region State Management
    private void transitionTo(TouristState newState) {
        if (this.level().isClientSide()) {
            return;
        }

        if (this.state == newState) {
            return;
        }

        this.state = newState;
        // TODO: Always sync to client?

        logActivity(Verbosity.MAJOR_EVENTS, "Transitioning state to " + newState);

        switch (newState) {
            case PLANNING_NEXT_MOVE -> this.planNextTarget();
            case CHOOSING_EXPERIENCE -> this.chooseExperienceOrWander();
            case WANDERING_WORLD -> this.beginWanderingWorld();
            case WANDERING_AT_BEACON -> this.beginWanderingAtBeacon();
            case DESPAWNING, LOST -> this.deSpawn();
            default -> {}
        }
    }

    public TouristState getTouristState() {
        return state;
    }

    public void prepareForJourney(@NonNull BlockPos beaconTarget, boolean withMap) {
        this.beaconTarget = beaconTarget.immutable();
        this.resetBeaconJourneyStats();
        if (withMap) {
            this.giveItemToHold(new ItemStack(Items.MAP));
        }
        this.transitionTo(TouristState.TRAVELLING_TO_BEACON);
    }

    public void arriveAtBeacon() {
        if (!this.isTravellingToBeacon()) {
            return;
        }

        this.getNavigation().stop();

        if (this.level() instanceof ServerLevel) {
            this.clearItemHeld();
            TouristState nextState = TouristState.PLANNING_NEXT_MOVE;
            BlockPos beaconTarget = this.beaconTarget;

            if (beaconTarget != null) {
                TouristBeaconBlockEntity beaconBlockEntity = TourismManager.getBeaconBlockEntity(this.level(), beaconTarget);

                if (beaconBlockEntity != null) {
                    if (beaconBlockEntity.isOpenForBusiness()) {
                        Component experienceMessage = Component.literal("arrived at");
                        // TODO: Set applyRatingToBeacon to false when experiences are available. Arriving shouldn't count as a successful visit.
                        this.rateExperience(VisitResult.ARRIVED, true, false, experienceMessage, true, true);
                        this.playSound(SoundEvents.VILLAGER_CELEBRATE);
                        nextState = TouristState.CHOOSING_EXPERIENCE;
                    } else {
                        Component experienceMessage = Component.literal("found beacon closed at");
                        this.rateExperience(VisitResult.CLOSED_ON_ARRIVAL, true, true, experienceMessage, true, true);
                        this.playSound(SoundEvents.VILLAGER_NO);
                    }
                } else {
                    // No beacon found at beaconTarget!
                    Component experienceMessage = Component.literal("did not find a beacon at " + beaconTarget.toShortString());
                    this.rateExperience(VisitResult.LOST, true, true, experienceMessage, true, false);
                    this.playSound(SoundEvents.VILLAGER_NO);
                }
            }

            this.transitionTo(nextState);
        }
    }

    public void markLost() {
        if (!this.isTravellingToBeacon()) {
            return;
        }

        this.getNavigation().stop();

        if (this.level() instanceof ServerLevel) {
            Component experienceMessage = Component.literal("got lost travelling to");
            this.rateExperience(VisitResult.LOST, true, true, experienceMessage, true, true);
            this.playSound(SoundEvents.VILLAGER_NO);
        }

        this.transitionTo(TouristState.LOST);
    }

    protected void planNextTarget() {
        List<TouristBeaconBlockEntity> closestBeacons;

        if (this.level() instanceof ServerLevel serverLevel) {
            double maxTravelDistanceToNextBeacon = ModServerConfigManager.getConfig().touristEntity().getMaxTravelDistanceToNextBeacon();
            double maxTravelDistanceToNextBeaconSqr = maxTravelDistanceToNextBeacon * maxTravelDistanceToNextBeacon;
            if (beaconTarget == null) {
                closestBeacons = TourismManager.getLoadedTouristBeaconsByDistance(
                        serverLevel,
                        this.blockPosition(),
                        beaconBlockEntity ->
                                beaconBlockEntity.isOpenForBusiness()
                                && this.blockPosition().distSqr(beaconBlockEntity.getBlockPos()) <= maxTravelDistanceToNextBeaconSqr
                );
            } else {
                closestBeacons = TourismManager.getLoadedTouristBeaconsByDistance(
                        serverLevel,
                        this.beaconTarget,
                        beaconBlockEntity ->
                                !beaconBlockEntity.getBlockPos().equals(this.beaconTarget)
                                && beaconBlockEntity.isOpenForBusiness()
                                && this.blockPosition().distSqr(beaconBlockEntity.getBlockPos()) <= maxTravelDistanceToNextBeaconSqr
                );
            }

            int numBeaconsCloseBy = closestBeacons.size();
            if (numBeaconsCloseBy > 0) {
                int beaconIndex = 0;
                if (numBeaconsCloseBy > 1) {
                    beaconIndex = this.random.nextInt(numBeaconsCloseBy);
                }

                TouristBeaconBlockEntity beaconBlockEntity = closestBeacons.get(beaconIndex);
                this.prepareForJourney(beaconBlockEntity.getBlockPos(), true);
                return;
            }

            this.transitionTo(TouristState.WANDERING_WORLD);
        }
    }

    protected void chooseExperienceOrWander() {
        this.transitionTo(TouristState.WANDERING_AT_BEACON);
    }

    protected void beginWanderingAtBeacon() {
        this.clearItemHeld();
        this.nextChooseActivityTicks = this.chooseNextChooseActivityTicks();
    }

    protected void beginWanderingWorld() {
        this.beaconTarget = null;
        this.resetBeaconJourneyStats();
        this.clearItemHeld();
        this.nextChooseActivityTicks = this.chooseNextChooseActivityTicks();
    }

    @Override
    public void stopSleeping() {
        super.stopSleeping();

        this.resetDailyStats();
        this.isHungry = true;

        if (this.level().isClientSide()) {
            return;
        }

        int tickTimeOfDay = (int) (this.level().getDayTime() % 24000L);
        if (tickTimeOfDay <= 1000 && !this.reportedHurtOnPremises) {
            Component experienceMessage = Component.literal("woke up in a good mood at");
            this.rateExperience(VisitResult.GOOD, true, false, experienceMessage, true, true);
            this.playSound(SoundEvents.VILLAGER_CELEBRATE);
            this.transitionTo(TouristState.CHOOSING_EXPERIENCE);
        } else {
            Component experienceMessage = Component.literal("woke up abruptly at");
            this.rateExperience(VisitResult.UNFAVORABLE, true, false, experienceMessage, true, true);
            this.playSound(SoundEvents.VILLAGER_NO);
            this.transitionTo(TouristState.DESPAWNING);
        }
    }

    public void deSpawn() {
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ParticleTypes.PORTAL,
                    this.getRandomX(0.5),
                    this.getRandomY(),
                    this.getRandomZ(0.5),
                    15, // # of particles
                    0.3, 0.4, 0.3, // spread
                    0.02 // particle speed
            );

            this.playSound(SoundEvents.ENDERMAN_TELEPORT);

            TourismManager.unregisterTourist(this);
            this.discard();
        }

        this.transitionTo(TouristState.FINISHED);
    }
    //endregion

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide()) {
            return;
        }

        if (this.state == TouristState.FINISHED) {
            return;
        }

        if (TourismManager.shouldForceDespawn(this)) {
            this.transitionTo(TouristState.DESPAWNING);
            return;
        }

        if (!this.registeredWithTourismManager) {
            TourismManager.registerTourist(this);
            this.registeredWithTourismManager = true;
        }

        if (this.state == TouristState.SLEEPING) {
            return;
        }

        if (this.isTimeToDespawn()) {
            this.transitionTo(TouristState.DESPAWNING);
        }

        if (this.isTimeToCheckMood()) {
            if (this.nextMoodCheckTicks > 0) {
                this.nextMoodCheckTicks--;
            }

            if (this.nextMoodCheckTicks <= 0) {
                if (this.isInMoodToDespawn()) {
                    if (this.isCurrentActivityAtBeacon()) {
                        Component moodMessage = Component.literal("is in a bad mood and is leaving early from");
                        this.rateExperience(VisitResult.UNFAVORABLE, false, true, moodMessage, true, true);
                    } else {
                        Component moodMessage = Component.literal("is in a bad mood and is leaving early");
                        this.rateExperience(VisitResult.UNFAVORABLE, false, true, moodMessage, true, false);
                    }
                    this.playSound(SoundEvents.VILLAGER_NO);
                    this.transitionTo(TouristState.DESPAWNING);
                }
                this.nextMoodCheckTicks = CHECK_MOOD_INTERVAL_TICKS;
            }
        }

        if (!this.isTravellingToBeacon() && !this.isPlanningActivity()) {
            if (this.nextChooseActivityTicks > 0) {
                this.nextChooseActivityTicks--;
            }

            if (this.nextChooseActivityTicks <= 0) {
                this.transitionTo(TouristState.PLANNING_NEXT_MOVE);
            }
        }
    }

    protected void rateExperience(
            VisitResult result,
            boolean applyRatingToBeacon,
            boolean sendToNearbyPlayers,
            Component experienceMessage,
            boolean prependTouristName,
            boolean appendBeaconTargetName) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        TouristBeaconBlockEntity beaconBlockEntity = null;
        String beaconName = "unknown beacon";

        if (this.beaconTarget != null) {
            beaconBlockEntity = TourismManager.getBeaconBlockEntity(serverLevel, this.beaconTarget);
            if (beaconBlockEntity != null) {
                beaconName = beaconBlockEntity.getPlainTextName();
            }
        }

        if (applyRatingToBeacon) {
            if (beaconBlockEntity != null) {
                beaconBlockEntity.rateVisit(result);
            } else {
                // TODO: Leave a pending VisitResult rating with the TourismManager for if/when the beacon returns.
            }
        }

        this.mood = this.getUpdatedMood(result);

        MutableComponent message = prependTouristName ? this.getDisplayName().copy().append(" ") : Component.empty();
        if (experienceMessage != null) {
            message.append(experienceMessage);
            if (appendBeaconTargetName) {
                message.append(Component.literal(" " + beaconName));
            }
        } else {
            message.append(Component.literal("logged a " + result.name() + " experience from " + this.blockPosition().toShortString()));
            if (appendBeaconTargetName) {
                message.append(Component.literal(" for " + beaconName));
            }
        }

        if (sendToNearbyPlayers) {
            this.sendMessageToNearbyPlayers(serverLevel, message);
        }

        logActivity(Verbosity.LEVEL_2_DIAGNOSTICS, message.getString());
    }

    public boolean avoidWater() {
        return this.state == TouristState.WANDERING_AT_BEACON;
    }

    protected void clearItemHeld() {
        this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
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

    protected double getUpdatedMood(VisitResult result) {
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

    protected void giveItemToHold(ItemStack itemStack) {
        this.setItemSlot(EquipmentSlot.MAINHAND, itemStack);
    }

    public boolean isCurrentActivityAtBeacon() {
        return this.state == TouristState.CHOOSING_EXPERIENCE || this.state == TouristState.ENJOYING_EXPERIENCE
                || this.state == TouristState.WANDERING_AT_BEACON;
    }

    protected boolean isInMoodToDespawn() {
        if (mood >= 0) {
            return false;
        }

        if (mood <= MIN_MOOD) {
            return true;
        }

        return this.random.nextDouble() < (mood / MIN_MOOD);
    }

    protected boolean isPlanningActivity() {
        return this.state == TouristState.PLANNING_NEXT_MOVE || this.state == TouristState.CHOOSING_EXPERIENCE;
    }

    protected boolean isTimeToCheckMood() {
        long dayTime = this.level().getDayTime();
        int tickTimeOfDay = (int) (dayTime % 24000L);
        return !this.isSleeping() && tickTimeOfDay >= 6000;
    }

    protected boolean isTimeToDespawn() {
        long dayTime = this.level().getDayTime();
        int tickTimeOfDay = (int) (dayTime % 24000L);
        return !this.isStayingOvernight && tickTimeOfDay >= this.eveningDespawnTimeTicks;
    }

    public boolean isTravellingToBeacon() {
        return this.state == TouristState.TRAVELLING_TO_BEACON;
    }

    public boolean isWandering() {
        return this.state == TouristState.WANDERING_AT_BEACON || this.state == TouristState.WANDERING_WORLD;
    }

    public void reportProgressTowardsBeaconTarget(double closestDistanceToBeacon, int consecutiveFailedProgressChecks) {
        this.closestDistanceToBeacon = closestDistanceToBeacon;
        this.consecutiveFailedProgressChecks = consecutiveFailedProgressChecks;
    }

    protected void sendMessageToNearbyPlayers(ServerLevel serverLevel, Component message) {
        double radiusSq = 70.0 * 70.0;

        for (ServerPlayer player : serverLevel.players()) {
            if (player.distanceToSqr(this) <= radiusSq) {
                player.sendSystemMessage(message);
            }
        }
    }
}
