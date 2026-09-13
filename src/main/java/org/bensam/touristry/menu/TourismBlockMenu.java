package org.bensam.touristry.menu;

import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.NonNull;

public interface TourismBlockMenu {
    boolean clickMenuButton(@NonNull Player player, int buttonId);
    int getContainerId();
    boolean isOpenForBusiness();
}
