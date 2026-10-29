package org.bensam.touristry.tourism.experience;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.UUID;

public class LodgingBedInfo {
    private ItemStack customCost; // EMPTY if using default
    private UUID reservation; // UUID of tourist entity who has reserved this bed for the coming night

    public static final Codec<LodgingBedInfo> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemStack.CODEC.optionalFieldOf("cost").forGetter(info -> info.customCost.isEmpty() ? Optional.empty() : Optional.of(info.customCost)),
            UUIDUtil.CODEC.optionalFieldOf("reservation").forGetter(info -> Optional.ofNullable(info.reservation))
    ).apply(instance, (costOpt, resOpt) -> new LodgingBedInfo(
            costOpt.orElse(ItemStack.EMPTY),
            resOpt.orElse(null)
    )));

    public static final Codec<LinkedHashMap<BlockPos, LodgingBedInfo>> BED_INFO_CODEC = Codec.pair(BlockPos.CODEC, LodgingBedInfo.CODEC).listOf().xmap(
            entries -> {
                LinkedHashMap<BlockPos, LodgingBedInfo> result = new LinkedHashMap<>();
                for (Pair<BlockPos, LodgingBedInfo> pair : entries) {
                    result.put(pair.getFirst(), pair.getSecond());
                }
                return result;
            },
            bedInfo -> bedInfo.entrySet().stream()
                    .map(entry -> Pair.of(entry.getKey(), entry.getValue()))
                    .toList()
    );

    public LodgingBedInfo() {
        this.customCost = ItemStack.EMPTY;
        this.reservation = null;
    }

    public LodgingBedInfo(ItemStack customCost, UUID reservation) {
        this.customCost = customCost.copy();
        this.reservation = reservation;
    }

    public ItemStack getCost() {
        return this.customCost.copy();
    }

    public ItemStack getCostOrDefault(ItemStack defaultCost) {
        return this.hasCustomCost() ? this.customCost.copy() : defaultCost;
    }

    public boolean hasCustomCost() {
        return !this.customCost.isEmpty();
    }

    public void setCost(ItemStack cost) {
        this.customCost = cost.copy();
    }

    public void clearReservation() {
        this.reservation = null;
    }

    public @Nullable UUID getReservation() {
        return this.reservation;
    }

    public boolean hasReservation() {
        return this.reservation != null;
    }

    public void setReservation(UUID uuid) {
        this.reservation = uuid;
    }
}
