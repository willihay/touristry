package org.bensam.touristry.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.bensam.touristry.ModEntities;
import org.bensam.touristry.Touristry;
import org.bensam.touristry.block.entity.TouristBeaconBlockEntity;
import org.bensam.touristry.entity.goal.MoveToBeaconGoal;
import org.bensam.touristry.tourism.TourismManager;
import org.bensam.touristry.tourism.VisitResult;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class TouristEntity extends AbstractVillager {
    // persisted data
    private BlockPos beaconTarget;
    private double closestDistanceToBeacon;
    private int consecutiveFailedProgressChecks;
    private boolean reportedHurtEnRoute;
    private boolean reportedHurtOnPremises;
    private TouristState state;

    private boolean registeredWithTourismManager;

    public TouristEntity(Level level) {
        this(ModEntities.TOURIST.get(), level);
    }

    public TouristEntity(EntityType<? extends TouristEntity> entityType, Level level) {
        super(entityType, level);
        this.beaconTarget = null;
        this.resetBeaconJourneyStats();
        this.state = TouristState.IDLE;
    }

    public static AttributeSupplier.Builder createTouristAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    protected void resetBeaconJourneyStats() {
        this.closestDistanceToBeacon = Double.MAX_VALUE;
        this.consecutiveFailedProgressChecks = 0;
        this.reportedHurtEnRoute = false;
        this.reportedHurtOnPremises = false;
    }

    //region Class Overrides
    @Override
    public void die(DamageSource damageSource) {
        if (this.level() instanceof ServerLevel serverLevel) {
            Component deathMessage = damageSource.getLocalizedDeathMessage(this);
            TouristBeaconBlockEntity beaconBlockEntity = TourismManager.getBeaconBlockEntity(serverLevel, this.beaconTarget);
            MutableComponent playerMessage = deathMessage.copy();

            if (beaconBlockEntity != null) {
                String beaconName = beaconBlockEntity.getPlainTextName();
                if (this.isAtBeacon()) {
                    playerMessage.append(Component.literal(" while at beacon "))
                            .append(Component.literal(beaconName));
                    this.sendMessageToNearbyPlayers(serverLevel, playerMessage);

                    beaconBlockEntity.rateVisit(VisitResult.KILLED_ON_PREMISES);
                } else {
                    playerMessage.append(Component.literal(" while travelling to beacon "))
                            .append(Component.literal(beaconName));
                    this.sendMessageToNearbyPlayers(serverLevel, playerMessage);

                    beaconBlockEntity.rateVisit(VisitResult.KILLED_EN_ROUTE);
                }
            } else {
                // TODO: Leave a pending VisitResult rating with the TourismManager for if/when the beacon returns.
                this.sendMessageToNearbyPlayers(serverLevel, playerMessage);
                Touristry.LOGGER.info(deathMessage.getString());
            }
        }

        super.die(damageSource);
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(@NonNull ServerLevel serverLevel, @NonNull AgeableMob ageableMob) {
        return null; // not applicable
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.VILLAGER_HURT;
    }

    @Override
    public boolean hurtServer(ServerLevel serverLevel, DamageSource damageSource, float f) {
        TouristBeaconBlockEntity beaconBlockEntity = TourismManager.getBeaconBlockEntity(this.level(), this.beaconTarget);
        if (this.isAtBeacon()) {
            if (!this.reportedHurtOnPremises) {
                if (beaconBlockEntity != null) {
                    // Send hurt message to nearby players and log it.
                    String beaconName = beaconBlockEntity.getPlainTextName();
                    MutableComponent playerMessage = getHurtMessage(damageSource);
                    playerMessage.append(Component.literal(" while at beacon " + beaconName));
                    this.sendMessageToNearbyPlayers(serverLevel, playerMessage);
                    Touristry.LOGGER.info(playerMessage.getString());

                    beaconBlockEntity.rateVisit(VisitResult.HURT_ON_PREMISES);
                } else {
                    // TODO: Leave a pending VisitResult rating with the TourismManager for if/when the beacon returns.
                }
                this.reportedHurtOnPremises = true;
            }
        } else {
            if (!this.reportedHurtEnRoute) {
                if (beaconBlockEntity != null) {
                    // Send hurt message to nearby players and log it.
                    String beaconName = beaconBlockEntity.getPlainTextName();
                    MutableComponent playerMessage = getHurtMessage(damageSource);
                    playerMessage.append(Component.literal(" while travelling to beacon " + beaconName));
                    this.sendMessageToNearbyPlayers(serverLevel, playerMessage);
                    Touristry.LOGGER.info(playerMessage.getString());

                    beaconBlockEntity.rateVisit(VisitResult.HURT_EN_ROUTE);
                } else {
                    // TODO: Leave a pending VisitResult rating with the TourismManager for if/when the beacon returns.
                }
                this.reportedHurtEnRoute = true;
            }
        }

        return super.hurtServer(serverLevel, damageSource, f);
    }

    public MutableComponent getHurtMessage(DamageSource damageSource) {
        MutableComponent hurtMessage = this.getDisplayName().copy();
        if (damageSource.getEntity() == null && damageSource.getDirectEntity() == null) {
            return hurtMessage.append(Component.literal(" hurt"));
        } else {
            Component hurtBy = damageSource.getEntity() == null ? damageSource.getDirectEntity().getDisplayName() : damageSource.getEntity().getDisplayName();
            return hurtMessage.append(Component.literal(" hurt by ")).append(hurtBy);
        }
    }

    @Override
    public void onRemoval(Entity.@NonNull RemovalReason removalReason) {
        TourismManager.unregisterTourist(this);
        this.registeredWithTourismManager = false;
        super.onRemoval(removalReason);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MoveToBeaconGoal(this, 1.0));
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

        if (beaconTarget != null) {
            valueOutput.store("BeaconTarget", BlockPos.CODEC, this.beaconTarget);
        }
        valueOutput.store("State", TouristState.CODEC, this.state);
        valueOutput.putDouble("ClosestDistanceToBeacon", this.closestDistanceToBeacon);
        valueOutput.putInt("FailedProgressChecks", this.consecutiveFailedProgressChecks);
        valueOutput.putBoolean("ReportedHurtEnRoute", this.reportedHurtEnRoute);
        valueOutput.putBoolean("ReportedHurtOnPremises", this.reportedHurtOnPremises);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput valueInput) {
        super.readAdditionalSaveData(valueInput);

        this.beaconTarget = valueInput.read("BeaconTarget", BlockPos.CODEC).orElse(null);
        this.state = valueInput.read("State", TouristState.CODEC).orElse(
                this.isAtBeacon() ? TouristState.AT_BEACON : TouristState.TRAVELLING
        );
        this.closestDistanceToBeacon = valueInput.getDoubleOr("ClosestDistanceToBeacon", Double.MAX_VALUE);
        this.consecutiveFailedProgressChecks = valueInput.getIntOr("FailedProgressChecks", 0);
        this.reportedHurtEnRoute = valueInput.getBooleanOr("ReportedHurtEnRoute", false);
        this.reportedHurtOnPremises = valueInput.getBooleanOr("ReportedHurtOnPremises", false);
    }
    //endregion

    //region State Management
    private void transitionTo(TouristState newState) {

    }

    public TouristState getTouristState() {
        return state;
    }

    public void beginJourney(BlockPos beaconTarget) {
        this.beaconTarget = beaconTarget.immutable();
        this.resetBeaconJourneyStats();
        this.state = TouristState.TRAVELLING;
    }

    public void arriveAtBeacon() {
        if (this.state == TouristState.AT_BEACON) {
            return;
        }
        this.state = TouristState.AT_BEACON;
        this.reactToJourney();
    }

    public void markLost() {
        this.state = TouristState.LOST;

        if (this.level() instanceof ServerLevel serverLevel) {
            // Send navigation failure message to nearby players and log it.
            TouristBeaconBlockEntity beaconBlockEntity = TourismManager.getBeaconBlockEntity(serverLevel, this.beaconTarget);
            String beaconName = beaconBlockEntity != null ? beaconBlockEntity.getPlainTextName() : "unknown destination";
            MutableComponent playerMessage = this.getDisplayName().copy()
                    .append(Component.literal(" got lost travelling to " + beaconName));
            this.sendMessageToNearbyPlayers(serverLevel, playerMessage);
            Touristry.LOGGER.info(playerMessage.getString());

            if (beaconBlockEntity != null) {
                beaconBlockEntity.rateVisit(VisitResult.LOST);
            }

            serverLevel.playSound(
                    this,
                    this.getX(),
                    this.getY(),
                    this.getZ(),
                    SoundEvents.VILLAGER_NO,
                    SoundSource.NEUTRAL,
                    1.0f,
                    1.0f
            );
        }

        this.completeVisit();
    }

    public void completeVisit() {
        this.state = TouristState.FINISHED;

        if (this.level() instanceof ServerLevel serverLevel) {
            TourismManager.unregisterTourist(this);
            this.discard();
        }
    }
    //endregion

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            if (TourismManager.shouldForceDespawn(this)) {
                TourismManager.unregisterTourist(this);
                this.discard();
                return;
            }

            if (!this.registeredWithTourismManager) {
                TourismManager.registerTourist(this);
                this.registeredWithTourismManager = true;
            }
        }
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

    public boolean isAtBeacon() {
        return this.state == TouristState.AT_BEACON;
    }

    public void reportProgressTowardsBeaconTarget(double closestDistanceToBeacon, int consecutiveFailedProgressChecks) {
        this.closestDistanceToBeacon = closestDistanceToBeacon;
        this.consecutiveFailedProgressChecks = consecutiveFailedProgressChecks;
    }

    private void reactToJourney() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        BlockPos beaconTarget = this.getBeaconTarget();
        if (beaconTarget == null) {
            return;
        }

        TouristBeaconBlockEntity beaconBlockEntity = TourismManager.getBeaconBlockEntity(serverLevel, beaconTarget);
        if (beaconBlockEntity != null) {
            if (!beaconBlockEntity.isOpenForBusiness()) {
                beaconBlockEntity.rateVisit(VisitResult.CLOSED_ON_ARRIVAL);
                serverLevel.playSound(
                        this,
                        this.getX(),
                        this.getY(),
                        this.getZ(),
                        SoundEvents.VILLAGER_NO,
                        SoundSource.NEUTRAL,
                        1.0f,
                        1.0f
                );
                return;
            }

            if (beaconBlockEntity.tryDepositItem(new ItemStack(Items.EMERALD))) {
                beaconBlockEntity.rateVisit(VisitResult.GOOD);
                serverLevel.playSound(
                        this,
                        this.getX(),
                        this.getY(),
                        this.getZ(),
                        SoundEvents.VILLAGER_CELEBRATE,
                        SoundSource.NEUTRAL,
                        1.0f,
                        1.0f
                );
            } else {
                serverLevel.playSound(
                        this,
                        this.getX(),
                        this.getY(),
                        this.getZ(),
                        SoundEvents.VILLAGER_TRADE,
                        SoundSource.NEUTRAL,
                        1.0f,
                        1.0f
                );
            }
        } else {
            // No beacon found at beaconTarget!
            if (serverLevel == serverLevel.getServer().overworld()) {
                // TODO: Leave a pending VisitResult.CLOSED_ON_ARRIVAL rating with the TourismManager for if/when the beacon returns.
                serverLevel.playSound(
                        this,
                        this.getX(),
                        this.getY(),
                        this.getZ(),
                        SoundEvents.VILLAGER_NO,
                        SoundSource.NEUTRAL,
                        1.0f,
                        1.0f
                );
            } else {
                // Tourist got teleported or pushed into another dimension? They're probably confused.
                serverLevel.playSound(
                        this,
                        this.getX(),
                        this.getY(),
                        this.getZ(),
                        SoundEvents.VILLAGER_AMBIENT,
                        SoundSource.NEUTRAL,
                        1.0f,
                        1.0f
                );
            }
        }
    }

    public boolean hasCompletedVisit() {
        return this.state == TouristState.AT_BEACON || this.state == TouristState.FINISHED;
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
