package org.bensam.touristry.tourism.experience;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.bensam.touristry.tourism.TourismManager;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public record ExperienceTarget(
        BlockPos pos,
        Direction playerFacing,
        @Nullable UUID childExperienceUUID,
        @Nullable UUID entityUUID,
        long registeredAtTicks
) {
    public static final Codec<ExperienceTarget> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.fieldOf("pos").forGetter(ExperienceTarget::pos),
            Direction.CODEC.fieldOf("player_facing").forGetter(ExperienceTarget::playerFacing),
            UUIDUtil.CODEC.optionalFieldOf("child_experience_uuid").forGetter(target -> Optional.ofNullable(target.childExperienceUUID())),
            UUIDUtil.CODEC.optionalFieldOf("entity_uuid").forGetter(target -> Optional.ofNullable(target.entityUUID())),
            Codec.LONG.fieldOf("registered_at_ticks").forGetter(ExperienceTarget::registeredAtTicks)
    ).apply(instance, (pos, facing, childUUID, entityUUID, time) ->
            new ExperienceTarget(pos, facing, childUUID.orElse(null), entityUUID.orElse(null), time))
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ExperienceTarget> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            ExperienceTarget::pos,
            Direction.STREAM_CODEC,
            ExperienceTarget::playerFacing,
            ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC),
            target -> Optional.ofNullable(target.childExperienceUUID()),
            ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC),
            target -> Optional.ofNullable(target.entityUUID()),
            ByteBufCodecs.VAR_LONG,
            ExperienceTarget::registeredAtTicks,
            (pos, facing, childUUID, entityUUID, time) ->
                    new ExperienceTarget(pos, facing, childUUID.orElse(null), entityUUID.orElse(null), time)
    );

    public Component getDisplayName(ServerLevel serverLevel) {
        if (this.isEntity()) {
            Entity entity = serverLevel.getEntity(this.entityUUID);
            return entity != null ? entity.getDisplayName() : Component.literal("Unknown target");
        } else if (this.isChildExperience()) {
            TouristExperience experience = TourismManager.getTouristExperienceById(this.childExperienceUUID);
            if (experience != null) {
                return experience.getDisplayName();
            }
        }

        return serverLevel.getBlockState(this.pos).getBlock().getName();
    }

    public @NonNull ItemStack getItemStack(ServerLevel serverLevel) {
        if (this.isEntity()) {
            Entity entity = serverLevel.getEntity(this.entityUUID);
            if (entity == null) {
                return new ItemStack(Items.AIR);
            }
            ItemStack itemStack = entity.getPickResult();
            return itemStack == null || itemStack.isEmpty() ? ItemStack.EMPTY : itemStack.copy();
        }

        return new ItemStack(serverLevel.getBlockState(this.pos).getBlock().asItem());
    }

    public boolean isBlock() {
        return this.childExperienceUUID == null && this.entityUUID == null;
    }

    public boolean isChildExperience() {
        return this.childExperienceUUID != null;
    }

    public boolean isEntity() {
        return this.entityUUID != null;
    }
}
