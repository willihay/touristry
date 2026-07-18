package org.bensam.touristry.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.bensam.touristry.ModEntities;
import org.bensam.touristry.Touristry;
import org.bensam.touristry.block.entity.TouristBeaconBlockEntity;
import org.bensam.touristry.config.ModServerConfigManager;
import org.bensam.touristry.config.Verbosity;
import org.bensam.touristry.entity.goal.MoveToTargetGoal;
import org.bensam.touristry.entity.goal.TouristLookAtEntityGoal;
import org.bensam.touristry.entity.goal.TouristRandomLookAroundGoal;
import org.bensam.touristry.entity.goal.TouristRandomStrollGoal;
import org.bensam.touristry.tourism.TourismManager;
import org.bensam.touristry.tourism.TouristLocation;
import org.bensam.touristry.tourism.TouristReview;
import org.bensam.touristry.tourism.VisitResult;
import org.bensam.touristry.tourism.experience.TouristExperience;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class TouristEntity extends AbstractVillager {
    private final TouristMind mind;
    private boolean registeredWithTourismManager;

    public TouristEntity(Level level) {
        this(ModEntities.TOURIST.get(), level);
    }

    public TouristEntity(EntityType<? extends TouristEntity> entityType, Level level) {
        super(entityType, level);
        this.mind = new TouristMind(this);
        this.getNavigation().setCanOpenDoors(true);
        this.getNavigation().setCanFloat(true);
        this.getNavigation().setRequiredPathLength(48.0F);
    }

    private double chooseSpeedModifier() {
        return 0.75 + (0.5 * this.random.nextDouble());
    }

    public static AttributeSupplier.Builder createTouristAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MoveToTargetGoal(this)); // MOVE
        this.goalSelector.addGoal(2, new OpenDoorGoal(this, true));
        this.goalSelector.addGoal(4, new TouristRandomStrollGoal(this, 0.6)); // MOVE
        this.goalSelector.addGoal(4, new TouristLookAtEntityGoal(this, Player.class, 12.0f, 0.02f)); // LOOK
        this.goalSelector.addGoal(5, new TouristLookAtEntityGoal(this, AbstractVillager.class, 8.0f, 0.02f)); // LOOK
        this.goalSelector.addGoal(6, new TouristLookAtEntityGoal(this, Animal.class, 8.0f, 0.01f)); // LOOK
        this.goalSelector.addGoal(7, new TouristRandomLookAroundGoal(this)); // LOOK
    }

    public void addExperienceGoal(Goal goal) {
        this.goalSelector.addGoal(3, goal);
    }

    public void removeExperienceGoal(Goal goal) {
        this.goalSelector.removeGoal(goal);
    }

    public static void logActivity(Verbosity verbosityLevel, String message) {
        Verbosity verbosityConfig = ModServerConfigManager.getConfig().touristEntityConfig().getVerbosityLevel();
        if (verbosityLevel == Verbosity.ERRORS) {
            Touristry.LOGGER.error("[TouristEntity] {}", message);
        } else if (verbosityLevel.ordinal() <= verbosityConfig.ordinal()) {
            Touristry.LOGGER.info("[TouristEntity] {}", message);
        } else {
            Touristry.LOGGER.debug("[TouristEntity] {}", message);
        }
    }

    public static void logActivity(Verbosity verbosityLevel, String message, Object... args) {
        Verbosity verbosityConfig = ModServerConfigManager.getConfig().touristEntityConfig().getVerbosityLevel();
        if (verbosityLevel == Verbosity.ERRORS) {
            Touristry.LOGGER.error("[TouristEntity] " + message, args);
        } else if (verbosityLevel.ordinal() <= verbosityConfig.ordinal()) {
            Touristry.LOGGER.info("[TouristEntity] " + message, args);
        } else {
            Touristry.LOGGER.debug("[TouristEntity] " + message, args);
        }
    }

    public void applyExperienceToWorld(
            ServerLevel serverLevel,
            TouristReview review,
            @Nullable SoundEvent soundEvent
    ) {
        BlockPos reviewTargetPos = null;
        Component targetName = Component.literal("no specific target");

        if (review.reviewTarget() == TouristLocation.BEACON) {
            reviewTargetPos = this.getBeaconTarget();

            if (review.applyRatingToTarget()) {
                TouristBeaconBlockEntity beaconBlockEntity = TourismManager.getBeaconBlockEntity(serverLevel, reviewTargetPos);
                if (beaconBlockEntity != null) {
                    beaconBlockEntity.rateVisit(review.result(), serverLevel.getDayTime());
                } else {
                    // TODO: Leave a pending VisitResult rating with the TourismManager for if/when the beacon returns.
                }
            }
        } else if (review.reviewTarget() == TouristLocation.EXPERIENCE) {
            reviewTargetPos = this.getExperienceTarget();

            if (review.applyRatingToTarget()) {
                TouristExperience experience = TourismManager.getTouristExperienceByPos(reviewTargetPos);
                if (experience != null) {
                    experience.rateVisit(review.result(), serverLevel.getDayTime());
                } else {
                    // TODO: Leave a pending VisitResult rating with the TourismManager for if/when the experience returns.
                }
            }
        }

        if (reviewTargetPos != null) {
            targetName = TourismManager.getTouristBlockNameOrPos(serverLevel, review.reviewTarget(), reviewTargetPos);
        }

        MutableComponent message = review.prependTouristName() ? this.getDisplayName().copy().append(" ") : Component.empty();
        if (review.reviewMessage() != null) {
            message.append(review.reviewMessage());
            if (review.appendTargetNameOrPos()) {
                message.append(Component.literal(" ")).append(targetName);
            }
        } else {
            message.append(Component.literal("logged a " + review.result().name() + " experience from " + this.blockPosition().toShortString()));
            if (review.appendTargetNameOrPos()) {
                message.append(Component.literal(" for ")).append(targetName);
            }
        }

        if (review.announceToNearbyPlayers()) {
            this.sendMessageToNearbyPlayers(serverLevel, message);
        }

        if (soundEvent != null) {
            this.playSound(soundEvent);
        }

        logActivity(Verbosity.LEVEL_2_DIAGNOSTICS, message.getString());
    }

    public void clearHeldItem() {
        this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
    }

    @Override
    public void die(DamageSource damageSource) {
        if (this.level() instanceof ServerLevel serverLevel) {
            MutableComponent experienceMessage = damageSource.getLocalizedDeathMessage(this).copy();
            VisitResult visitResult = null;

            if (this.isTraveling()) {
                experienceMessage.append(Component.literal(" while travelling to"));
                visitResult = VisitResult.KILLED_EN_ROUTE;
            } else if (this.isAtTouristLocation()) {
                experienceMessage.append(Component.literal(" while at"));
                visitResult = VisitResult.KILLED_ON_PREMISES;
            }

            if (visitResult != null) {
                TouristReview review = new TouristReview(
                        this.mind.getState().reviewTarget(),
                        visitResult,
                        true,
                        true,
                        experienceMessage,
                        false,
                        true
                );
                this.mind.recordExperience(serverLevel, review);
            }
        }

        super.die(damageSource);
    }

    public @Nullable BlockPos getBeaconTarget() {
        return this.mind.getBeaconPos();
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(@NonNull ServerLevel serverLevel, @NonNull AgeableMob ageableMob) {
        return null; // not applicable
    }

    public double getClosestDistanceToTarget() {
        return this.mind.getClosestDistanceToDestination();
    }

    public int getConsecutiveFailedProgressChecks() {
        return this.mind.getConsecutiveFailedProgressChecks();
    }

    public String getCurrentLocationNameOrPos() {
        return this.mind.getLocationNameOrPos();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.VILLAGER_DEATH;
    }

    public @Nullable BlockPos getExperienceTarget() {
        return this.mind.getExperiencePos();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.VILLAGER_HURT;
    }

    public TouristMind getMind() {
        return this.mind;
    }

    public @Nullable BlockPos getMoveToTarget() {
        return this.mind.getMoveToTarget();
    }

    public String getMoveToTargetName() {
        return this.mind.getMoveToTargetName();
    }

    public int getTicksAtCurrentTarget() {
        return this.mind.getTicksAtCurrentTarget();
    }

    public void giveItemToHold(ItemStack itemStack) {
        this.setItemSlot(EquipmentSlot.MAINHAND, itemStack);
    }

    public boolean hasHeldItem() {
        return !(this.getMainHandItem().isEmpty());
    }

    @Override
    public boolean hurtServer(ServerLevel serverLevel, DamageSource damageSource, float f) {
        MutableComponent experienceMessage = getHurtMessage(damageSource);
        VisitResult visitResult = null;

        if (this.isTraveling()) {
            if (!this.mind.hasReportedHurtEnRoute()) {
                visitResult = VisitResult.HURT_EN_ROUTE;
                this.mind.updateMood(visitResult);
                experienceMessage.append(Component.literal(" while travelling to"));
            }
        } else if (this.isAtTouristLocation()) {
            if (!this.mind.hasReportedHurtOnPremises()) {
                this.mind.updateMood(VisitResult.HURT_ON_PREMISES);
                experienceMessage.append(Component.literal(" while at"));
            }
        }

        if (visitResult != null) {
            TouristReview review = new TouristReview(
                    this.mind.getState().reviewTarget(),
                    visitResult,
                    true,
                    true,
                    experienceMessage,
                    true,
                    true
            );
            this.mind.recordExperience(serverLevel, review);
        }

        return super.hurtServer(serverLevel, damageSource, f);
    }

    public boolean isAtTouristLocation() {
        return this.mind.getState().isAtTouristLocation();
    }

    public boolean isTraveling() {
        return this.mind.getState().isTraveling();
    }

    public boolean isWandering() {
        return this.mind.getState().isWandering();
    }

    public void onDespawn() {
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

    public void stopNavigation() {
        this.getNavigation().stop();
    }

    @Override
    public void stopSleeping() {
        super.stopSleeping();

        if (this.level() instanceof ServerLevel serverLevel) {
            this.mind.onStoppedSleeping(serverLevel);
        }
    }

    @Override
    protected void updateTrades(@NonNull ServerLevel serverLevel) {
        // not applicable
    }

    @Override
    public void tick() {
        super.tick();

        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (TourismManager.shouldForceDespawn(this)) {
            this.mind.onForcedDespawn();
            return;
        }

        if (!this.registeredWithTourismManager) {
            TourismManager.registerTourist(this);
            this.registeredWithTourismManager = true;
        }

        this.mind.tick(serverLevel);
    }

    private static MutableComponent getHurtMessage(DamageSource damageSource) {
        if (damageSource.getEntity() == null && damageSource.getDirectEntity() == null) {
            return Component.literal("hurt");
        } else {
            Component hurtBy = damageSource.getEntity() == null ? damageSource.getDirectEntity().getDisplayName() : damageSource.getEntity().getDisplayName();
            return Component.literal("hurt by ").append(hurtBy);
        }
    }

    private void sendMessageToNearbyPlayers(ServerLevel serverLevel, Component message) {
        double radiusSq = 70.0 * 70.0;

        for (ServerPlayer player : serverLevel.players()) {
            if (player.distanceToSqr(this) <= radiusSq) {
                player.sendSystemMessage(message);
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput valueOutput) {
        super.addAdditionalSaveData(valueOutput);
        this.mind.addAdditionalSaveData(valueOutput);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput valueInput) {
        super.readAdditionalSaveData(valueInput);
        this.mind.readAdditionalSaveData(valueInput);

        if (this.level() instanceof ServerLevel serverLevel) {
            this.mind.onEntityLoaded(serverLevel);
        }
    }
}
