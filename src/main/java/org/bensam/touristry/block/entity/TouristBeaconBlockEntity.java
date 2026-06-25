package org.bensam.touristry.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.bensam.touristry.ModBlockEntities;
import org.bensam.touristry.ModComponents;
import org.bensam.touristry.Touristry;
import org.bensam.touristry.block.TouristBeaconBlock;
import org.bensam.touristry.menu.TouristBeaconMenu;
import org.bensam.touristry.tourism.TourismManager;
import org.bensam.touristry.tourism.TouristBeaconExperience;
import org.bensam.touristry.tourism.TouristBeaconStats;
import org.bensam.touristry.tourism.VisitResult;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

public class TouristBeaconBlockEntity extends BlockEntity implements MenuProvider, Nameable {
    private static final double MIN_REPUTATION = -100.0;
    private static final double MAX_REPUTATION = 100.0;
    public static final int DATA_REPUTATION = 0;
    public static final int DATA_OPEN_FOR_BUSINESS = 1;
    public static final int DATA_COUNT = 2;

    @Nullable private Component name;
    private UUID uuid = UUID.randomUUID();

    // stats
    private boolean openForBusiness;
    private int successfulVisits;
    private int closedEarly;
    private int failedSpawns;
    private int navFailures;
    private int touristsHurt;
    private int touristsKilled;
    private double reputation;

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            return switch (i) {
                case DATA_REPUTATION -> (int) Math.round(TouristBeaconBlockEntity.this.reputation * 100.0);
                case DATA_OPEN_FOR_BUSINESS -> TouristBeaconBlockEntity.this.openForBusiness ? 1 : 0;
                default -> throw new IndexOutOfBoundsException("Invalid container data index: " + i);
            };
        }

        @Override
        public void set(int i, int value) {
            switch (i) {
                case DATA_REPUTATION, DATA_OPEN_FOR_BUSINESS -> { /* ignore: synced display-only value */ }
                default -> throw new IndexOutOfBoundsException("Invalid container data index: " + i);
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public TouristBeaconBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(ModBlockEntities.TOURIST_BEACON.get(), blockPos, blockState);
        this.openForBusiness = false;
        this.successfulVisits = 0;
        this.closedEarly = 0;
        this.failedSpawns = 0;
        this.navFailures = 0;
        this.touristsHurt = 0;
        this.touristsKilled = 0;
        this.reputation = 0.0d;
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return new TouristBeaconMenu(i, inventory, this.data, ContainerLevelAccess.create(this.level, this.getBlockPos()));
    }

    public TouristBeaconExperience getBeaconExperience() {
        return new TouristBeaconExperience(this.openForBusiness);
    }

    public TouristBeaconStats getBeaconStats() {
        return new TouristBeaconStats(
                this.successfulVisits,
                this.closedEarly,
                this.failedSpawns,
                this.navFailures,
                this.touristsHurt,
                this.touristsKilled,
                this.reputation);
    }

    @Override
    public @Nullable Component getCustomName() {
        return this.name;
    }

    protected @NonNull Component getDefaultName() {
        return Component.translatable("block." + Touristry.MOD_ID + ".tourist_beacon");
    }

    @Override
    public @NonNull Component getDisplayName() {
        return this.getName();
    }

    @Override
    public @NonNull Component getName() {
        return this.name != null ? this.name : this.getDefaultName();
    }

    public UUID getUUID() {
        return this.uuid;
    }

    public boolean isOpenForBusiness() {
        return this.openForBusiness;
    }

    public void setOpenForBusiness(boolean openForBusiness) {
        this.openForBusiness = openForBusiness;
        BlockState blockState = this.getBlockState();
        if (blockState.hasProperty(TouristBeaconBlock.OPEN_FOR_BUSINESS) && this.level != null) {
            this.level.setBlockAndUpdate(this.getBlockPos(), blockState.setValue(TouristBeaconBlock.OPEN_FOR_BUSINESS, openForBusiness));
        }
        this.setChanged();
    }

    public void resetReputation() {
        this.reputation = 0.0d;
        this.setChanged();
    }

    public void resetAllStats() {
        this.successfulVisits = 0;
        this.closedEarly = 0;
        this.failedSpawns = 0;
        this.navFailures = 0;
        this.touristsHurt = 0;
        this.touristsKilled = 0;
        this.reputation = 0.0d;
        this.setChanged();
    }

    public void rateVisit(VisitResult result) {
        this.reputation = applyRating(this.reputation, result);

        // Use a Runnable to make compiler catch forgotten updates when new VisitResult enums are added.
        Runnable update = switch (result) {
            case GOOD, GREAT -> () -> this.successfulVisits++;
            case CLOSED_ON_SPAWN, CLOSED_ON_ARRIVAL -> () -> this.closedEarly++;
            case LOST -> () -> this.navFailures++;
            case HURT_EN_ROUTE, HURT_ON_PREMISES -> () -> this.touristsHurt++;
            case KILLED_EN_ROUTE, KILLED_ON_PREMISES -> () -> this.touristsKilled++;
            case FAILED_SPAWN -> () -> this.failedSpawns++;
            case ARRIVED, UNFAVORABLE -> () -> {};
        };
        update.run();

        this.setChanged();
    }

    private double applyRating(double reputation, VisitResult result) {
        double positiveNormalized = Math.max(0.0, reputation) / MAX_REPUTATION;
        double negativeNormalized = Math.max(0.0, -reputation) / MAX_REPUTATION;
        double change = switch (result) {
            case ARRIVED, GOOD, GREAT ->
                    result.baseReputationDelta() * (1.0 - positiveNormalized) * (1.0 + 0.5 * negativeNormalized);
            case UNFAVORABLE, FAILED_SPAWN, LOST, CLOSED_ON_SPAWN, CLOSED_ON_ARRIVAL, HURT_EN_ROUTE, HURT_ON_PREMISES, KILLED_EN_ROUTE, KILLED_ON_PREMISES ->
                    result.baseReputationDelta() * (0.75 + 0.5 * positiveNormalized);
        };

        return Mth.clamp(reputation + change, MIN_REPUTATION, MAX_REPUTATION);
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        // Register this block entity for tourism when block entity is attached back into a chunk/world.
        this.syncTourismRegistration();
    }

    @Override
    public void setRemoved() {
        // Unregister this block entity from tourism when block entity is being removed, replaced, or unloaded from active use.
        TourismManager.unregisterTouristBeacon(this);
        super.setRemoved();
    }

    private void syncTourismRegistration() {
        if (this.level instanceof ServerLevel) {
            if (!this.isRemoved()) {
                TourismManager.registerTouristBeacon(this);
            } else {
                TourismManager.unregisterTouristBeacon(this);
            }
        }
    }

    //region Persistence Methods
    @Override
    protected void loadAdditional(@NonNull ValueInput valueInput) {
        super.loadAdditional(valueInput);
        this.name = parseCustomNameSafe(valueInput, "CustomName");
        valueInput.read("UUID", UUIDUtil.CODEC).ifPresent(UUID -> { this.uuid = UUID; });

        TouristBeaconExperience experience = valueInput.read("BeaconExperience", TouristBeaconExperience.CODEC)
                .orElse(TouristBeaconExperience.EMPTY);
        this.setOpenForBusiness(experience.beaconOpenForBusiness());

        this.successfulVisits = valueInput.getIntOr("SuccessfulVisits", 0);
        this.closedEarly = valueInput.getIntOr("ClosedEarly", 0);
        this.failedSpawns = valueInput.getIntOr("FailedSpawns", 0);
        this.navFailures = valueInput.getIntOr("NavFailures", 0);
        this.touristsHurt = valueInput.getIntOr("TouristsHurt", 0);
        this.touristsKilled = valueInput.getIntOr("TouristsKilled", 0);
        this.reputation = valueInput.getDoubleOr("Reputation", 0d);
    }

    @Override
    protected void saveAdditional(@NonNull ValueOutput valueOutput) {
        super.saveAdditional(valueOutput);
        valueOutput.storeNullable("CustomName", ComponentSerialization.CODEC, this.name);
        valueOutput.store("UUID", UUIDUtil.CODEC, this.getUUID());
        valueOutput.store("BeaconExperience", TouristBeaconExperience.CODEC, this.getBeaconExperience());
        valueOutput.putInt("SuccessfulVisits", this.successfulVisits);
        valueOutput.putInt("ClosedEarly", this.closedEarly);
        valueOutput.putInt("FailedSpawns", this.failedSpawns);
        valueOutput.putInt("NavFailures", this.navFailures);
        valueOutput.putInt("TouristsHurt", this.touristsHurt);
        valueOutput.putInt("TouristsKilled", this.touristsKilled);
        valueOutput.putDouble("Reputation", this.reputation);
    }

    @Override
    protected void applyImplicitComponents(@NonNull DataComponentGetter dataComponentGetter) {
        super.applyImplicitComponents(dataComponentGetter);

        // Restore additional components when BlockItem is placed as a Block/Block Entity.
        this.name = dataComponentGetter.get(DataComponents.CUSTOM_NAME);

        this.uuid = dataComponentGetter.getOrDefault(
                ModComponents.TOURIST_BEACON_UUID,
                this.getUUID()
        );

        TouristBeaconExperience experience = dataComponentGetter.getOrDefault(
                ModComponents.TOURIST_BEACON_EXPERIENCE,
                TouristBeaconExperience.EMPTY
        );
        this.setOpenForBusiness(experience.beaconOpenForBusiness());

        TouristBeaconStats stats = dataComponentGetter.getOrDefault(
                ModComponents.TOURIST_BEACON_STATS,
                TouristBeaconStats.EMPTY
        );
        this.successfulVisits = stats.successfulVisits();
        this.closedEarly = stats.closedEarly();
        this.failedSpawns = stats.failedSpawns();
        this.navFailures = stats.navFailures();
        this.touristsHurt = stats.touristsHurt();
        this.touristsKilled = stats.touristsKilled();
        this.reputation = stats.reputation();

        this.syncTourismRegistration();
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.@NonNull Builder builder) {
        super.collectImplicitComponents(builder);

        // Collect additional components to save in components container in BlockItem when block breaks.
        builder.set(DataComponents.CUSTOM_NAME, this.name);
        builder.set(ModComponents.TOURIST_BEACON_UUID, this.getUUID());
        builder.set(ModComponents.TOURIST_BEACON_EXPERIENCE, this.getBeaconExperience());
        builder.set(ModComponents.TOURIST_BEACON_STATS, this.getBeaconStats());
    }

    @SuppressWarnings("deprecation")
    @Override
    public void removeComponentsFromTag(@NonNull ValueOutput valueOutput) {
        super.removeComponentsFromTag(valueOutput);

        // Remove raw tag entries for data that is carried by custom components in the block item form.
        valueOutput.discard("CustomName");
        valueOutput.discard("UUID");
        valueOutput.discard("BeaconExperience");
        valueOutput.discard("SuccessfulVisits");
        valueOutput.discard("ClosedEarly");
        valueOutput.discard("FailedSpawns");
        valueOutput.discard("NavFailures");
        valueOutput.discard("TouristsHurt");
        valueOutput.discard("TouristsKilled");
        valueOutput.discard("Reputation");
    }

    @Override
    public void preRemoveSideEffects(@NonNull BlockPos blockPos, @NonNull BlockState blockState) {
        // Overriding with an empty method prevents spilling contents on block break.
    }
    //endregion
}
