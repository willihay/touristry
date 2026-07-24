package org.bensam.touristry.client.screen;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.*;
import org.bensam.touristry.ModBlocks;
import org.bensam.touristry.ModItems;
import org.bensam.touristry.Touristry;
import org.bensam.touristry.menu.ShoppingExperienceMenu;
import org.jspecify.annotations.NonNull;

public class ShoppingExperienceScreen extends AbstractContainerScreen<ShoppingExperienceMenu> {
    // Screen textures
    private static final Identifier BG_TEXTURE_STATUS = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "textures/gui/tourist_experience_status.png");
    private static final Identifier BG_TEXTURE_TARGETS = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "textures/gui/tourist_experience_targets.png");
    private static final Identifier BG_TEXTURE_PRICING = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "textures/gui/tourist_experience_pricing.png");

    // Tab textures
    private static final Identifier[] UNSELECTED_TOP_TABS = new Identifier[]{
            Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "tab_top_unselected_1"),
            Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "tab_top_unselected_2"),
            Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "tab_top_unselected_3")
    };
    private static final Identifier[] SELECTED_TOP_TABS = new Identifier[]{
            Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "tab_top_selected_1"),
            Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "tab_top_selected_2"),
            Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "tab_top_selected_3")
    };

    // Sprite textures
    private static final Identifier ON_OFF_SLIDER_TEXTURE = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "textures/gui/on_off_slider.png");
    private static final Identifier SCROLLER_SPRITE = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "scroller");
    private static final Identifier SCROLLER_DISABLED_SPRITE = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "scroller_disabled");

    // Common constants
    private static final int TAB_WIDTH = 26;
    private static final int TAB_HEIGHT = 32;
    private static final int SCROLLER_WIDTH = 12;
    private static final int SCROLLER_HEIGHT = 15;

    // Status screen constants
    private static final Component TITLE_STATUS = Component.translatable("screen.touristry.tourist_block.tab.status");
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

    // Targets screen constants
    private static final Component TITLE_TARGETS = Component.translatable("screen.touristry.tourist_block.tab.targets");
    private static final int NUM_ROWS = 5;
    private static final int NUM_COLS = 9;

    // Pricing screen constants
    private static final Component TITLE_PRICING = Component.translatable("screen.touristry.tourist_block.tab.pricing");


    private enum TabDisplay {
        STATUS(ShoppingExperienceMenu.Tab.STATUS, BG_TEXTURE_STATUS, ModBlocks.SHOPPING_EXPERIENCE.get().asItem(), TITLE_STATUS, false),
        TARGETS(ShoppingExperienceMenu.Tab.TARGETS, BG_TEXTURE_TARGETS, ModItems.EXPERIENCE_TARGET_KEY.get(), TITLE_TARGETS, true),
        PRICING(ShoppingExperienceMenu.Tab.PRICING, BG_TEXTURE_PRICING, Items.EMERALD, TITLE_PRICING, true);

        private final ShoppingExperienceMenu.Tab menuTab;
        private final Identifier background;
        private final ItemStack icon;
        private final Component title;
        private final boolean canScroll;

        TabDisplay(ShoppingExperienceMenu.Tab menuTab, Identifier background, Item icon, Component title, boolean canScroll) {
            this.menuTab = menuTab;
            this.canScroll = canScroll;
            this.background = background;
            this.icon = new ItemStack(icon);
            this.title = title;
        }

        public boolean canScroll() {
            return this.canScroll;
        }

        public Identifier getBackground() {
            return this.background;
        }

        public ItemStack getIcon() {
            return this.icon;
        }

        public ShoppingExperienceMenu.Tab getMenuTab() {
            return this.menuTab;
        }

        public Identifier getTexture(TabDisplay selectedTab) {
            if (this == selectedTab) {
                return SELECTED_TOP_TABS[this.ordinal()];
            } else {
                return UNSELECTED_TOP_TABS[this.ordinal()];
            }
        }

        public Component getTitle() {
            return this.title;
        }

        public int getTabX() {
            return (TAB_WIDTH + 1) * this.ordinal();
        }

        public int getTabY() {
            return -(TAB_HEIGHT - 4);
        }
    }

    private boolean scrolling;
    private TabDisplay selectedTab = TabDisplay.STATUS;
    private GuiEventListener statusToggleButton;

    public ShoppingExperienceScreen(ShoppingExperienceMenu container, Inventory inventory, Component title) {
        super(container, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        this.addStatusToggleButton();
    }

    private void addStatusToggleButton() {
        this.statusToggleButton = this.addRenderableWidget(new TourismStatusToggleButton(
                this.leftPos + ON_OFF_SLIDER_X,
                this.topPos + ON_OFF_SLIDER_Y,
                this.menu, ON_OFF_SLIDER_TEXTURE)
        );
    }

    private void removeStatusToggleButton() {
        if (this.statusToggleButton != null) {
            this.removeWidget(this.statusToggleButton);
            this.statusToggleButton = null;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        super.render(guiGraphics, i, j, f);

        for (TabDisplay tab : TabDisplay.values()) {
            this.checkTabHovering(guiGraphics, tab, i, j);
        }

        this.renderTooltip(guiGraphics, i, j);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float f, int i, int j) {
        // Render unselected tabs.
        for (TabDisplay tab : TabDisplay.values()) {
            if (tab != this.selectedTab) {
                this.renderTabButton(guiGraphics, i, j, tab);
            }
        }
        // Render background.
        int xo = (this.width - this.imageWidth) / 2;
        int yo = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(
                RenderPipelines.GUI_TEXTURED,
                this.selectedTab.getBackground(),
                xo,
                yo,
                0.0F,
                0.0F,
                this.imageWidth,
                this.imageHeight,
                BACKGROUND_TEXTURE_WIDTH,
                BACKGROUND_TEXTURE_HEIGHT
        );

        // Render selected tab.
        this.renderTabButton(guiGraphics, i, j, this.selectedTab);
    }

    private void renderTabButton(GuiGraphics guiGraphics, int i, int j, TabDisplay tab) {
        boolean isSelectedTab = tab == this.selectedTab;
        int xTab = this.leftPos + tab.getTabX();
        int yTab = this.topPos + tab.getTabY();
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, tab.getTexture(this.selectedTab), xTab, yTab, 26, 32);

        if (!isSelectedTab && i > xTab && j > yTab && i < xTab + TAB_WIDTH && j < yTab + TAB_HEIGHT) {
            guiGraphics.requestCursor(CursorTypes.POINTING_HAND);
        }

        int xIcon = xTab + 13 - 8;
        int yIcon = yTab + 17 - 8;
        guiGraphics.renderItem(tab.getIcon(), xIcon, yIcon);
    }

    @Override
    protected void renderLabels(@NonNull GuiGraphics guiGraphics, int i, int j) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, -12566464, false);
        if (this.selectedTab == TabDisplay.STATUS) {
            guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, -12566464, false);
        }

        switch (this.selectedTab) {
            case STATUS -> this.renderStatusLabels(guiGraphics, i, j);
            case TARGETS -> this.renderTargetLabels(guiGraphics, i, j);
            case PRICING -> this.renderPricingLabels(guiGraphics, i, j);
        }
    }

    private void renderStatusLabels(@NonNull GuiGraphics guiGraphics, int i, int j) {
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

        Component targetKeyLabel = Component.translatable("screen." + Touristry.MOD_ID + ".tourist_block.target_key.label");
        guiGraphics.drawString(
                this.font,
                targetKeyLabel,
                TARGET_KEY_LABEL_X,
                TARGET_KEY_LABEL_Y,
                0xFF404040, // gray
                false
        );

        Component entryFeeLabel = Component.translatable("screen." + Touristry.MOD_ID + ".tourist_block.entry_fee.label");
        guiGraphics.drawString(
                this.font,
                entryFeeLabel,
                ENTRY_FEE_LABEL_X,
                ENTRY_FEE_LABEL_Y,
                0xFF404040, // gray
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

    private void renderTargetLabels(@NonNull GuiGraphics guiGraphics, int i, int j) {

    }

    private void renderPricingLabels(@NonNull GuiGraphics guiGraphics, int i, int j) {

    }

    private boolean canScroll() {
        return selectedTab.canScroll() /* && this.menu.canScroll() */;
    }

    private boolean checkTabClicked(TabDisplay tab, double d, double e) {
        int i = tab.getTabX();
        int j = tab.getTabY();
        return d >= i && d <= i + TAB_WIDTH && e >= j && e <= j + TAB_HEIGHT;
    }

    private boolean checkTabHovering(GuiGraphics guiGraphics, TabDisplay tab, int i, int j) {
        int k = tab.getTabX();
        int l = tab.getTabY();
        if (this.isHovering(k + 3, l + 3, 21, 27, i, j)) {
            guiGraphics.setTooltipForNextFrame(this.font, tab.getTitle(), i, j);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean bl) {
        if (mouseButtonEvent.button() == 0) {
            double d = mouseButtonEvent.x() - this.leftPos;
            double e = mouseButtonEvent.y() - this.topPos;

            for (TabDisplay tab : TabDisplay.values()) {
                if (this.checkTabClicked(tab, d, e)) {
                    return true;
                }
            }

            /*
            if (selectedTab == Tab.TARGETS && this.insideTargetsScrollbar(mouseButtonEvent.x(), mouseButtonEvent.y())) {
                this.scrolling = this.canScroll();
                return true;
            } else if (selectedTab == Tab.PRICING && this.insidePricingScrollbar(mouseButtonEvent.x(), mouseButtonEvent.y())) {
                this.scrolling = this.canScroll();
                return true;
            }
            */
        }

        return super.mouseClicked(mouseButtonEvent, bl);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent mouseButtonEvent) {
        if (mouseButtonEvent.button() == 0) {
            double d = mouseButtonEvent.x() - this.leftPos;
            double e = mouseButtonEvent.y() - this.topPos;
            this.scrolling = false;

            for (TabDisplay tab : TabDisplay.values()) {
                if (this.checkTabClicked(tab, d, e)) {
                    this.selectTab(tab);
                    return true;
                }
            }
        }

        return super.mouseReleased(mouseButtonEvent);
    }

    private void selectTab(TabDisplay tab) {
        if (tab == this.selectedTab) {
            return;
        }

        this.selectedTab = tab;
        this.menu.setSelectedTab(tab.getMenuTab());

        if (tab == TabDisplay.STATUS) {
            this.addStatusToggleButton();
        } else {
            this.removeStatusToggleButton();
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(
                    this.menu.getContainerId(),
                    ShoppingExperienceMenu.buttonForTab(tab.getMenuTab())
            );
        }
    }
}
