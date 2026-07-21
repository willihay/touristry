package org.bensam.touristry.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.bensam.touristry.Touristry;
import org.bensam.touristry.menu.TourismStatusMenu;
import org.bensam.touristry.menu.TouristBeaconMenu;

public class TourismStatusToggleButton extends AbstractButton {
    private static final int TEXTURE_WIDTH = 32;
    private static final int TEXTURE_HEIGHT = 32;
    private static final int BUTTON_WIDTH = 32;
    private static final int BUTTON_HEIGHT = 16;
    private static final int DRAW_WIDTH = 16;
    private static final int DRAW_HEIGHT = 8;
    private static final Component OPEN_FOR_BUSINESS_MESSAGE = Component.translatable("screen." + Touristry.MOD_ID + ".tourist_block.status.open_for_business");
    private static final Component CLOSED_FOR_BUSINESS_MESSAGE = Component.translatable("screen." + Touristry.MOD_ID + ".tourist_block.status.closed_for_business");

    private final TourismStatusMenu menu;
    private final Identifier texture;

    public TourismStatusToggleButton(int x, int y, TourismStatusMenu menu, Identifier texture) {
        super(x, y, DRAW_WIDTH, DRAW_HEIGHT, CLOSED_FOR_BUSINESS_MESSAGE);
        this.menu = menu;
        this.texture = texture;
        this.refreshAccessibilityText();
    }

    @Override
    public void onPress(InputWithModifiers inputWithModifiers) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player != null
                && minecraft.gameMode != null
                && this.menu.clickMenuButton(minecraft.player, TouristBeaconMenu.BUTTON_TOGGLE_OPEN_FOR_BUSINESS)) {
            minecraft.gameMode.handleInventoryButtonClick(this.menu.getContainerId(), TouristBeaconMenu.BUTTON_TOGGLE_OPEN_FOR_BUSINESS);
        }
    }

    @Override
    protected void renderContents(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.refreshAccessibilityText();

        int u = 0;
        int v = this.menu.isOpenForBusiness() ? 0 : TEXTURE_HEIGHT / 2;

        guiGraphics.blit(
                RenderPipelines.GUI_TEXTURED,
                this.texture,
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

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        this.refreshAccessibilityText();
        this.defaultButtonNarrationText(narrationElementOutput);
    }

    private void refreshAccessibilityText() {
        Component message = this.menu.isOpenForBusiness() ? OPEN_FOR_BUSINESS_MESSAGE : CLOSED_FOR_BUSINESS_MESSAGE;
        this.setMessage(message);
        this.setTooltip(Tooltip.create(message));
    }
}
