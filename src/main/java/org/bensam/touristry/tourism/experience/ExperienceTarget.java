package org.bensam.touristry.tourism.experience;

import com.mojang.serialization.Codec;
import com.mojang.datafixers.util.Either;
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
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public record ExperienceTarget(
        BlockPos pos,
        Direction approachFrom,
        @Nullable UUID entityUUID,
        long registeredAtTicks
) {
    // TODO: Remove the legacy codec after all test environments have been updated.
    private static final Codec<ExperienceTarget> APPROACH_FROM_CODEC = createCodec("approach_from", false);
    private static final Codec<ExperienceTarget> LEGACY_PLAYER_FACING_CODEC = createCodec("player_facing", true);

    public static final Codec<ExperienceTarget> CODEC = Codec.either(APPROACH_FROM_CODEC, LEGACY_PLAYER_FACING_CODEC)
            .xmap(
                    target -> target.map(value -> value, value -> value),
                    Either::left
            );

    private static Codec<ExperienceTarget> createCodec(String approachFieldName, boolean invertDirection) {
        Codec<Direction> directionCodec = invertDirection
                ? Direction.CODEC.xmap(Direction::getOpposite, Direction::getOpposite)
                : Direction.CODEC;

        return RecordCodecBuilder.create(instance -> instance.group(
                BlockPos.CODEC.fieldOf("pos").forGetter(ExperienceTarget::pos),
                directionCodec.fieldOf(approachFieldName).forGetter(ExperienceTarget::approachFrom),
                UUIDUtil.CODEC.optionalFieldOf("entity_uuid").forGetter(target -> Optional.ofNullable(target.entityUUID())),
                Codec.LONG.fieldOf("registered_at_ticks").forGetter(ExperienceTarget::registeredAtTicks)
        ).apply(instance, (pos, approachFrom, entityUUID, time) ->
                new ExperienceTarget(pos, approachFrom, entityUUID.orElse(null), time))
        );
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, ExperienceTarget> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            ExperienceTarget::pos,
            Direction.STREAM_CODEC,
            ExperienceTarget::approachFrom,
            ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC),
            target -> Optional.ofNullable(target.entityUUID()),
            ByteBufCodecs.VAR_LONG,
            ExperienceTarget::registeredAtTicks,
            (pos, approach, entityUUID, time) ->
                    new ExperienceTarget(pos, approach, entityUUID.orElse(null), time)
    );

    public Component getDisplayName(ServerLevel serverLevel) {
        if (this.isEntity()) {
            Entity entity = serverLevel.getEntity(this.entityUUID);
            return entity != null ? entity.getDisplayName() : Component.literal("Unknown target");
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
        return this.entityUUID == null;
    }

    public boolean isEntity() {
        return this.entityUUID != null;
    }
}
