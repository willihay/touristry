package org.bensam.touristry.tourism;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.bensam.touristry.tourism.experience.ItemPrice;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public final class TourismSavedData extends SavedData {
    public record PendingTouristSpawnData(int timeOfDay, UUID beaconUUID) {
        public static final Codec<PendingTouristSpawnData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("time_of_day").forGetter(PendingTouristSpawnData::timeOfDay),
                UUIDUtil.CODEC.fieldOf("beacon_uuid").forGetter(PendingTouristSpawnData::beaconUUID)
        ).apply(instance, PendingTouristSpawnData::new));
    }

    private static final Codec<TourismSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.optionalFieldOf("highest_budget", 0.0F).forGetter(TourismSavedData::getHighestTouristBudget),
            ItemPrice.CODEC.optionalFieldOf("most_valuable_purchase", new ItemPrice(ItemStack.EMPTY, ItemStack.EMPTY)).forGetter(TourismSavedData::getMostValuablePurchase),
            Codec.LONG.optionalFieldOf("prepared_day", -1L).forGetter(TourismSavedData::getPreparedDay),
            PendingTouristSpawnData.CODEC.listOf().optionalFieldOf("pending_spawns", List.of()).forGetter(TourismSavedData::getPendingSpawns)
    ).apply(instance, TourismSavedData::new));

    public static final SavedDataType<TourismSavedData> TYPE = new SavedDataType<>(
            "touristry_tourism",
            TourismSavedData::new,
            CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE
    );

    private float highestTouristBudget;
    private ItemPrice mostValuablePurchase;
    private long preparedDay;
    private final List<PendingTouristSpawnData> pendingSpawns;

    public TourismSavedData() {
        this(0.0F, new ItemPrice(ItemStack.EMPTY, ItemStack.EMPTY), -1L, List.of());
    }

    private TourismSavedData(float highestTouristBudget, ItemPrice mostValuablePurchase, long preparedDay, List<PendingTouristSpawnData> pendingSpawns) {
        this.highestTouristBudget = highestTouristBudget;
        this.mostValuablePurchase = mostValuablePurchase.cost() == null ? new ItemPrice(mostValuablePurchase.itemForSale(), ItemStack.EMPTY) : mostValuablePurchase;
        this.preparedDay = preparedDay;
        this.pendingSpawns = new ArrayList<>(pendingSpawns);
    }

    public float getHighestTouristBudget() {
        return this.highestTouristBudget;
    }

    public ItemPrice getMostValuablePurchase() {
        return this.mostValuablePurchase;
    }

    public long getPreparedDay() {
        return this.preparedDay;
    }

    public List<PendingTouristSpawnData> getPendingSpawns() {
        return List.copyOf(this.pendingSpawns);
    }

    public void setSavedState(float highestTouristBudget, ItemPrice mostValuablePurchase, long preparedDay, Collection<PendingTouristSpawnData> pendingSpawns) {
        List<PendingTouristSpawnData> pendingSpawnCopy = List.copyOf(pendingSpawns);
        if (this.highestTouristBudget == highestTouristBudget &&
                (mostValuablePurchase.cost() == null || ItemStack.matches(this.mostValuablePurchase.cost(), mostValuablePurchase.cost())) &&
                this.preparedDay == preparedDay &&
                this.pendingSpawns.equals(pendingSpawnCopy)) {
            return;
        }

        this.highestTouristBudget = highestTouristBudget;
        this.mostValuablePurchase = mostValuablePurchase;
        this.preparedDay = preparedDay;
        this.pendingSpawns.clear();
        this.pendingSpawns.addAll(pendingSpawnCopy);
        this.setDirty();
    }
}
