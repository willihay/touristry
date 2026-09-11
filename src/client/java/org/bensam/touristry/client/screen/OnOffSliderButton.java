package org.bensam.touristry.client.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.bensam.touristry.Touristry;

@Environment(EnvType.CLIENT)
public class OnOffSliderButton extends Button {
    private static final Identifier ON_OFF_SLIDER_TEXTURE = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "textures/gui/on_off_slider.png");
    private static final int TEXTURE_WIDTH = 32;
    private static final int TEXTURE_HEIGHT = 32;
    private static final int BUTTON_WIDTH = 32;
    private static final int BUTTON_HEIGHT = 16;
    private static final int DRAW_WIDTH = 16;
    private static final int DRAW_HEIGHT = 8;

    private boolean isOn;
    private final Component onMessage;
    private final Component offMessage;

    protected OnOffSliderButton(boolean isOn, int x, int y, Component onMessage, Component offMessage, OnPress onPress) {
        super(x, y, DRAW_WIDTH, DRAW_HEIGHT, CommonComponents.EMPTY, onPress, DEFAULT_NARRATION);
        this.onMessage = onMessage;
        this.offMessage = offMessage;
        this.setState(isOn);
    }

    private void refreshAccessibilityText() {
        Component message = this.isOn ? onMessage : offMessage;
        this.setMessage(message);
        this.setTooltip(Tooltip.create(message));
    }

    @Override
    protected void renderContents(GuiGraphics guiGraphics, int i, int j, float f) {
        int u = 0;
        int v = this.isOn ? 0 : TEXTURE_HEIGHT / 2;

        guiGraphics.blit(
                RenderPipelines.GUI_TEXTURED,
                ON_OFF_SLIDER_TEXTURE,
                this.getX(),
                this.getY(),
                (float) u,
                (float) v,
                this.width,
                this.height,
                BUTTON_WIDTH,
                BUTTON_HEIGHT,
                TEXTURE_WIDTH,
                TEXTURE_HEIGHT
        );
    }

    public void setState(boolean isOn) {
        this.isOn = isOn;
        this.refreshAccessibilityText();
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        this.refreshAccessibilityText();
        this.defaultButtonNarrationText(narrationElementOutput);
    }
}
