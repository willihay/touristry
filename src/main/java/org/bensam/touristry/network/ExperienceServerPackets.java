package org.bensam.touristry.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.bensam.touristry.menu.AbstractExperienceMenu;

public class ExperienceServerPackets {
    private ExperienceServerPackets() {}

    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(ExperienceScreenActionC2SPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> {
                if (!(context.player().containerMenu instanceof AbstractExperienceMenu<?> menu)) {
                    return;
                }

                if (menu.getContainerId() != payload.containerId()) {
                    return;
                }

                menu.handleScreenAction(context.player(), payload);
            });
        });
    }
}
