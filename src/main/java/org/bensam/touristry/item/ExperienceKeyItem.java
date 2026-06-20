package org.bensam.touristry.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.bensam.touristry.ModComponents;
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

    public InteractionResult useOnBlock(ServerLevel serverLevel, Player player, ItemStack key, BlockPos blockPos, Direction playerFacing) {
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

        double maxDistance = experience.getMaxDistanceToTarget();
        double maxDistanceSq = maxDistance * maxDistance;
        if (blockPos.distSqr(experience.getBlockPos()) > maxDistanceSq) {
            player.displayClientMessage(
                    Component.literal("Too far away from linked experience block"),
                    true
            );
            return InteractionResult.FAIL;
        }

        BlockEntity blockEntity = serverLevel.getBlockEntity(blockPos);
        UUID childUUID = blockEntity instanceof TouristExperience childExperience ? childExperience.getUUID() : null;

        if (player.isShiftKeyDown()) {
            // Remove target from experience.
            experience.removeTarget(serverLevel, blockPos);
            player.displayClientMessage(
                    Component.literal("Unlinked from ")
                            .append(experience.getDisplayName()),
                    true
            );
        } else {
            // Add target to experience.
            // TODO: Check if target is already linked to a different experience and require unlinking first.
            if (experience.addTarget(serverLevel, blockPos, playerFacing, childUUID)) {
                player.displayClientMessage(
                        Component.literal("Linked to ")
                                .append(experience.getDisplayName()),
                        true
                );
            } else {
                player.displayClientMessage(
                        Component.literal("Not a valid target"),
                        true
                );
            }
        }

        return InteractionResult.SUCCESS;
    }
}
