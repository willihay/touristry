package org.bensam.touristry.menu;

import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.NonNull;

public interface TourismStatusMenu {
    int BUTTON_TOGGLE_OPEN_FOR_BUSINESS = 0;
    int BUTTON_SELECT_STATUS = 1;
    int BUTTON_SELECT_TARGETS = 2;
    int BUTTON_SELECT_PRICING = 3;
    int BUTTON_MOVE_TARGET_UP = 4;
    int BUTTON_MOVE_TARGET_DOWN = 5;

    boolean clickMenuButton(@NonNull Player player, int buttonId);
    int getContainerId();
    boolean isOpenForBusiness();
}
