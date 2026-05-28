package org.bensam.touristry.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.bensam.touristry.ModComponents;

import java.util.UUID;

public class BeaconKeyItem extends Item {
    public BeaconKeyItem(Properties properties) {
        super(properties);
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

    public final UUID getBeaconUUID(ItemStack stack) {
        return stack.getOrDefault(ModComponents.TOURIST_BEACON_UUID, UUID.randomUUID());
    }

    public void setBeaconUUID(ItemStack stack, UUID beaconUUID) {
        stack.set(ModComponents.TOURIST_BEACON_UUID, beaconUUID);
    }
}
