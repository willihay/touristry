package org.bensam.touristry.item;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import org.bensam.touristry.ModAttachments;
import org.bensam.touristry.ModComponents;
import org.bensam.touristry.block.entity.TouristBeaconBlockEntity;
import org.bensam.touristry.tourism.TourismManager;
import org.bensam.touristry.tourism.TouristExperience;

import java.util.UUID;

public class BeaconKeyItem extends Item {
    public BeaconKeyItem(Properties properties) {
        super(properties);
    }

    public final UUID getBeaconUUID(ItemStack itemStack) {
        return itemStack.getOrDefault(ModComponents.TOURIST_BEACON_UUID, new UUID(0, 0));
    }

    public void setBeaconUUID(ItemStack itemStack, UUID beaconUUID) {
        itemStack.set(ModComponents.TOURIST_BEACON_UUID, beaconUUID);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand interactionHand) {
        if (!level.isClientSide()) {
            ItemStack itemStack = player.getItemInHand(interactionHand);
            player.displayClientMessage(
                    Component.literal("Associated beacon UUID: " + this.getBeaconUUID(itemStack)),
                    true
            );
        }
        return super.use(level, player, interactionHand);
    }

    @Override
    public InteractionResult useOn(UseOnContext useOnContext) {
        Level level = useOnContext.getLevel();
        ItemStack stack = useOnContext.getItemInHand();
        UUID beaconUUID = this.getBeaconUUID(stack);
        BlockPos blockPos = useOnContext.getClickedPos();
        BlockEntity blockEntity = level.getBlockEntity(blockPos);

//        if (blockEntity instanceof LecternBlockEntity lectern) {
//            if (level instanceof ServerLevel) {
//                lectern.setAttached(ModAttachments.LECTERN_TOURIST_BEACON_UUID, beaconUUID);
//                TourismManager.registerExperience(beaconUUID, new SightseeingExperience(beaconUUID, blockPos.immutable(), true));
//            }
//            return InteractionResult.SUCCESS;
//        }

        return super.useOn(useOnContext);
    }

    public InteractionResult useOnLectern(ServerLevel serverLevel, Player player, ItemStack key, LecternBlockEntity lectern) {
        UUID keyBeaconUUID = this.getBeaconUUID(key);

        if (keyBeaconUUID.equals(new UUID(0, 0))) {
            return InteractionResult.FAIL;
        }

        TouristBeaconBlockEntity keyBeacon = TourismManager.getBeaconBlockEntityByUUID(keyBeaconUUID);
        MutableComponent keyBeaconNameComponent = keyBeacon == null
                ? Component.literal(keyBeaconUUID.toString().substring(0, 8))
                : keyBeacon.getName().copy();

        if (player.isShiftKeyDown()) {
            if (lectern.hasAttached(ModAttachments.LECTERN_TOURIST_BEACON_UUID)) {
                UUID attachedUUID = lectern.getAttached(ModAttachments.LECTERN_TOURIST_BEACON_UUID);
                if (keyBeaconUUID.equals(attachedUUID)) {
                    lectern.removeAttached(ModAttachments.LECTERN_TOURIST_BEACON_UUID);
                    TouristExperience.unregisterLectern(lectern);
                    player.displayClientMessage(
                            Component.literal("Unlinked Lectern from beacon ")
                                    .append(keyBeaconNameComponent),
                            true
                    );
                } else {
                    TouristBeaconBlockEntity linkedBeacon = TourismManager.getBeaconBlockEntityByUUID(attachedUUID);
                    MutableComponent linkedBeaconNameComponent = linkedBeacon == null
                            ? Component.literal(attachedUUID.toString().substring(0, 8))
                            : linkedBeacon.getName().copy();
                    player.displayClientMessage(
                            Component.literal("Must use key from linked beacon ")
                                    .append(linkedBeaconNameComponent)
                                    .append(" to unlink this Lectern"),
                            true
                    );
                }
            }
        } else {
            // TODO: Check if Lectern is within a configurable max range of the beacon.

            if (lectern.hasAttached(ModAttachments.LECTERN_TOURIST_BEACON_UUID)) {
                UUID attachedUUID = lectern.getAttached(ModAttachments.LECTERN_TOURIST_BEACON_UUID);
                if (!(keyBeaconUUID.equals(attachedUUID))) {
                    TouristBeaconBlockEntity linkedBeacon = TourismManager.getBeaconBlockEntityByUUID(attachedUUID);
                    MutableComponent linkedBeaconNameComponent = linkedBeacon == null
                            ? Component.literal(attachedUUID.toString().substring(0, 8))
                            : linkedBeacon.getName().copy();
                    player.displayClientMessage(
                            Component.literal("Must unlink Lectern from beacon ")
                                    .append(linkedBeaconNameComponent)
                                    .append(" first"),
                            true
                    );
                    return InteractionResult.FAIL;
                }
            }

            lectern.setAttached(ModAttachments.LECTERN_TOURIST_BEACON_UUID, keyBeaconUUID);
            if (TouristExperience.registerLecternIfLinked(lectern)) {
                player.displayClientMessage(
                        Component.literal("Linked Lectern to beacon ")
                                .append(keyBeaconNameComponent),
                        true
                );
            }
        }

        return InteractionResult.SUCCESS;
    }
}
