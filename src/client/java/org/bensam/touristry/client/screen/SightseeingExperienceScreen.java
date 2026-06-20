package org.bensam.touristry.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.bensam.touristry.Touristry;
import org.bensam.touristry.menu.SightseeingExperienceMenu;
import org.jspecify.annotations.NonNull;

public class SightseeingExperienceScreen extends AbstractContainerScreen<SightseeingExperienceMenu> {
    private static final Identifier BG_TEXTURE = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "textures/gui/tourist_experience.png");
    private static final Identifier ON_OFF_SLIDER_TEXTURE = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "textures/gui/on_off_slider.png");

    private static final int REPUTATION_LABEL_X = 8;
    private static final int REPUTATION_LABEL_Y = 17;
    private static final int STATUS_LABEL_X = 116;
    private static final int STATUS_LABEL_Y = 70;
    private static final int ON_OFF_SLIDER_X = 152;
    private static final int ON_OFF_SLIDER_Y = 70;

    public SightseeingExperienceScreen(SightseeingExperienceMenu container, Inventory inventory, Component title) {
        super(container, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        this.addRenderableWidget(new TourismStatusToggleButton(
                this.leftPos + ON_OFF_SLIDER_X,
                this.topPos + ON_OFF_SLIDER_Y,
                this.menu, ON_OFF_SLIDER_TEXTURE)
        );
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        super.render(guiGraphics, i, j, f);
        this.renderTooltip(guiGraphics, i, j);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float f, int i, int j) {
        int xo = (this.width - this.imageWidth) / 2;
        int yo = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(
                RenderPipelines.GUI_TEXTURED,
                BG_TEXTURE,
                xo,
                yo,
                0.0F,
                0.0F,
                this.imageWidth,
                this.imageHeight,
                BACKGROUND_TEXTURE_WIDTH,
                BACKGROUND_TEXTURE_HEIGHT
        );
    }

    @Override
    protected void renderLabels(@NonNull GuiGraphics guiGraphics, int i, int j) {
        super.renderLabels(guiGraphics, i, j);

        Component reputationLabel = Component.translatable(
                "screen." + Touristry.MOD_ID + ".tourism_status.reputation",
                String.format("%.2f", this.menu.getReputation())
        );

        guiGraphics.drawString(
                this.font,
                reputationLabel,
                REPUTATION_LABEL_X,
                REPUTATION_LABEL_Y,
                ReputationColor.getColor(this.menu.getReputation()),
                false
        );

        Component statusLabel = Component.translatable("screen." + Touristry.MOD_ID + ".tourism_status.status_label");

        guiGraphics.drawString(
                this.font,
                statusLabel,
                STATUS_LABEL_X,
                STATUS_LABEL_Y,
                0xFF404040, // gray
                false
        );
    }
}
