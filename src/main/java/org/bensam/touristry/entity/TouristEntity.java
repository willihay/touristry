package org.bensam.touristry.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
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
import org.bensam.touristry.block.entity.TouristBeaconBlockEntity;
import org.bensam.touristry.entity.goal.MoveToBeaconGoal;
import org.bensam.touristry.tourism.TourismManager;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class TouristEntity extends AbstractVillager {
    private BlockPos beaconTarget;
    private boolean visitCompleted;
    private boolean registeredWithTourismManager;

    public TouristEntity(Level level) {
        this(ModEntities.TOURIST.get(), level);
    }

    public TouristEntity(EntityType<? extends TouristEntity> entityType, Level level) {
        super(entityType, level);
        this.visitCompleted = false;
    }

    public static AttributeSupplier.Builder createTouristAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.35)
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    //region Class Overrides
    @Override
    public @Nullable AgeableMob getBreedOffspring(@NonNull ServerLevel serverLevel, @NonNull AgeableMob ageableMob) {
        return null; // not applicable
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
        valueOutput.putBoolean("VisitCompleted", visitCompleted);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput valueInput) {
        super.readAdditionalSaveData(valueInput);

        this.beaconTarget = valueInput.read("BeaconTarget", BlockPos.CODEC).orElse(null);
        this.visitCompleted = valueInput.getBooleanOr("VisitCompleted", false);
    }
    //endregion

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide() && !this.registeredWithTourismManager) {
            TourismManager.registerTourist(this);
            this.registeredWithTourismManager = true;
        }
    }

    public @Nullable BlockPos getBeaconTarget() {
        return this.beaconTarget;
    }

    public void setBeaconTarget(BlockPos beaconTarget) {
        this.beaconTarget = beaconTarget;
    }

    public void onArrivedAtBeacon() {
        if (this.visitCompleted) {
            return;
        }

        this.visitCompleted = true;
        this.completeVisit();
    }

    private void completeVisit() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (serverLevel != serverLevel.getServer().overworld()) {
            return;
        }

        BlockPos beaconTarget = this.getBeaconTarget();
        if (beaconTarget != null && serverLevel.getBlockEntity(beaconTarget) instanceof TouristBeaconBlockEntity touristBeaconBlockEntity) {
            if (touristBeaconBlockEntity.tryDepositItem(new ItemStack(Items.EMERALD))) {
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
        }
    }

    public boolean hasCompletedVisit() {
        return this.visitCompleted;
    }
}
