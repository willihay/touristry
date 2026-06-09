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
import org.bensam.touristry.entity.goal.*;
import org.bensam.touristry.tourism.TourismManager;
import org.bensam.touristry.tourism.VisitResult;
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
        this.goalSelector.addGoal(1, new MoveToBeaconGoal(this)); // MOVE
        this.goalSelector.addGoal(2, new OpenDoorGoal(this, true));
        this.goalSelector.addGoal(3, new TouristRandomStrollGoal(this, 0.6)); // MOVE
        this.goalSelector.addGoal(3, new TouristLookAtPlayerGoal(this, Player.class, 12.0f, 0.02f)); // LOOK
        this.goalSelector.addGoal(4, new LookAtTargetPosGoal(this)); // LOOK
        this.goalSelector.addGoal(5, new TouristLookAtPlayerGoal(this, AbstractVillager.class, 8.0f, 0.02f)); // LOOK
        this.goalSelector.addGoal(6, new TouristLookAtPlayerGoal(this, Animal.class, 8.0f, 0.01f)); // LOOK
        this.goalSelector.addGoal(7, new TouristRandomLookAroundGoal(this)); // LOOK
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

    public void applyExperienceToWorld(
            ServerLevel serverLevel,
            VisitResult result,
            boolean applyRatingToBeacon,
            boolean sendToNearbyPlayers,
            Component experienceMessage,
            boolean prependTouristName,
            boolean appendBeaconTargetName,
            @Nullable SoundEvent soundEvent
    ) {
        BlockPos beaconTarget = this.getBeaconTarget();
        TouristBeaconBlockEntity beaconBlockEntity = null;
        String beaconName = "unknown beacon";

        if (beaconTarget != null) {
            beaconBlockEntity = TourismManager.getBeaconBlockEntity(serverLevel, beaconTarget);
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
            Component deathMessage = damageSource.getLocalizedDeathMessage(this);

            if (this.isCurrentActivityAtBeacon()) {
                Component experienceMessage = deathMessage.copy().append(Component.literal(" while at"));
                this.mind.recordExperience(serverLevel, VisitResult.KILLED_ON_PREMISES, true, true, experienceMessage, false, true);
            } else if (this.isTravellingToBeacon()) {
                Component experienceMessage = deathMessage.copy().append(Component.literal(" while travelling to"));
                this.mind.recordExperience(serverLevel, VisitResult.KILLED_EN_ROUTE, true, true, experienceMessage, false, true);
            }
        }

        super.die(damageSource);
    }

    public @Nullable BlockPos getBeaconTarget() {
        return this.mind.getBeaconTarget();
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(@NonNull ServerLevel serverLevel, @NonNull AgeableMob ageableMob) {
        return null; // not applicable
    }

    public double getClosestDistanceToBeacon() {
        return this.mind.getClosestDistanceToBeacon();
    }

    public int getConsecutiveFailedProgressChecks() {
        return this.mind.getConsecutiveFailedProgressChecks();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.VILLAGER_DEATH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.VILLAGER_HURT;
    }

    public TouristMind getMind() {
        return this.mind;
    }

    public void giveItemToHold(ItemStack itemStack) {
        this.setItemSlot(EquipmentSlot.MAINHAND, itemStack);
    }

    @Override
    public boolean hurtServer(ServerLevel serverLevel, DamageSource damageSource, float f) {
        if (this.isCurrentActivityAtBeacon()) {
            if (!this.mind.hasReportedHurtOnPremises()) {
                this.mind.updateMood(VisitResult.HURT_ON_PREMISES);
                Component experienceMessage = getHurtMessage(damageSource).append(Component.literal(" while at"));
                this.mind.recordExperience(serverLevel, VisitResult.HURT_ON_PREMISES, true, true, experienceMessage, true, true);
            }
        } else if (this.isTravellingToBeacon()) {
            if (!this.mind.hasReportedHurtEnRoute()) {
                this.mind.updateMood(VisitResult.HURT_EN_ROUTE);
                Component experienceMessage = getHurtMessage(damageSource).append(Component.literal(" while travelling to"));
                this.mind.recordExperience(serverLevel, VisitResult.HURT_EN_ROUTE, true, true, experienceMessage, true, true);
            }
        }

        return super.hurtServer(serverLevel, damageSource, f);
    }

    public boolean isCurrentActivityAtBeacon() {
        return this.mind.isCurrentActivityAtBeacon();
    }

    public boolean isTravellingToBeacon() {
        return this.mind.isTravellingToBeacon();
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
    }
}
