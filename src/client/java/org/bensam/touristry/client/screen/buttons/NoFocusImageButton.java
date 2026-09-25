package org.bensam.touristry.client.screen.buttons;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;

public class NoFocusImageButton extends ImageButton {
    private static final int STANDARD_SPRITE_WIDTH = 32;
    private static final int STANDARD_SPRITE_HEIGHT = 32;

    private final int spriteWidth;
    private final int spriteHeight;
    private final int u;
    private final int v;
    private final int sourceWidth;
    private final int sourceHeight;

    public NoFocusImageButton(int x, int y, int drawWidth, int drawHeight, WidgetSprites widgetSprites, OnPress onPress) {
        super(x, y, drawWidth, drawHeight, widgetSprites, onPress);
        this.spriteWidth = 0;
        this.spriteHeight = 0;
        this.u = 0;
        this.v = 0;
        this.sourceWidth = 0;
        this.sourceHeight = 0;
    }

    public NoFocusImageButton(int x, int y, int drawWidth, int drawHeight, WidgetSprites widgetSprites, OnPress onPress, Component tooltipAndMessage) {
        super(x, y, drawWidth, drawHeight, widgetSprites, onPress, tooltipAndMessage);
        this.spriteWidth = 0;
        this.spriteHeight = 0;
        this.u = 0;
        this.v = 0;
        this.sourceWidth = 0;
        this.sourceHeight = 0;
        this.setTooltip(Tooltip.create(tooltipAndMessage));
    }

    public NoFocusImageButton(int x, int y, int drawWidth, int drawHeight, int u, int v, WidgetSprites widgetSprites, OnPress onPress) {
        this(x, y, drawWidth, drawHeight, u, v, drawWidth, drawHeight, widgetSprites, onPress);
    }

    public NoFocusImageButton(int x, int y, int drawWidth, int drawHeight, int u, int v, int sourceWidth, int sourceHeight, WidgetSprites widgetSprites, OnPress onPress) {
        super(x, y, drawWidth, drawHeight, widgetSprites, onPress);
        this.spriteWidth = STANDARD_SPRITE_WIDTH;
        this.spriteHeight = STANDARD_SPRITE_HEIGHT;
        this.u = u;
        this.v = v;
        this.sourceWidth = sourceWidth;
        this.sourceHeight = sourceHeight;
    }

    public NoFocusImageButton(int x, int y, int drawWidth, int drawHeight, int u, int v, WidgetSprites widgetSprites, OnPress onPress, Component tooltipAndMessage) {
        this(x, y, drawWidth, drawHeight, u, v, drawWidth, drawHeight, widgetSprites, onPress, tooltipAndMessage);
    }

    public NoFocusImageButton(int x, int y, int drawWidth, int drawHeight, int u, int v, int sourceWidth, int sourceHeight, WidgetSprites widgetSprites, OnPress onPress, Component tooltipAndMessage) {
        super(x, y, drawWidth, drawHeight, widgetSprites, onPress, tooltipAndMessage);
        this.spriteWidth = STANDARD_SPRITE_WIDTH;
        this.spriteHeight = STANDARD_SPRITE_HEIGHT;
        this.u = u;
        this.v = v;
        this.sourceWidth = sourceWidth;
        this.sourceHeight = sourceHeight;
        this.setTooltip(Tooltip.create(tooltipAndMessage));
    }

    @Override
    public void renderContents(GuiGraphics guiGraphics, int i, int j, float f) {
        // Renders like ImageButton except button does not remain highlighted after clicking.
        if (this.u == 0 && this.v == 0) {
            guiGraphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    this.sprites.get(this.isActive(), this.isHovered()),
                    this.getX(),
                    this.getY(),
                    this.width,
                    this.height);
            return;
        }

        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(this.getX(), this.getY());
        guiGraphics.pose().scale((float) this.width / this.sourceWidth, (float) this.height / this.sourceHeight);
        guiGraphics.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                this.sprites.get(this.isActive(), this.isHovered()),
                this.spriteWidth,
                this.spriteHeight,
                this.u,
                this.v,
                0,
                0,
                this.sourceWidth,
                this.sourceHeight
        );
        guiGraphics.pose().popMatrix();
    }
}
