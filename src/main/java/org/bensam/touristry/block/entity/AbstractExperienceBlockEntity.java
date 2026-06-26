package org.bensam.touristry.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.bensam.touristry.ModComponents;
import org.bensam.touristry.ModItems;
import org.bensam.touristry.block.TouristExperienceBlock;
import org.bensam.touristry.tourism.TourismManager;
import org.bensam.touristry.tourism.experience.ExperienceStatistics;
import org.bensam.touristry.tourism.experience.ExperienceTarget;
import org.bensam.touristry.tourism.experience.TouristExperience;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.*;

public abstract class AbstractExperienceBlockEntity extends BaseContainerBlockEntity implements TouristExperience {
    private static final double MIN_REPUTATION = -100.0;
    private static final double MAX_REPUTATION = 100.0;
    public static final int DATA_REPUTATION = 0;
    public static final int DATA_OPEN_FOR_BUSINESS = 1;
    public static final int DATA_COUNT = 2;

    protected UUID uuid;
    protected @Nullable UUID parentExperienceUUID;
    private boolean openForBusiness;
    protected List<ExperienceTarget> targets;
    protected ExperienceStatistics statistics;
    protected NonNullList<ItemStack> inventory;

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            return switch (i) {
                case DATA_REPUTATION -> (int) Math.round(AbstractExperienceBlockEntity.this.statistics.getReputationScore() * 100.0);
                case DATA_OPEN_FOR_BUSINESS -> AbstractExperienceBlockEntity.this.openForBusiness ? 1 : 0;
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

    protected AbstractExperienceBlockEntity(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState, int inventorySize) {
        super(blockEntityType, blockPos, blockState);

        this.uuid = UUID.randomUUID();
        this.openForBusiness = false;
        this.targets = new ArrayList<>();
        this.statistics = new ExperienceStatistics();
        this.inventory = NonNullList.withSize(inventorySize, ItemStack.EMPTY);
        this.setItem(this.getExperienceKeySlotIndex(), this.createExperienceKey());
    }

    public boolean addTarget(ServerLevel serverLevel, ExperienceTarget target) {
        if (this.isTargetValid(serverLevel, target)) {
            this.targets.add(target);
            this.setChanged();
            return true;
        }
        return false;
    }

    @Override
    public boolean addBlockTarget(ServerLevel serverLevel, BlockPos blockPos, Direction playerFacing) {
        long timeAdded = serverLevel.getDayTime();
        ExperienceTarget target = new ExperienceTarget(blockPos, playerFacing, null, null, timeAdded);
        return this.addTarget(serverLevel, target);
    }

    @Override
    public boolean addChildExperienceTarget(ServerLevel serverLevel, BlockPos blockPos, Direction playerFacing, UUID childUUID) {
        // Check if child experience already has a different parent.
        TouristExperience childExperience = TourismManager.getTouristExperienceById(childUUID);
        if (childExperience != null) {
            UUID existingParent = childExperience.getParentExperienceUUID();
            if (existingParent != null && !existingParent.equals(this.uuid)) {
                return false; // already has a different parent
            }
        }

        long timeAdded = serverLevel.getDayTime();
        ExperienceTarget target = new ExperienceTarget(blockPos, playerFacing, childUUID, null, timeAdded);

        if (this.addTarget(serverLevel, target)) {
            // Set this experience as child's parent.
            if (childExperience instanceof AbstractExperienceBlockEntity childExperienceBlockEntity) {
                childExperienceBlockEntity.setParent(this.uuid);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean addEntityTarget(ServerLevel serverLevel, BlockPos entityPos, Direction playerFacing, UUID entityUUID) {
        long timeAdded = serverLevel.getDayTime();
        ExperienceTarget target = new ExperienceTarget(entityPos, playerFacing, null, entityUUID, timeAdded);
        return this.addTarget(serverLevel, target);
    }

    @Override
    public void clearContent() {
        super.clearContent();
        this.setChanged();
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        // Register this block entity for tourism when block entity is attached back into a chunk/world.
        this.syncTourismRegistration();
    }

    public ItemStack createExperienceKey() {
        ItemStack key = new ItemStack(ModItems.EXPERIENCE_KEY.get());

        key.set(ModComponents.TOURIST_EXPERIENCE_KEY_UUID, this.uuid);

        MutableComponent keyName = Component.literal("Key for ").append(this.getName().copy());
        if (!this.hasCustomName()) {
            keyName.append(" " + this.uuid.toString().substring(0, 8));
        }
        key.set(DataComponents.ITEM_NAME, keyName);

        return key;
    }

    @Override
    public List<UUID> getChildExperienceUUIDs() {
        return this.targets.stream()
                .filter(ExperienceTarget::isChildExperience)
                .map(ExperienceTarget::childExperienceUUID)
                .toList();
    }

    @Override
    public int getContainerSize() {
        return this.inventory.size();
    }

    protected abstract int getExperienceKeySlotIndex();

    @Override
    protected @NonNull NonNullList<ItemStack> getItems() {
        return this.inventory;
    }

    @Override
    public @Nullable UUID getParentExperienceUUID() {
        return this.parentExperienceUUID;
    }

    public abstract int getPaymentSlotSize();

    @Override
    public ExperienceStatistics getStatistics() {
        return this.statistics;
    }

    @Override
    public List<ExperienceTarget> getTargets(ServerLevel serverLevel) {
        this.pruneInvalidTargets(serverLevel);
        return Collections.unmodifiableList(this.targets);
    }

    @Override
    public UUID getUUID() {
        return this.uuid;
    }

    public boolean isOpenForBusiness() {
        return this.openForBusiness;
    }

    public boolean isTargetChildExperienceValid(UUID targetUUID) {
        if (this.uuid.equals(targetUUID)) {
            return false; // can't link experience block to itself
        }

        // Check if adding target as child would create circular dependency.
        Set<UUID> visited = new HashSet<>();
        UUID current = targetUUID;
        UUID parent = this.uuid;

        while (current != null) {
            if (visited.contains(current)) {
                return false; // found a cycle
            }

            if (current.equals(parent)) {
                return false; // would create a direct cycle
            }

            visited.add(current);

            TouristExperience experience = TourismManager.getTouristExperienceById(current);
            current = experience != null ? experience.getParentExperienceUUID() : null;
        }

        return true;
    }

    protected void pruneInvalidTargets(ServerLevel serverLevel) {
        boolean changed = this.targets.removeIf(target -> !isTargetValid(serverLevel, target));
        if (changed) {
            this.setChanged();
        }
    }

    @Override
    public void removeTarget(ServerLevel serverLevel, BlockPos pos) {
        // Clear parent relationship if removing a child experience.
        for (ExperienceTarget target : this.targets) {
            if (target.pos().equals(pos) && target.isChildExperience()) {
                TouristExperience childExperience = TourismManager.getTouristExperienceById(target.childExperienceUUID());
                if (childExperience instanceof AbstractExperienceBlockEntity childBE && 
                    this.uuid.equals(childBE.getParentExperienceUUID())) {
                    childBE.setParent(null);
                }
                break;
            }
        }
        
        this.targets.removeIf(target -> target.pos().equals(pos));
        this.setChanged();
    }

    @Override
    public void removeEntityTargetById(ServerLevel serverLevel, UUID entityUUID) {
        this.targets.removeIf(target -> entityUUID.equals(target.entityUUID()));
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

    @Override
    protected void setItems(NonNullList<ItemStack> nonNullList) {
        this.inventory = nonNullList;
    }

    public void setOpenForBusiness(boolean openForBusiness) {
        this.openForBusiness = openForBusiness;
        BlockState blockState = this.getBlockState();
        if (blockState.hasProperty(TouristExperienceBlock.OPEN_FOR_BUSINESS) && this.level != null) {
            this.level.setBlockAndUpdate(this.getBlockPos(), blockState.setValue(TouristExperienceBlock.OPEN_FOR_BUSINESS, openForBusiness));
        }
        this.setChanged();
    }

    public void setParent(@Nullable UUID parentUUID) {
        this.parentExperienceUUID = parentUUID;
        this.setChanged();
    }

    @Override
    public void setRemoved() {
        TourismManager.unregisterTouristExperience(this);
        super.setRemoved();
    }

    private void syncTourismRegistration() {
        if (this.level instanceof ServerLevel) {
            if (!this.isRemoved()) {
                TourismManager.registerTouristExperience(this);
            } else {
                TourismManager.unregisterTouristExperience(this);
            }
        }
    }

    public boolean tryDepositItem(ItemStack itemStack) {
        if (!this.isOpenForBusiness()) {
            return false;
        }

        if (itemStack.isEmpty()) {
            return true;
        }

        ItemStack depositStack = itemStack.copy();

        for (int i = 0; i < this.getPaymentSlotSize(); i++) {
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

    //region Persistence Methods
    @Override
    protected void loadAdditional(ValueInput valueInput) {
        super.loadAdditional(valueInput);
        valueInput.read("UUID", UUIDUtil.CODEC).ifPresent(UUID -> { this.uuid = UUID; });
        valueInput.read("ParentExperienceUUID", UUIDUtil.CODEC).ifPresent(UUID -> { this.parentExperienceUUID = UUID; });
        this.setOpenForBusiness(valueInput.getBooleanOr("OpenForBusiness", false));
        this.targets = new ArrayList<>(valueInput.read("Targets", ExperienceTarget.CODEC.listOf()).orElse(List.of()));
        valueInput.read("Statistics", ExperienceStatistics.CODEC).ifPresent(statistics -> { this.statistics = statistics; });
        this.inventory = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(valueInput, this.inventory);
        this.setItem(this.getExperienceKeySlotIndex(), this.createExperienceKey());
    }

    @Override
    protected void saveAdditional(ValueOutput valueOutput) {
        super.saveAdditional(valueOutput);
        valueOutput.store("UUID", UUIDUtil.CODEC, this.getUUID());
        if (this.parentExperienceUUID != null) {
            valueOutput.store("ParentExperienceUUID", UUIDUtil.CODEC, this.parentExperienceUUID);
        }
        valueOutput.putBoolean("OpenForBusiness", this.openForBusiness);
        valueOutput.store("Targets", ExperienceTarget.CODEC.listOf(), this.targets);
        valueOutput.store("Statistics", ExperienceStatistics.CODEC, this.statistics);
        ContainerHelper.saveAllItems(valueOutput, this.inventory);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter dataComponentGetter) {
        super.applyImplicitComponents(dataComponentGetter);

        // Restore additional components when BlockItem is placed as a Block/Block Entity.
        this.uuid = dataComponentGetter.getOrDefault(
                ModComponents.TOURIST_EXPERIENCE_UUID,
                this.getUUID()
        );
        this.parentExperienceUUID = dataComponentGetter.get(ModComponents.TOURIST_EXPERIENCE_PARENT_UUID);
        this.setOpenForBusiness(dataComponentGetter.getOrDefault(
                ModComponents.TOURIST_EXPERIENCE_STATUS,
                false
        ));
        this.targets = new ArrayList<>(dataComponentGetter.getOrDefault(
                ModComponents.TOURIST_EXPERIENCE_TARGETS,
                List.of()
        ));
        this.statistics = dataComponentGetter.getOrDefault(
                ModComponents.TOURIST_EXPERIENCE_STATISTICS,
                new ExperienceStatistics()
        );

        this.setItem(this.getExperienceKeySlotIndex(), this.createExperienceKey());
        this.syncTourismRegistration();
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);

        // Collect additional components to save in components container in BlockItem when block breaks.
        builder.set(ModComponents.TOURIST_EXPERIENCE_UUID, this.uuid);
        if (this.parentExperienceUUID != null) {
            builder.set(ModComponents.TOURIST_EXPERIENCE_PARENT_UUID, this.parentExperienceUUID);
        }
        builder.set(ModComponents.TOURIST_EXPERIENCE_STATUS, this.openForBusiness);
        if (!this.targets.isEmpty()) {
            builder.set(ModComponents.TOURIST_EXPERIENCE_TARGETS, List.copyOf(this.targets));
        }
        builder.set(ModComponents.TOURIST_EXPERIENCE_STATISTICS, this.statistics);
    }

    @Override
    public void removeComponentsFromTag(ValueOutput valueOutput) {
        super.removeComponentsFromTag(valueOutput);

        // Remove raw tag entries for data that is carried by custom components in the block item form.
        valueOutput.discard("UUID");
        valueOutput.discard("ParentExperienceUUID");
        valueOutput.discard("OpenForBusiness");
        valueOutput.discard("Targets");
        valueOutput.discard("Statistics");
    }

    @Override
    public void preRemoveSideEffects(@NonNull BlockPos blockPos, @NonNull BlockState blockState) {
        // Overriding with an empty method prevents spilling contents on block break.
    }
    //endregion
}
