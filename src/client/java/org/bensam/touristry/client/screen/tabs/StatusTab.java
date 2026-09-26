package org.bensam.touristry.client.screen.tabs;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.bensam.touristry.Touristry;
import org.bensam.touristry.client.screen.AbstractTabbedExperienceScreen;
import org.bensam.touristry.client.screen.ReputationColor;
import org.bensam.touristry.client.screen.buttons.OnOffSliderButton;
import org.bensam.touristry.menu.AbstractExperienceMenu;
import org.bensam.touristry.network.ExperienceScreenActionC2SPayload;
import org.bensam.touristry.tourism.experience.ExperienceScreenAction;

public class StatusTab<T extends Enum<T>> implements ScreenTab<T> {
    private static final Component TAB_TITLE = Component.translatable("screen.touristry.tourist_block.tab.status");

    //region Constants: Textures and sprites
    private static final Identifier BACKGROUND_TEXTURE = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "textures/gui/tourist_experience_status.png");
    //endregion

    //region Constants: Labels and positions
    private static final Component PAYMENTS_LABEL = Component.translatable("screen.touristry.tourist_block.payments.label");
    private static final Component TARGET_KEY_LABEL = Component.translatable("screen.touristry.tourist_block.target_key.label");
    private static final Component ENTRY_FEE_LABEL = Component.translatable("screen.touristry.tourist_block.entry_fee.label");
    private static final Component STATUS_LABEL = Component.translatable("screen.touristry.tourist_block.status.label");
    private static final Component OPEN_FOR_BUSINESS_MESSAGE = Component.translatable("screen.touristry.tourist_block.status.open_for_business");
    private static final Component CLOSED_FOR_BUSINESS_MESSAGE = Component.translatable("screen.touristry.tourist_block.status.closed_for_business");
    private static final int TITLE_LABEL_X = 8;
    private static final int TITLE_LABEL_Y = 6;
    private static final int REPUTATION_LABEL_X = 8;
    private static final int REPUTATION_LABEL_Y = 17;
    private static final int PAYMENTS_BOX_WIDTH = 54;
    private static final int PAYMENTS_BOX_X = 215;
    private static final int PAYMENTS_LABEL_Y = 6;
    private static final int STATUS_LABEL_X = 107;
    private static final int STATUS_LABEL_Y = 21;
    private static final int ON_OFF_SLIDER_X = 180;
    private static final int ON_OFF_SLIDER_Y = 21;
    private static final int TARGET_KEY_LABEL_X = STATUS_LABEL_X;
    private static final int TARGET_KEY_LABEL_Y = 39;
    private static final int ENTRY_FEE_LABEL_X = STATUS_LABEL_X;
    private static final int ENTRY_FEE_LABEL_Y = 57;
    private static final int INVENTORY_LABEL_X = 107;
    private static final int INVENTORY_LABEL_Y = 72;
    //endregion

    private final T tabEnum;
    private final ItemStack tabIcon;
    private int tabOrder;

    private boolean hasReceivedSync;
    private boolean openForBusiness;
    private OnOffSliderButton statusToggleButton;

    public StatusTab(T tabEnum, Item tabIcon) {
        this.tabEnum = tabEnum;
        this.tabIcon = new ItemStack(tabIcon);
    }

    // Screen setup handlers
    @Override
    public void onSelected(AbstractTabbedExperienceScreen<?, ?> screen, AbstractExperienceMenu<?> menu) {
        if (this.statusToggleButton == null) {
            this.statusToggleButton = screen.addButton(new OnOffSliderButton(
                    this.openForBusiness, // open == on, closed == off
                    screen.getScreenLeft() + ON_OFF_SLIDER_X,
                    screen.getScreenTop() + ON_OFF_SLIDER_Y,
                    OPEN_FOR_BUSINESS_MESSAGE,
                    CLOSED_FOR_BUSINESS_MESSAGE,
                    button -> {
                        if (button instanceof OnOffSliderButton onOffSliderButton) {
                            this.openForBusiness = !menu.isOpenForBusiness();
                            onOffSliderButton.setState(this.openForBusiness);

                            ClientPlayNetworking.send(new ExperienceScreenActionC2SPayload(
                                    menu.getContainerId(),
                                    ExperienceScreenAction.SET_OPEN_STATUS,
                                    this.openForBusiness ? 1 : 0,
                                    -1
                            ));
                        }
                    })
            );
            this.statusToggleButton.visible = this.hasReceivedSync;
        }
    }

    @Override
    public void onDeselected(AbstractTabbedExperienceScreen<?, ?> screen) {
        if (this.statusToggleButton != null) {
            screen.removeButton(this.statusToggleButton);
            this.statusToggleButton = null;
        }
    }

    @Override
    public void tick(AbstractExperienceMenu<?> menu) {
        int generation = menu.getSyncGeneration();
        boolean openStatus = menu.isOpenForBusiness();

        if (!hasReceivedSync && generation > 0) {
            // Set the initial value of openForBusiness, update the button state, and make the toggle button visible.
            this.hasReceivedSync = true;
            if (this.statusToggleButton != null) {
                this.statusToggleButton.visible = true;
                this.statusToggleButton.setState(openStatus);
                this.openForBusiness = openStatus;
            }
        } else if (openStatus != this.openForBusiness) {
            // Update the value of openForBusiness and the button state.
            this.openForBusiness = openStatus;
            if (this.statusToggleButton != null) {
                this.statusToggleButton.setState(openStatus);
            }
        }
    }

    // Tab properties
    @Override
    public T getTabEnum() {
        return this.tabEnum;
    }

    @Override
    public int getTabOrder() {
        return this.tabOrder;
    }

    @Override
    public Component getTabTitle() {
        return TAB_TITLE;
    }

    @Override
    public void setTabOrder(int tabOrder) {
        this.tabOrder = tabOrder;
    }

    // Render methods
    @Override
    public Identifier getBackground() {
        return BACKGROUND_TEXTURE;
    }

    @Override
    public ItemStack getTabIcon() {
        return this.tabIcon;
    }

    @Override
    public void renderContents(GuiGraphics guiGraphics, int mouseX, int mouseY, AbstractTabbedExperienceScreen<?, ?> screen, AbstractExperienceMenu<?> menu) {}

    @Override
    public void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY, AbstractTabbedExperienceScreen<?, ?> screen, AbstractExperienceMenu<?> menu) {
        guiGraphics.drawString(screen.getFont(), this.getTabTitle(), TITLE_LABEL_X, TITLE_LABEL_Y, AbstractTabbedExperienceScreen.ARGB_SCREEN_TEXT_COLOR, false);
        guiGraphics.drawString(screen.getFont(), screen.getPlayerInventoryTitle(), INVENTORY_LABEL_X, INVENTORY_LABEL_Y, AbstractTabbedExperienceScreen.ARGB_SCREEN_TEXT_COLOR, false);

        Component reputationLabel = Component.translatable(
                "screen.touristry.tourist_block.reputation",
                String.format("%.2f", menu.getReputation())
        );
        guiGraphics.drawString(
                screen.getFont(),
                reputationLabel,
                REPUTATION_LABEL_X,
                REPUTATION_LABEL_Y,
                ReputationColor.getColor(menu.getReputation()),
                false
        );

        int paymentsLabelWidth = screen.getFont().width(PAYMENTS_LABEL);
        guiGraphics.drawString(
                screen.getFont(),
                PAYMENTS_LABEL,
                PAYMENTS_BOX_X + ((PAYMENTS_BOX_WIDTH - paymentsLabelWidth) / 2),
                PAYMENTS_LABEL_Y,
                AbstractTabbedExperienceScreen.ARGB_SCREEN_TEXT_COLOR,
                false
        );

        guiGraphics.drawString(
                screen.getFont(),
                TARGET_KEY_LABEL,
                TARGET_KEY_LABEL_X,
                TARGET_KEY_LABEL_Y,
                AbstractTabbedExperienceScreen.ARGB_SCREEN_TEXT_COLOR,
                false
        );

        guiGraphics.drawString(
                screen.getFont(),
                ENTRY_FEE_LABEL,
                ENTRY_FEE_LABEL_X,
                ENTRY_FEE_LABEL_Y,
                AbstractTabbedExperienceScreen.ARGB_SCREEN_TEXT_COLOR,
                false
        );

        guiGraphics.drawString(
                screen.getFont(),
                STATUS_LABEL,
                STATUS_LABEL_X,
                STATUS_LABEL_Y,
                AbstractTabbedExperienceScreen.ARGB_SCREEN_TEXT_COLOR,
                false
        );
    }
}
