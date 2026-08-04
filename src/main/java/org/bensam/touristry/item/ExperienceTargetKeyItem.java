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
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BedBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.bensam.touristry.ModComponents;
import org.bensam.touristry.block.entity.AbstractExperienceBlockEntity;
import org.bensam.touristry.tourism.TourismManager;
import org.bensam.touristry.tourism.experience.TouristExperience;

import java.util.UUID;

public class ExperienceTargetKeyItem extends Item {
    public ExperienceTargetKeyItem(Properties properties) {
        super(properties);
    }

    public UUID getLinkedExperienceUUID(ItemStack itemStack) {
        return itemStack.getOrDefault(ModComponents.TOURIST_EXPERIENCE_KEY_UUID, new UUID(0, 0));
    }

    public InteractionResult useOnBlock(ServerLevel serverLevel, Player player, ItemStack key, BlockHitResult hitResult) {
        TouristExperience experience = this.validateKeyAndGetExperience(player, key);
        if (experience == null)
        {
            return InteractionResult.FAIL;
        }

        BlockPos blockPos = hitResult.getBlockPos();
        BlockEntity blockEntity = serverLevel.getBlockEntity(blockPos);
        BlockState blockState = serverLevel.getBlockState(blockPos);

        // If the block entity is part of a double-wide connection (e.g. chest, bed), always store the BlockPos of the LEFT/HEAD half.
        // This way, players can click on either half and the object will be stored as a single target using a consistent BlockPos.
        // Note that the LEFT half of a chest is the opposite of what you'd think as a player looking at the front of the chest...
        if (blockEntity instanceof ChestBlockEntity) {
            ChestType chestType = blockState.getValue(ChestBlock.TYPE);
            if (chestType == ChestType.RIGHT) {
                blockPos = ChestBlock.getConnectedBlockPos(blockPos, blockState);
            }
        } else if (blockEntity instanceof BedBlockEntity) {
            BedPart bedPart = blockState.getValue(BedBlock.PART);
            if (bedPart == BedPart.FOOT) {
                blockPos = blockPos.relative(BedBlock.getConnectedDirection(blockState));
            }
        }

        if (player.isShiftKeyDown()) {
            // Remove target from experience.
            boolean removed = experience.removeTarget(serverLevel, blockPos);
            this.displayLinkRemovalMessage(player, experience, removed);
            return InteractionResult.SUCCESS;
        } else {
            // Add target to experience.
            // Check if target is already linked to the experience.
            boolean alreadyLinked = experience.hasTarget(blockPos);

            if (!alreadyLinked) {
                // Check if target is already linked to a different experience.
                TouristExperience owner = TourismManager.findOwnerOfExperienceTarget(blockPos);
                if (owner != null) {
                    this.displayLinkedToDifferentExperienceMessage(player, owner);
                    return InteractionResult.FAIL;
                }

                // Check if target is too far away from experience to establish a link.
                if (!this.validateTargetDistance(player, experience, blockPos)) {
                    return InteractionResult.FAIL;
                }

                // Check if target is also an experience block, in which case it will become a child experience.
                UUID childUUID = blockEntity instanceof AbstractExperienceBlockEntity childExperience ? childExperience.getUUID() : null;

                // Get the player's facing direction so that it can be stored in the target, so that pathfinding goals can
                // lead a tourist to approach from the same direction, if desired.
                Direction playerFacing = player.getDirection();

                // Try to add target to experience.
                boolean success = false;
                if (childUUID == null) {
                    success = experience.addBlockTarget(serverLevel, blockPos, playerFacing);
                } else {
                    success = experience.addChildExperienceTarget(serverLevel, blockPos, playerFacing, childUUID);
                }

                this.displayLinkAdditionMessage(player, experience, success);
                return success ? InteractionResult.SUCCESS : InteractionResult.FAIL;
            }

            player.displayClientMessage(
                    Component.literal("Already linked to ")
                            .append(experience.getDisplayName()),
                    true
            );
            return InteractionResult.SUCCESS;
        }
    }

    public InteractionResult useOnEntity(ServerLevel serverLevel, Player player, ItemStack key, Entity entity, EntityHitResult hitResult) {
        TouristExperience experience = this.validateKeyAndGetExperience(player, key);
        if (experience == null)
        {
            return InteractionResult.FAIL;
        }

        BlockPos entityPos = entity.blockPosition();

        if (player.isShiftKeyDown()) {
            // Remove target from experience.
            boolean removed = experience.removeEntityTargetById(serverLevel, entity.getUUID());
            this.displayLinkRemovalMessage(player, experience, removed);
            return InteractionResult.SUCCESS;
        } else {
            // Add target to experience.
            // Check if target is already linked to the experience.
            boolean alreadyLinked = experience.hasTarget(entityPos);

            if (!alreadyLinked) {
                // Check if target is already linked to a different experience.
                TouristExperience owner = TourismManager.findOwnerOfExperienceTarget(entityPos);
                if (owner != null) {
                    this.displayLinkedToDifferentExperienceMessage(player, owner);
                    return InteractionResult.FAIL;
                }

                // Check if target is too far away from experience to establish a link.
                if (!this.validateTargetDistance(player, experience, entityPos)) {
                    return InteractionResult.FAIL;
                }

                // Get the player's facing direction so that it can be stored in the target, so that pathfinding goals can
                // lead a tourist to approach from the same direction, if desired.
                Direction playerFacing = player.getDirection();

                // Try to add target to experience.
                boolean success = experience.addEntityTarget(serverLevel, entityPos, playerFacing, entity.getUUID());
                this.displayLinkAdditionMessage(player, experience, success);
                return success ? InteractionResult.SUCCESS : InteractionResult.FAIL;
            }

            player.displayClientMessage(
                    Component.literal("Already linked to ")
                            .append(experience.getDisplayName()),
                    true
            );
            return InteractionResult.SUCCESS;
        }
    }

    private void displayLinkedToDifferentExperienceMessage(Player player, TouristExperience otherExperience) {
        player.displayClientMessage(
                Component.literal("Unable to link - already linked to ")
                        .append(otherExperience.getDisplayName()),
                true
        );
    }

    private void displayLinkAdditionMessage(Player player, TouristExperience experience, boolean added) {
        if (added) {
            player.displayClientMessage(
                    Component.literal("Linked to ")
                            .append(experience.getDisplayName()),
                    true
            );
        } else {
            player.displayClientMessage(
                    Component.literal("Not a valid target for ")
                            .append(experience.getDisplayName()),
                    true
            );
        }
    }

    private void displayLinkRemovalMessage(Player player, TouristExperience experience, boolean removed) {
        if (removed) {
            player.displayClientMessage(
                    Component.literal("Unlinked from ")
                            .append(experience.getDisplayName()),
                    true
            );
        } else {
            player.displayClientMessage(
                    Component.literal("Not linked to ")
                            .append(experience.getDisplayName()),
                    true
            );
        }
    }

    private TouristExperience validateKeyAndGetExperience(Player player, ItemStack key) {
        UUID linkedExperienceUUID = this.getLinkedExperienceUUID(key);
        if (linkedExperienceUUID.equals(new UUID(0, 0))) {
            player.displayClientMessage(
                    Component.literal("Key not linked to any experience block - get a new key from an experience block"),
                    true
            );
            return null;
        }

        TouristExperience experience = TourismManager.getTouristExperienceById(linkedExperienceUUID);
        if (experience == null)
        {
            player.displayClientMessage(
                    Component.literal("Linked experience block not found - verify it still exists, try using key while closer to the block, or get a new key from the block"),
                    true
            );
            return null;
        }

        return experience;
    }

    private boolean validateTargetDistance(Player player, TouristExperience experience, BlockPos targetPos) {
        double maxDistance = experience.getMaxRangeToTarget();
        double maxDistanceSq = maxDistance * maxDistance;
        if (targetPos.distSqr(experience.getBlockPos()) > maxDistanceSq) {
            player.displayClientMessage(
                    Component.literal("Too far away from linked experience block"),
                    true
            );
            return false;
        }
        return true;
    }
}
