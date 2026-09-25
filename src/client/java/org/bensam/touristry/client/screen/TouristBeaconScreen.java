package org.bensam.touristry.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.bensam.touristry.Touristry;
import org.bensam.touristry.client.screen.buttons.OnOffSliderButton;
import org.bensam.touristry.menu.TouristBeaconMenu;
import org.jspecify.annotations.NonNull;

public class TouristBeaconScreen extends AbstractContainerScreen<TouristBeaconMenu> {
    private static final Identifier BG_TEXTURE = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "textures/gui/tourist_beacon.png");

    private static final Component OPEN_FOR_BUSINESS_MESSAGE = Component.translatable("screen." + Touristry.MOD_ID + ".tourist_block.status.open_for_business");
    private static final Component CLOSED_FOR_BUSINESS_MESSAGE = Component.translatable("screen." + Touristry.MOD_ID + ".tourist_block.status.closed_for_business");
    private static final int REPUTATION_LABEL_X = 8;
    private static final int REPUTATION_LABEL_Y = 17;
    private static final int STATUS_LABEL_X = 116;
    private static final int STATUS_LABEL_Y = 72;
    private static final int ON_OFF_SLIDER_X = 152;
    private static final int ON_OFF_SLIDER_Y = 72;

    private boolean openForBusiness;
    private OnOffSliderButton statusToggleButton;

    public TouristBeaconScreen(TouristBeaconMenu container, Inventory inventory, Component title) {
        super(container, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        this.addStatusToggleButton();
    }

    @Override
    protected void containerTick() {
        super.containerTick();

        if (this.menu.isOpenForBusiness() != this.openForBusiness) {
            this.openForBusiness = this.menu.isOpenForBusiness();

            if (this.statusToggleButton != null) {
                this.statusToggleButton.setState(this.openForBusiness);
            }
        }
    }

    private void addStatusToggleButton() {
        if (this.statusToggleButton == null) {
            this.statusToggleButton = this.addRenderableWidget(new OnOffSliderButton(
                    this.openForBusiness, // open == on, closed == off
                    this.leftPos + ON_OFF_SLIDER_X,
                    this.topPos + ON_OFF_SLIDER_Y,
                    OPEN_FOR_BUSINESS_MESSAGE,
                    CLOSED_FOR_BUSINESS_MESSAGE,
                    button -> {
                        if (button instanceof OnOffSliderButton onOffSliderButton) {
                            this.openForBusiness = !this.menu.isOpenForBusiness();
                            onOffSliderButton.setState(this.openForBusiness);

                            Minecraft minecraft = Minecraft.getInstance();
                            if (minecraft.player != null
                                    && minecraft.gameMode != null
                                    && this.menu.clickMenuButton(minecraft.player, TouristBeaconMenu.BUTTON_TOGGLE_OPEN_FOR_BUSINESS)) {
                                minecraft.gameMode.handleInventoryButtonClick(this.menu.getContainerId(), TouristBeaconMenu.BUTTON_TOGGLE_OPEN_FOR_BUSINESS);
                            }
                        }
                    })
            );
        }
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
                "screen." + Touristry.MOD_ID + ".tourist_block.reputation",
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

        Component statusLabel = Component.translatable("screen." + Touristry.MOD_ID + ".tourist_block.status.label");

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
