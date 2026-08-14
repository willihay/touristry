package org.bensam.touristry.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.renderer.RenderPipelines;

public class NoFocusImageButton extends ImageButton {

    public NoFocusImageButton(int x, int y, int width, int height, WidgetSprites widgetSprites, OnPress onPress) {
        super(x, y, width, height, widgetSprites, onPress);
    }

    @Override
    public void renderContents(GuiGraphics guiGraphics, int i, int j, float f) {
        // Renders like ImageButton except button does not remain highlighted after clicking.
        guiGraphics.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                this.sprites.get(this.isActive(), this.isHovered()),
                this.getX(),
                this.getY(),
                this.width,
                this.height);
    }
}
