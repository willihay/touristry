package org.bensam.touristry.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.bensam.touristry.ModComponents;
import org.bensam.touristry.block.entity.AbstractExperienceBlockEntity;
import org.bensam.touristry.tourism.TourismManager;
import org.bensam.touristry.tourism.experience.TouristExperience;

import java.util.UUID;

public class ExperienceKeyItem extends Item {
    public ExperienceKeyItem(Properties properties) {
        super(properties);
    }

    public UUID getLinkedExperienceUUID(ItemStack itemStack) {
        return itemStack.getOrDefault(ModComponents.TOURIST_EXPERIENCE_KEY_UUID, new UUID(0, 0));
    }

    public InteractionResult useOnBlock(ServerLevel serverLevel, Player player, ItemStack key, BlockHitResult hitResult) {
        UUID linkedExperienceUUID = this.getLinkedExperienceUUID(key);
        if (linkedExperienceUUID.equals(new UUID(0, 0))) {
            player.displayClientMessage(
                    Component.literal("Key not linked to any experience block - get a new key from an experience block"),
                    true
            );
            return InteractionResult.FAIL;
        }

        TouristExperience experience = TourismManager.getTouristExperienceById(linkedExperienceUUID);
        if (experience == null)
        {
            player.displayClientMessage(
                    Component.literal("Linked experience block not found - verify it still exists, try using key while closer to the block, or get a new key from the block"),
                    true
            );
            return InteractionResult.FAIL;
        }

        BlockPos blockPos = hitResult.getBlockPos();
        double maxDistance = experience.getMaxRangeToTarget();
        double maxDistanceSq = maxDistance * maxDistance;
        if (blockPos.distSqr(experience.getBlockPos()) > maxDistanceSq) {
            player.displayClientMessage(
                    Component.literal("Too far away from linked experience block"),
                    true
            );
            return InteractionResult.FAIL;
        }

        BlockEntity blockEntity = serverLevel.getBlockEntity(blockPos);
        UUID childUUID = blockEntity instanceof AbstractExperienceBlockEntity childExperience ? childExperience.getUUID() : null;

        if (player.isShiftKeyDown()) {
            // Remove target from experience.
            experience.removeTarget(serverLevel, blockPos);
            player.displayClientMessage(
                    Component.literal("Unlinked from ")
                            .append(experience.getDisplayName()),
                    true
            );
            return InteractionResult.SUCCESS;
        } else {
            // Add target to experience.
            // TODO: Check if target is already linked to a different experience and require unlinking first.

            Direction playerFacing = player.getDirection();
            boolean success = false;
            if (childUUID == null) {
                success = experience.addBlockTarget(serverLevel, blockPos, playerFacing);
            } else {
                success = experience.addChildExperienceTarget(serverLevel, blockPos, playerFacing, childUUID);
            }

            if (success) {
                player.displayClientMessage(
                        Component.literal("Linked to ")
                                .append(experience.getDisplayName()),
                        true
                );
                return InteractionResult.SUCCESS;
            } else {
                player.displayClientMessage(
                        Component.literal("Not a valid target"),
                        true
                );
            }
        }

        return InteractionResult.PASS;
    }

    public InteractionResult useOnEntity(ServerLevel serverLevel, Player player, ItemStack key, Entity entity, EntityHitResult hitResult) {
        UUID linkedExperienceUUID = this.getLinkedExperienceUUID(key);
        if (linkedExperienceUUID.equals(new UUID(0, 0))) {
            player.displayClientMessage(
                    Component.literal("Key not linked to any experience block - get a new key from an experience block"),
                    true
            );
            return InteractionResult.FAIL;
        }

        TouristExperience experience = TourismManager.getTouristExperienceById(linkedExperienceUUID);
        if (experience == null)
        {
            player.displayClientMessage(
                    Component.literal("Linked experience block not found - verify it still exists, try using key while closer to the entity, or get a new key from the block"),
                    true
            );
            return InteractionResult.FAIL;
        }

        BlockPos entityPos = entity.blockPosition();
        double maxDistance = experience.getMaxRangeToTarget();
        double maxDistanceSq = maxDistance * maxDistance;
        if (entityPos.distSqr(experience.getBlockPos()) > maxDistanceSq) {
            player.displayClientMessage(
                    Component.literal("Too far away from linked experience block"),
                    true
            );
            return InteractionResult.FAIL;
        }

        if (player.isShiftKeyDown()) {
            // Remove target from experience.
            experience.removeEntityTargetById(serverLevel, entity.getUUID());
            player.displayClientMessage(
                    Component.literal("Unlinked from ")
                            .append(experience.getDisplayName()),
                    true
            );
            return InteractionResult.SUCCESS;
        } else {
            // Add target to experience.
            // TODO: Check if target is already linked to a different experience and require unlinking first.

            Direction playerFacing = player.getDirection();
            if (experience.addEntityTarget(serverLevel, entityPos, playerFacing, entity.getUUID())) {
                player.displayClientMessage(
                        Component.literal("Linked to ")
                                .append(experience.getDisplayName()),
                        true
                );
                return InteractionResult.SUCCESS;
            } else {
                player.displayClientMessage(
                        Component.literal("Not a valid target"),
                        true
                );
            }
        }

        return InteractionResult.PASS;
    }
}
