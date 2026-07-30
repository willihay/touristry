package org.bensam.touristry.client.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.CommonComponents;

@Environment(EnvType.CLIENT)
public class ExperienceScrollBoxButton extends Button.Plain {
    private static final int BUTTON_WIDTH = 88;
    private static final int BUTTON_HEIGHT = 20;

    private final int index;

    public ExperienceScrollBoxButton(int index, int x, int y, OnPress onPress) {
        super(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, CommonComponents.EMPTY, onPress, DEFAULT_NARRATION);
        this.index = index;
        this.visible = false;
    }

    public int getIndex() {
        return this.index;
    }
}
