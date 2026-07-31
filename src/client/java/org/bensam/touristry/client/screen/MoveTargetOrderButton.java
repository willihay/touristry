package org.bensam.touristry.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class MoveTargetOrderButton extends ImageButton {
    private static final int TEXTURE_WIDTH = 32;
    private static final int TEXTURE_HEIGHT = 32;
    private static final int DRAW_WIDTH = 12;
    private static final int DRAW_HEIGHT = 8;

    private final int u;
    private final int v;

    public MoveTargetOrderButton(int x, int y, int u, int v, WidgetSprites widgetSprites, OnPress onPress, Component tooltip) {
        super(x, y, DRAW_WIDTH, DRAW_HEIGHT, widgetSprites, onPress, tooltip);
        this.setTooltip(Tooltip.create(tooltip));
        this.u = u;
        this.v = v;
    }

    @Override
    public void onPress(InputWithModifiers inputWithModifiers) {
        super.onPress(inputWithModifiers);
        this.setFocused(false);
    }

    @Override
    public void renderContents(GuiGraphics guiGraphics, int i, int j, float f) {
        Identifier identifier = this.sprites.get(this.isActive(), this.isHoveredOrFocused());
        guiGraphics.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                identifier,
                TEXTURE_WIDTH,
                TEXTURE_HEIGHT,
                this.u,
                this.v,
                this.getX(),
                this.getY(),
                this.width,
                this.height
        );
    }
}
