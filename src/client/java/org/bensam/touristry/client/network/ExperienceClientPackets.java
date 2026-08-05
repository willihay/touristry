package org.bensam.touristry.client.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import org.bensam.touristry.menu.ShoppingExperienceMenu;
import org.bensam.touristry.client.render.ExperienceTargetOverlayRenderer;
import org.bensam.touristry.network.SyncTargetOverlayViewS2CPayload;
import org.bensam.touristry.network.SyncTargetViewS2CPayload;

public final class ExperienceClientPackets {
    private ExperienceClientPackets() {}

    public static void registerClientReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(SyncTargetViewS2CPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                if (context.client().player == null) {
                    return;
                }

                if (!(context.client().player.containerMenu instanceof ShoppingExperienceMenu menu)) {
                    return;
                }

                if (menu.getContainerId() != payload.containerId()) {
                    return;
                }

                menu.setSyncedTargets(payload.orderedTargets(), payload.targets());
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(SyncTargetOverlayViewS2CPayload.TYPE, (payload, context) ->
                context.client().execute(() -> ExperienceTargetOverlayRenderer.setTargets(
                        payload.experienceUUID(),
                        payload.targets()
                )));
    }
}
