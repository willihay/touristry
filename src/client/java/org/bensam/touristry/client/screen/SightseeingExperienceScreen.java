package org.bensam.touristry.client.screen;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.bensam.touristry.Touristry;
import org.bensam.touristry.client.screen.buttons.OnOffSliderButton;
import org.bensam.touristry.menu.SightseeingExperienceMenu;
import org.bensam.touristry.network.ExperienceScreenActionC2SPayload;
import org.bensam.touristry.tourism.experience.ExperienceScreenAction;
import org.jspecify.annotations.NonNull;

public class SightseeingExperienceScreen extends AbstractContainerScreen<SightseeingExperienceMenu> {
    //region Constants: Sprites & Textures
    // Screen textures
    private static final Identifier BG_TEXTURE = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "textures/gui/tourist_experience.png");
    //endregion

    //region Constants: Common
    private static final int ARGB_SCREEN_TEXT_COLOR = 0xFF404040; // gray
    //endregion

    //region Constants: Status Tab
    // Status screen constants
    private static final Component OPEN_FOR_BUSINESS_MESSAGE = Component.translatable("screen.touristry.tourist_block.status.open_for_business");
    private static final Component CLOSED_FOR_BUSINESS_MESSAGE = Component.translatable("screen.touristry.tourist_block.status.closed_for_business");
    private static final int REPUTATION_LABEL_X = 8;
    private static final int REPUTATION_LABEL_Y = 17;
    private static final int TARGET_KEY_LABEL_X = 8;
    private static final int TARGET_KEY_LABEL_Y = 38;
    private static final int ENTRY_FEE_LABEL_X = 8;
    private static final int ENTRY_FEE_LABEL_Y = 56;
    private static final int STATUS_LABEL_X = 116;
    private static final int STATUS_LABEL_Y = 72;
    private static final int ON_OFF_SLIDER_X = 152;
    private static final int ON_OFF_SLIDER_Y = 72;
    //endregion

    // Status screen fields
    private boolean openForBusiness;
    private OnOffSliderButton statusToggleButton;

    public SightseeingExperienceScreen(SightseeingExperienceMenu container, Inventory inventory, Component title) {
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

                            ClientPlayNetworking.send(new ExperienceScreenActionC2SPayload(
                                    this.menu.getContainerId(),
                                    ExperienceScreenAction.SET_OPEN_STATUS,
                                    this.openForBusiness ? 1 : 0,
                                    -1
                            ));
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
                "screen.touristry.tourist_block.reputation",
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

        Component targetKeyLabel = Component.translatable("screen.touristry.tourist_block.target_key.label");
        guiGraphics.drawString(
                this.font,
                targetKeyLabel,
                TARGET_KEY_LABEL_X,
                TARGET_KEY_LABEL_Y,
                ARGB_SCREEN_TEXT_COLOR,
                false
        );

        Component entryFeeLabel = Component.translatable("screen.touristry.tourist_block.entry_fee.label");
        guiGraphics.drawString(
                this.font,
                entryFeeLabel,
                ENTRY_FEE_LABEL_X,
                ENTRY_FEE_LABEL_Y,
                ARGB_SCREEN_TEXT_COLOR,
                false
        );

        Component statusLabel = Component.translatable("screen.touristry.tourist_block.status.label");
        guiGraphics.drawString(
                this.font,
                statusLabel,
                STATUS_LABEL_X,
                STATUS_LABEL_Y,
                ARGB_SCREEN_TEXT_COLOR,
                false
        );
    }
}
