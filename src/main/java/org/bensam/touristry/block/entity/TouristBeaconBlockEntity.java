package org.bensam.touristry.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.level.ServerLevel;
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
import org.bensam.touristry.tourism.TouristReview;
import org.bensam.touristry.tourism.VisitResult;
import org.bensam.touristry.tourism.experience.TouristLocationStats;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

public class TouristBeaconBlockEntity extends BlockEntity implements MenuProvider, Nameable {
    public static final int DATA_REPUTATION = 0;
    public static final int DATA_OPEN_FOR_BUSINESS = 1;
    public static final int DATA_COUNT = 2;

    @Nullable private Component name;
    private UUID uuid = UUID.randomUUID();
    private boolean openForBusiness;
    private TouristLocationStats statistics;

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            return switch (i) {
                case DATA_REPUTATION -> (int) Math.round(TouristBeaconBlockEntity.this.statistics.getReputation() * 100.0);
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
        this.statistics = new TouristLocationStats();
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        // Register this block entity for tourism when block entity is attached back into a chunk/world.
        this.syncTourismRegistration();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return new TouristBeaconMenu(i, inventory, this.data, ContainerLevelAccess.create(this.level, this.getBlockPos()));
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

    public TouristLocationStats getStatistics() {
        return this.statistics;
    }

    public UUID getUUID() {
        return this.uuid;
    }

    public boolean isOpenForBusiness() {
        return this.openForBusiness;
    }

    public void rateVisit(VisitResult result) {
        this.rateVisit(result, 0);
    }

    // VisitResult::ARRIVED requires current time in ticks
    public void rateVisit(VisitResult result, long currentTimeTicks) {
        this.statistics.setReputation(TouristReview.calculateNewReputation(this.statistics.getReputation(), result));

        // Use a Runnable to make compiler catch forgotten updates when new VisitResult enums are added.
        Runnable update = switch (result) {
            case ARRIVED -> () -> this.statistics.recordVisit(currentTimeTicks);
            case GOOD, GREAT, UNFAVORABLE -> this.statistics::recordCompletedVisit;
            case FAILED_SPAWN -> this.statistics::recordFailedSpawn;
            case LOST -> this.statistics::recordNavFailure;
            case CLOSED_EARLY -> this.statistics::recordClosedEarly;
            case HURT_EN_ROUTE, HURT_ON_PREMISES -> this.statistics::recordTouristHurt;
            case KILLED_EN_ROUTE, KILLED_ON_PREMISES -> this.statistics::recordTouristKilled;
        };
        update.run();

        this.setChanged();
    }

    public void resetAllStats() {
        this.statistics.resetAll();
        this.setChanged();
    }

    public void resetReputation() {
        this.statistics.resetReputation();
        this.setChanged();
    }

    public void setOpenForBusiness(boolean openForBusiness) {
        this.openForBusiness = openForBusiness;
        BlockState blockState = this.getBlockState();
        if (blockState.hasProperty(TouristBeaconBlock.OPEN_FOR_BUSINESS) && this.level != null) {
            this.level.setBlockAndUpdate(this.getBlockPos(), blockState.setValue(TouristBeaconBlock.OPEN_FOR_BUSINESS, openForBusiness));
        }
        this.setChanged();
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
        this.setOpenForBusiness(valueInput.getBooleanOr("OpenForBusiness", false));
        valueInput.read("Statistics", TouristLocationStats.CODEC).ifPresent(statistics -> { this.statistics = statistics; });
    }

    @Override
    protected void saveAdditional(@NonNull ValueOutput valueOutput) {
        super.saveAdditional(valueOutput);
        valueOutput.storeNullable("CustomName", ComponentSerialization.CODEC, this.name);
        valueOutput.store("UUID", UUIDUtil.CODEC, this.getUUID());
        valueOutput.putBoolean("OpenForBusiness", this.openForBusiness);
        valueOutput.store("Statistics", TouristLocationStats.CODEC, this.statistics);
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

        this.setOpenForBusiness(dataComponentGetter.getOrDefault(
                ModComponents.TOURIST_BEACON_STATUS,
                false
        ));

        this.statistics = dataComponentGetter.getOrDefault(
                ModComponents.TOURIST_BEACON_STATISTICS,
                new TouristLocationStats()
        );

        this.syncTourismRegistration();
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.@NonNull Builder builder) {
        super.collectImplicitComponents(builder);

        // Collect additional components to save in components container in BlockItem when block breaks.
        builder.set(DataComponents.CUSTOM_NAME, this.name);
        builder.set(ModComponents.TOURIST_BEACON_UUID, this.uuid);
        builder.set(ModComponents.TOURIST_BEACON_STATUS, this.openForBusiness);
        builder.set(ModComponents.TOURIST_BEACON_STATISTICS, this.getStatistics());
    }

    @SuppressWarnings("deprecation")
    @Override
    public void removeComponentsFromTag(@NonNull ValueOutput valueOutput) {
        super.removeComponentsFromTag(valueOutput);

        // Remove raw tag entries for data that is carried by custom components in the block item form.
        valueOutput.discard("CustomName");
        valueOutput.discard("UUID");
        valueOutput.discard("OpenForBusiness");
        valueOutput.discard("Statistics");
    }

    @Override
    public void preRemoveSideEffects(@NonNull BlockPos blockPos, @NonNull BlockState blockState) {
        // Overriding with an empty method prevents spilling contents on block break.
    }
    //endregion
}
