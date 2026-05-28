package org.bensam.touristry.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.bensam.touristry.ModBlockEntities;
import org.bensam.touristry.ModComponents;
import org.bensam.touristry.ModItems;
import org.bensam.touristry.Touristry;
import org.bensam.touristry.block.TouristBeaconBlock;
import org.bensam.touristry.item.BeaconKeyItem;
import org.bensam.touristry.menu.TouristBeaconMenu;
import org.bensam.touristry.tourism.TourismManager;
import org.bensam.touristry.tourism.TouristBeaconExperience;
import org.bensam.touristry.tourism.TouristBeaconStats;
import org.bensam.touristry.tourism.VisitResult;
import org.bensam.touristry.tourism.experience.SightseeingExperience;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TouristBeaconBlockEntity extends BaseContainerBlockEntity {
    public static final int TOTAL_INVENTORY_SIZE = 10;
    public static final int PAYMENT_SLOT_SIZE = 9;
    public static final int BEACON_KEY_SLOT_INDEX = PAYMENT_SLOT_SIZE;
    private static final double MIN_REPUTATION = -100.0;
    private static final double MAX_REPUTATION = 100.0;
    public static final int DATA_REPUTATION = 0;
    public static final int DATA_OPEN_FOR_BUSINESS = 1;
    public static final int DATA_COUNT = 2;

    private UUID uuid = UUID.randomUUID();

    private NonNullList<ItemStack> beaconItems = NonNullList.withSize(TOTAL_INVENTORY_SIZE, ItemStack.EMPTY);
    private List<SightseeingExperience> experiences = new ArrayList<>();
    private int experienceSlots;

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
        this.setItem(BEACON_KEY_SLOT_INDEX, this.getBeaconKey());
        this.experienceSlots = TouristBeaconExperience.BASE_EXPERIENCE_SLOTS;
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
    public void clearContent() {
        super.clearContent();
        this.setChanged();
    }

    @Override
    protected @NonNull AbstractContainerMenu createMenu(int i, @NonNull Inventory inventory) {
        return new TouristBeaconMenu(i, inventory, this, this.data, ContainerLevelAccess.create(this.level, this.getBlockPos()));
    }

    @Override
    public int getContainerSize() {
        return this.beaconItems.size();
    }

    public int getPaymentSlotSize() {
        return PAYMENT_SLOT_SIZE;
    }

    @Override
    protected @NonNull Component getDefaultName() {
        return Component.translatable("block." + Touristry.MOD_ID + ".tourist_beacon");
    }

    @Override
    protected @NonNull NonNullList<ItemStack> getItems() {
        return this.beaconItems;
    }

    @Override
    protected void setItems(@NonNull NonNullList<ItemStack> nonNullList) {
        this.beaconItems = nonNullList;
    }

    public boolean tryDepositItem(ItemStack itemStack) {
        if (!this.isOpenForBusiness()) {
            return false;
        }

        if (itemStack.isEmpty()) {
            return true;
        }

        ItemStack depositStack = itemStack.copy();

        for (int i = 0; i < PAYMENT_SLOT_SIZE; i++) {
            ItemStack slotStack = this.getItem(i);
            if (!slotStack.isEmpty() && (!ItemStack.isSameItemSameComponents(slotStack, depositStack)
                    || slotStack.getCount() >= slotStack.getMaxStackSize())) {
                continue;
            }

            if (slotStack.isEmpty()) {
                this.setItem(i, depositStack);
                return true;
            } else {
                int depositCount = depositStack.getCount();
                int slotCount = slotStack.getCount();
                int totalCount = slotCount + depositCount;
                int overMax = totalCount > slotStack.getMaxStackSize() ? totalCount - slotStack.getMaxStackSize() : 0;
                if (overMax == 0) {
                    slotStack.setCount(totalCount);
                    return true;
                }
                slotStack.setCount(getMaxStackSize());
                depositStack.setCount(overMax);
            }
        }

        return false;
    }

    public TouristBeaconExperience getBeaconExperience() {
        return new TouristBeaconExperience(this.openForBusiness, this.experienceSlots, this.experiences);
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

    public ItemStack getBeaconKey() {
        ItemStack key = new ItemStack(ModItems.BEACON_KEY.get());
        if (key.getItem() instanceof BeaconKeyItem beaconKeyItem) {
            beaconKeyItem.setBeaconUUID(key, this.getUUID());
        }
        return key;
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
            case ARRIVED, GOOD, GREAT -> () -> this.successfulVisits++;
            case CLOSED_ON_SPAWN, CLOSED_ON_ARRIVAL -> () -> this.closedEarly++;
            case LOST -> () -> this.navFailures++;
            case HURT_EN_ROUTE, HURT_ON_PREMISES -> () -> this.touristsHurt++;
            case KILLED_EN_ROUTE, KILLED_ON_PREMISES -> () -> this.touristsKilled++;
            case FAILED_SPAWN -> () -> this.failedSpawns++;
            case UNFAVORABLE -> () -> {};
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

//    @Override
//    public void setChanged() {
//        super.setChanged();
//
//        if (this.level != null) {
//            // Send update to neighbors AND clients (bitmask = 3) since this block's light level can change.
//            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
//        }
//    }

    @Override
    public void setRemoved() {
        // Unregister this block entity from tourism when block entity is being removed, replaced, or unloaded from active use.
        TourismManager.unregisterTouristBeacon(this);
        super.setRemoved();
    }

    private void syncTourismRegistration() {
        if (this.level instanceof ServerLevel && !this.isRemoved()) {
            TourismManager.registerTouristBeacon(this);
        } else {
            TourismManager.unregisterTouristBeacon(this);
        }
    }

    //region Persistence Methods
    @Override
    protected void loadAdditional(@NonNull ValueInput valueInput) {
        super.loadAdditional(valueInput);
        valueInput.read("UUID", UUIDUtil.CODEC).ifPresent(UUID -> { this.uuid = UUID; });
        this.beaconItems = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(valueInput, this.beaconItems);
        this.setItem(BEACON_KEY_SLOT_INDEX, this.getBeaconKey());

        TouristBeaconExperience experience = valueInput.read("BeaconExperience", TouristBeaconExperience.CODEC)
                .orElse(TouristBeaconExperience.EMPTY);
        this.experiences = new ArrayList<>(experience.experiences());
        this.experienceSlots = experience.experienceSlots();
        this.openForBusiness = experience.beaconOpenForBusiness();

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
        valueOutput.store("UUID", UUIDUtil.CODEC, this.getUUID());
        ContainerHelper.saveAllItems(valueOutput, this.beaconItems);
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
        this.setItem(BEACON_KEY_SLOT_INDEX, this.getBeaconKey());

        // Restore additional components when BlockItem is placed as a Block/Block Entity.
        this.uuid = dataComponentGetter.getOrDefault(
                ModComponents.TOURIST_BEACON_UUID,
                this.getUUID()
        );

        TouristBeaconExperience experience = dataComponentGetter.getOrDefault(
                ModComponents.TOURIST_BEACON_EXPERIENCE,
                TouristBeaconExperience.EMPTY
        );
        this.experiences = new ArrayList<>(experience.experiences());
        this.experienceSlots = experience.experienceSlots();
        this.openForBusiness = experience.beaconOpenForBusiness();

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
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.@NonNull Builder builder) {
        super.collectImplicitComponents(builder);

        // Collect additional components to save in components container in BlockItem when block breaks.
        builder.set(ModComponents.TOURIST_BEACON_UUID, this.getUUID());
        builder.set(ModComponents.TOURIST_BEACON_EXPERIENCE, this.getBeaconExperience());
        builder.set(ModComponents.TOURIST_BEACON_STATS, this.getBeaconStats());
    }

    @Override
    public void removeComponentsFromTag(@NonNull ValueOutput valueOutput) {
        super.removeComponentsFromTag(valueOutput);

        // Remove raw tag entries for data that is carried by custom components in the block item form.
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
