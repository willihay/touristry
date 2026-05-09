package org.bensam.touristry.tourism;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class TourismSavedData extends SavedData {
    public record PendingTouristSpawnData(int timeOfDay, BlockPos beaconPos) {
        public static final Codec<PendingTouristSpawnData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("time_of_day").forGetter(PendingTouristSpawnData::timeOfDay),
                BlockPos.CODEC.fieldOf("beacon_pos").forGetter(PendingTouristSpawnData::beaconPos)
        ).apply(instance, PendingTouristSpawnData::new));
    }

    private static final Codec<TourismSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.optionalFieldOf("prepared_day", -1L).forGetter(TourismSavedData::getPreparedDay),
            PendingTouristSpawnData.CODEC.listOf().optionalFieldOf("pending_spawns", List.of()).forGetter(TourismSavedData::getPendingSpawns)
    ).apply(instance, TourismSavedData::new));

    public static final SavedDataType<TourismSavedData> TYPE = new SavedDataType<>(
            "touristry_tourism",
            TourismSavedData::new,
            CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE
    );

    private long preparedDay;
    private final List<PendingTouristSpawnData> pendingSpawns;

    public TourismSavedData() {
        this(-1L, List.of());
    }

    private TourismSavedData(long preparedDay, List<PendingTouristSpawnData> pendingSpawns) {
        this.preparedDay = preparedDay;
        this.pendingSpawns = new ArrayList<>(pendingSpawns);
    }

    public long getPreparedDay() {
        return this.preparedDay;
    }

    public List<PendingTouristSpawnData> getPendingSpawns() {
        return List.copyOf(this.pendingSpawns);
    }

    public void setScheduleState(long preparedDay, Collection<PendingTouristSpawnData> pendingSpawns) {
        List<PendingTouristSpawnData> pendingSpawnCopy = List.copyOf(pendingSpawns);
        if (this.preparedDay == preparedDay && this.pendingSpawns.equals(pendingSpawnCopy)) {
            return;
        }

        this.preparedDay = preparedDay;
        this.pendingSpawns.clear();
        this.pendingSpawns.addAll(pendingSpawnCopy);
        this.setDirty();
    }
}
