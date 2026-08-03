package org.bensam.touristry.client.screen;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.*;
import org.bensam.touristry.ModBlocks;
import org.bensam.touristry.ModItems;
import org.bensam.touristry.Touristry;
import org.bensam.touristry.menu.ShoppingExperienceMenu;
import org.bensam.touristry.network.ExperienceScreenActionC2SPayload;
import org.bensam.touristry.tourism.experience.ExperienceScreenAction;
import org.bensam.touristry.tourism.experience.TargetView;
import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.List;

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

    // Sprites and textures
    private static final Identifier ON_OFF_SLIDER_TEXTURE = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "textures/gui/on_off_slider.png");
    private static final Identifier WIDE_CHEST_TEXTURE = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "textures/gui/wide_chest.png");
    private static final Identifier SCROLLER_SPRITE = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "scroller");
    private static final Identifier SCROLLER_DISABLED_SPRITE = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "scroller_disabled");
    private static final Identifier MOVE_UP_SPRITE = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "move_up");
    private static final Identifier MOVE_UP_HIGHLIGHTED_SPRITE = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "move_up_highlighted");
    private static final Identifier MOVE_DOWN_SPRITE = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "move_down");
    private static final Identifier MOVE_DOWN_HIGHLIGHTED_SPRITE = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "move_down_highlighted");
    private static final Identifier REMOVE_SPRITE = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "trash");
    private static final Identifier REMOVE_SPRITE_HIGHLIGHTED = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "trash_selected");
    private static final Identifier REMOVE_ALL_SPRITE = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "sweep");
    private static final Identifier REMOVE_ALL_SPRITE_HIGHLIGHTED = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "sweep_selected");

    // Common constants
    private static final int ARGB_SCREEN_TEXT = 0xFF404040;
    private static final int ARGB_SCROLL_BUTTON_LABEL = 0xFFFFFFFF;
    private static final int BG_TEXTURE_WIDTH = 512;
    private static final int BG_TEXTURE_HEIGHT = 256;
    private static final int BG_SCREEN_WIDTH = 276;
    private static final int BG_SCREEN_HEIGHT = 166;
    private static final int TAB_WIDTH = 26;
    private static final int TAB_HEIGHT = 32;
    private static final int SCROLLBOX_WIDTH = 96;
    private static final int SCROLLBOX_LABEL_Y = 6;
    private static final int SCROLLBOX_ROW_X = 5;
    private static final int SCROLLBOX_TOP_Y = 18;
    private static final int SCROLLBOX_ROW_WIDTH = 88;
    private static final int SCROLLBOX_ROW_HEIGHT = 20;
    private static final int SCROLLBOX_ROWS = 7;
    private static final int SCROLLER_TRACK_X = 94;
    private static final int SCROLLER_TRACK_TOP_Y = SCROLLBOX_TOP_Y;
    private static final int SCROLLER_TRACK_BOTTOM_Y = 157;
    private static final int SCROLLER_TRACK_LENGTH = SCROLLER_TRACK_BOTTOM_Y - SCROLLBOX_TOP_Y + 1;
    private static final int SCROLLER_WIDTH = 6;
    private static final int SCROLLER_HEIGHT = 27;

    // Status screen constants
    private static final Component STATUS_SCREEN_TITLE = Component.translatable("screen.touristry.tourist_block.tab.status");
    private static final Component PAYMENTS_LABEL = Component.translatable("screen." + Touristry.MOD_ID + ".tourist_block.payments.label");
    private static final Component TARGET_KEY_LABEL = Component.translatable("screen." + Touristry.MOD_ID + ".tourist_block.target_key.label");
    private static final Component ENTRY_FEE_LABEL = Component.translatable("screen." + Touristry.MOD_ID + ".tourist_block.entry_fee.label");
    private static final Component STATUS_LABEL = Component.translatable("screen." + Touristry.MOD_ID + ".tourist_block.status.label");
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

    // Targets screen constants
    private static final Component TARGETS_SCREEN_TITLE = Component.translatable("screen.touristry.tourist_block.tab.targets");
    private static final Component TARGET_ORDERED_LABEL = Component.translatable("screen." + Touristry.MOD_ID + ".tourist_block.targets.ordered_button.label");
    private static final Component TARGET_MOVE_UP_TOOLTIP = Component.translatable("screen." + Touristry.MOD_ID + ".tourist_block.target.move_up.tooltip");
    private static final Component TARGET_MOVE_DOWN_TOOLTIP = Component.translatable("screen." + Touristry.MOD_ID + ".tourist_block.target.move_down.tooltip");
    private static final Component TARGET_REMOVE_TOOLTIP = Component.translatable("screen." + Touristry.MOD_ID + ".tourist_block.target.remove.tooltip");
    private static final Component TARGET_REMOVE_ALL_TOOLTIP = Component.translatable("screen." + Touristry.MOD_ID + ".tourist_block.targets.remove_all.tooltip");
    private static final int TARGET_ORDERED_LABEL_X = 127;
    private static final int TARGET_ORDERED_LABEL_Y = SCROLLBOX_TOP_Y + 5;
    private static final int TARGET_ORDERED_BUTTON_X = 193;
    private static final int TARGET_ORDERED_BUTTON_Y = SCROLLBOX_TOP_Y;
    private static final int TARGET_CHANGE_ORDER_BUTTON_X = 107;
    private static final int TARGET_CHANGE_ORDER_BUTTON_Y = 44;
    private static final int TARGET_DETAILS_LABEL_X = 127;
    private static final int TARGET_DETAILS_LABEL_Y = 50;
    private static final int TARGET_REMOVE_BUTTON_X = BG_SCREEN_WIDTH - 22;
    private static final int TARGET_REMOVE_BUTTON_Y = TARGET_DETAILS_LABEL_Y + 8;
    private static final int TARGET_REMOVE_ALL_BUTTON_X = BG_SCREEN_WIDTH - 22;
    private static final int TARGET_REMOVE_ALL_BUTTON_Y = BG_SCREEN_HEIGHT - 22;

    // Pricing screen constants
    private static final Component PRICING_SCREEN_TITLE = Component.translatable("screen.touristry.tourist_block.tab.pricing");

    private enum TabDisplay {
        STATUS(ShoppingExperienceMenu.Tab.STATUS, BG_TEXTURE_STATUS, ModBlocks.SHOPPING_EXPERIENCE.get().asItem(), STATUS_SCREEN_TITLE, false),
        TARGETS(ShoppingExperienceMenu.Tab.TARGETS, BG_TEXTURE_TARGETS, ModItems.EXPERIENCE_TARGET_KEY.get(), TARGETS_SCREEN_TITLE, true),
        PRICING(ShoppingExperienceMenu.Tab.PRICING, BG_TEXTURE_PRICING, Items.EMERALD, PRICING_SCREEN_TITLE, true);

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

    // Common fields
    private boolean isScrolling;
    private TabDisplay selectedTab = TabDisplay.STATUS;

    // Status screen fields
    private GuiEventListener statusToggleButton;

    // Targets screen fields
    private int selectedTargetIndex;
    private int targetsScrolledOff;
    private final ExperienceScrollBoxButton[] targetButtons = new ExperienceScrollBoxButton[SCROLLBOX_ROWS];
    private TargetOrderedButton targetOrderedToggleButton;
    private MoveTargetOrderButton targetOrderUpButton;
    private MoveTargetOrderButton targetOrderDownButton;
    private ImageButton targetRemoveButton;
    private ImageButton targetRemoveAllButton;

    // Pricing screen fields
    private int selectedPricingIndex;
    private int pricingScrolledOff;
    private final ExperienceScrollBoxButton[] pricingButtons = new ExperienceScrollBoxButton[SCROLLBOX_ROWS];

    public ShoppingExperienceScreen(ShoppingExperienceMenu container, Inventory inventory, Component title) {
        super(container, inventory, title);
        this.imageWidth = BG_SCREEN_WIDTH;
        this.inventoryLabelX = 107;
        this.selectedPricingIndex = -1;
        this.selectedTargetIndex = -1;
    }

    @Override
    protected void init() {
        super.init();
        this.selectTab(this.selectedTab);
        ClientPlayNetworking.send(new ExperienceScreenActionC2SPayload(
                this.menu.getContainerId(),
                ExperienceScreenAction.REQUEST_TARGETS,
                -1,
                -1
        ));
    }

    private void removeAllButtons() {
        this.removeStatusToggleButton();
        this.removeTargetButtons();
        this.removePricingButtons();
    }

    private void addStatusToggleButton() {
        if (this.statusToggleButton == null) {
            this.statusToggleButton = this.addRenderableWidget(new TourismStatusToggleButton(
                    this.leftPos + ON_OFF_SLIDER_X,
                    this.topPos + ON_OFF_SLIDER_Y,
                    this.menu, ON_OFF_SLIDER_TEXTURE)
            );
        }
    }

    private void removeStatusToggleButton() {
        if (this.statusToggleButton != null) {
            this.removeWidget(this.statusToggleButton);
            this.statusToggleButton = null;
        }
    }

    private void addTargetButtons() {
        if (this.targetButtons[0] != null) {
            // already populated
            return;
        }

        // Add target row buttons for scroll box.
        int buttonX = this.leftPos + SCROLLBOX_ROW_X;
        int buttonY = this.topPos + SCROLLBOX_TOP_Y;

        for (int m = 0; m < SCROLLBOX_ROWS; m++) {
            this.targetButtons[m] = this.addRenderableWidget(new ExperienceScrollBoxButton(m, buttonX, buttonY, button -> {
                if (button instanceof ExperienceScrollBoxButton selectedButton) {
                    this.selectTargetRow(selectedButton.getIndex());
                }
            }));
            buttonY += SCROLLBOX_ROW_HEIGHT;
        }

        // Set the focus if there already is one.
        this.updateRowFocusForSelectedTarget();

        // Add target order toggle button.
        buttonX = this.leftPos + TARGET_ORDERED_BUTTON_X;
        buttonY = this.topPos + TARGET_ORDERED_BUTTON_Y;
        this.targetOrderedToggleButton = this.addRenderableWidget(new TargetOrderedButton(
                this.menu.getSyncedOrderedTargets(),
                buttonX,
                buttonY,
                button -> {
                    if (button instanceof TargetOrderedButton orderedButton) {
                        boolean isOrdered = !this.menu.getSyncedOrderedTargets();
                        orderedButton.setOrdered(isOrdered);
                        button.setFocused(false);
                        this.updateRowFocusForSelectedTarget();

                        ClientPlayNetworking.send(new ExperienceScreenActionC2SPayload(
                                this.menu.getContainerId(),
                                ExperienceScreenAction.SET_ORDERED_TARGETS,
                                isOrdered ? 1 : 0,
                                -1
                        ));
                    }
                }
        ));

        // Add target move up button.
        buttonX = this.leftPos + TARGET_CHANGE_ORDER_BUTTON_X;
        buttonY = this.topPos + TARGET_CHANGE_ORDER_BUTTON_Y;
        int u = 18;
        int v = 4;
        this.targetOrderUpButton = this.addRenderableWidget(new MoveTargetOrderButton(
                buttonX, buttonY,
                u, v,
                new WidgetSprites(MOVE_UP_SPRITE, MOVE_UP_HIGHLIGHTED_SPRITE),
                button -> {
                    if (this.selectedTargetIndex <= 0 || this.selectedTargetIndex >= this.menu.getSyncedTargets().size()) {
                        // Selected target index is invalid or is already at the top of the list.
                        return;
                    }
                    ClientPlayNetworking.send(new ExperienceScreenActionC2SPayload(
                            this.menu.getContainerId(),
                            ExperienceScreenAction.MOVE_TARGET,
                            this.selectedTargetIndex,
                            this.selectedTargetIndex - 1
                    ));
                    this.selectTargetIndex(this.selectedTargetIndex - 1);
                },
                TARGET_MOVE_UP_TOOLTIP
        ));

        // Add target move down button.
        buttonY += 12;
        v = 20;
        this.targetOrderDownButton = this.addRenderableWidget(new MoveTargetOrderButton(
                buttonX, buttonY,
                u, v,
                new WidgetSprites(MOVE_DOWN_SPRITE, MOVE_DOWN_HIGHLIGHTED_SPRITE),
                button -> {
                    if (this.selectedTargetIndex < 0 || this.selectedTargetIndex >= this.menu.getSyncedTargets().size() - 1) {
                        // Selected target index is invalid or is already at the bottom of the list.
                        return;
                    }
                    ClientPlayNetworking.send(new ExperienceScreenActionC2SPayload(
                            this.menu.getContainerId(),
                            ExperienceScreenAction.MOVE_TARGET,
                            this.selectedTargetIndex,
                            this.selectedTargetIndex + 1
                    ));
                    this.selectTargetIndex(this.selectedTargetIndex + 1);
                },
                TARGET_MOVE_DOWN_TOOLTIP
        ));

        buttonX = this.leftPos + TARGET_REMOVE_BUTTON_X;
        buttonY = this.topPos + TARGET_REMOVE_BUTTON_Y;
        this.targetRemoveButton = this.addRenderableWidget(new ImageButton(
                buttonX, buttonY,
                16, 16,
                new WidgetSprites(REMOVE_SPRITE, REMOVE_SPRITE_HIGHLIGHTED),
                button -> {
                    int numTargets = this.menu.getSyncedTargets().size();
                    if (this.selectedTargetIndex < 0 || this.selectedTargetIndex >= numTargets) {
                        // Selected target index is invalid.
                        this.selectTargetIndex(-1);
                        return;
                    }
                    ClientPlayNetworking.send(new ExperienceScreenActionC2SPayload(
                            this.menu.getContainerId(),
                            ExperienceScreenAction.REMOVE_TARGET,
                            this.selectedTargetIndex,
                            -1
                    ));
                    // Adjust scroll position as needed.
                    numTargets--;
                    if (numTargets - this.targetsScrolledOff < SCROLLBOX_ROWS) {
                        this.targetsScrolledOff = Math.max(0, this.targetsScrolledOff - 1);
                    }
                    this.selectTargetIndex(-1);
                }
        ));
        this.targetRemoveButton.setTooltip(Tooltip.create(TARGET_REMOVE_TOOLTIP));

        buttonX = this.leftPos + TARGET_REMOVE_ALL_BUTTON_X;
        buttonY = this.topPos + TARGET_REMOVE_ALL_BUTTON_Y;
        this.targetRemoveAllButton = this.addRenderableWidget(new ImageButton(
                buttonX, buttonY,
                16, 16,
                new WidgetSprites(REMOVE_ALL_SPRITE, REMOVE_ALL_SPRITE_HIGHLIGHTED),
                button -> {
                    if (this.menu.getSyncedTargets().isEmpty()) {
                        // No targets to remove.
                        return;
                    }
                    ClientPlayNetworking.send(new ExperienceScreenActionC2SPayload(
                            this.menu.getContainerId(),
                            ExperienceScreenAction.REMOVE_ALL_TARGETS,
                            -1,
                            -1
                    ));
                    this.targetsScrolledOff = 0;
                    this.selectTargetIndex(-1);
                }
        ));
        this.targetRemoveAllButton.setTooltip(Tooltip.create(TARGET_REMOVE_ALL_TOOLTIP));
    }

    private void removeTargetButtons() {
        if (this.targetButtons[0] == null) {
            // already cleared
            return;
        }

        for (var button : this.targetButtons) {
            if (button != null) {
                this.removeWidget(button);
            }
        }
        Arrays.fill(this.targetButtons, null);

        this.removeWidget(this.targetOrderedToggleButton);
        this.removeWidget(this.targetOrderUpButton);
        this.removeWidget(this.targetOrderDownButton);
        this.removeWidget(this.targetRemoveButton);
        this.removeWidget(this.targetRemoveAllButton);
    }

    private void addPricingButtons() {
        if (this.pricingButtons[0] != null) {
            // already populated
            return;
        }

        int buttonX = this.leftPos + SCROLLBOX_ROW_X;
        int buttonY = this.topPos + SCROLLBOX_TOP_Y;

        for (int m = 0; m < SCROLLBOX_ROWS; m++) {
            this.pricingButtons[m] = this.addRenderableWidget(new ExperienceScrollBoxButton(m, buttonX, buttonY, button -> {
                if (button instanceof ExperienceScrollBoxButton selectedButton) {
                    this.selectPricingRow(selectedButton.getIndex());
                }
            }));
            buttonY += SCROLLBOX_ROW_HEIGHT;
        }

        this.updateRowFocusForSelectedPricing();
    }

    private void removePricingButtons() {
        if (this.pricingButtons[0] == null) {
            // already cleared
            return;
        }

        for (var button : this.pricingButtons) {
            if (button != null) {
                this.removeWidget(button);
            }
        }
        Arrays.fill(this.pricingButtons, null);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float a) {
        super.render(guiGraphics, mouseX, mouseY, a);

        for (TabDisplay tab : TabDisplay.values()) {
            this.checkTabHovering(guiGraphics, tab, mouseX, mouseY);
        }

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float a, int mouseX, int mouseY) {
        // Render unselected tabs.
        for (TabDisplay tab : TabDisplay.values()) {
            if (tab != this.selectedTab) {
                this.renderTabButton(guiGraphics, mouseX, mouseY, tab);
            }
        }
        // Render background.
        guiGraphics.blit(
                RenderPipelines.GUI_TEXTURED,
                this.selectedTab.getBackground(),
                this.leftPos,
                this.topPos,
                0.0F,
                0.0F,
                this.imageWidth,
                this.imageHeight,
                BG_TEXTURE_WIDTH,
                BG_TEXTURE_HEIGHT
        );

        // Render selected tab.
        this.renderTabButton(guiGraphics, mouseX, mouseY, this.selectedTab);
    }

    private void renderTabButton(GuiGraphics guiGraphics, int mouseX, int mouseY, TabDisplay tab) {
        boolean isSelectedTab = tab == this.selectedTab;
        int xTab = this.leftPos + tab.getTabX();
        int yTab = this.topPos + tab.getTabY();
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, tab.getTexture(this.selectedTab), xTab, yTab, 26, 32);

        if (!isSelectedTab && mouseX > xTab && mouseY > yTab && mouseX < xTab + TAB_WIDTH && mouseY < yTab + TAB_HEIGHT) {
            guiGraphics.requestCursor(CursorTypes.POINTING_HAND);
        }

        int xIcon = xTab + 13 - 8;
        int yIcon = yTab + 17 - 8;
        guiGraphics.renderItem(tab.getIcon(), xIcon, yIcon);
    }

    @Override
    public void renderContents(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float a) {
        super.renderContents(guiGraphics, mouseX, mouseY, a);

        if (this.selectedTab == TabDisplay.TARGETS) {
            this.renderTargetsScrollBox(guiGraphics, mouseX, mouseY);
        } else if (this.selectedTab == TabDisplay.PRICING) {
            this.renderPricingScrollBox(guiGraphics, mouseX, mouseY);
        }
    }

    private void renderScroller(GuiGraphics guiGraphics, int mouseX, int mouseY, int numRows, int rowsScrolledOff) {
        int scrollSteps = numRows - SCROLLBOX_ROWS;
        int scrollerX = this.leftPos + SCROLLER_TRACK_X;
        int trackTopY = this.topPos + SCROLLER_TRACK_TOP_Y;

        // If we can't scroll because everything fits, draw a disabled scroller at the top.
        if (scrollSteps <= 0) {
            guiGraphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    SCROLLER_DISABLED_SPRITE,
                    scrollerX,
                    trackTopY,
                    SCROLLER_WIDTH,
                    SCROLLER_HEIGHT
            );
            return;
        }

        // Compute how far the scroller can travel.
        int maxScrollerOffset = SCROLLER_TRACK_LENGTH - SCROLLER_HEIGHT;

        // Compute the actual scroller travel.
        int scrollerOffset = 0;

        // Check for the simple case.
        if (rowsScrolledOff == scrollSteps) {
            scrollerOffset = maxScrollerOffset;
        } else {
            // Distribute the track height across all scroll steps.
            float pixelsPerStep = (float) maxScrollerOffset / scrollSteps;

            // Compute the scroller's offset based on how many rows have been scrolled off.
            scrollerOffset = Math.round(pixelsPerStep * rowsScrolledOff);
        }

        // Draw the scroller in the correct position.
        int scrollerY = trackTopY + scrollerOffset;
        guiGraphics.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                SCROLLER_SPRITE,
                scrollerX,
                scrollerY,
                SCROLLER_WIDTH,
                SCROLLER_HEIGHT
        );

        // Update cursor when hovering over the scroller.
        boolean mouseOverScroller =
            mouseX >= scrollerX &&
            mouseX < (scrollerX + SCROLLER_WIDTH) &&
            mouseY >= scrollerY &&
            mouseY <= (scrollerY + SCROLLER_HEIGHT);
        if (mouseOverScroller) {
            guiGraphics.requestCursor(this.isScrolling ? CursorTypes.RESIZE_NS : CursorTypes.POINTING_HAND);
        }
    }

    protected void renderTargetsScrollBox(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (this.targetOrderUpButton != null) {
            this.targetOrderUpButton.visible = this.selectedTargetIndex >= 0;
        }
        if (this.targetOrderDownButton != null) {
            this.targetOrderDownButton.visible = this.selectedTargetIndex >= 0;
        }
        if (this.targetRemoveButton != null) {
            this.targetRemoveButton.visible = this.selectedTargetIndex >= 0;
        }

        // Render scroller in correct position or disabled.
        List<TargetView> syncedTargets = this.menu.getSyncedTargets();
        this.renderScroller(guiGraphics, mouseX, mouseY, syncedTargets.size(), this.targetsScrolledOff);

        // Render target buttons for visible targets.
        int yRow = this.topPos + SCROLLBOX_TOP_Y;
        int xItemStart = this.leftPos + 10;
        int xTextStart = this.leftPos + 30;
        int maxTextWidth = SCROLLBOX_ROW_WIDTH - 30;

        for (int row = 0; row < SCROLLBOX_ROWS; row++) {
            int targetIndex = this.targetsScrolledOff + row;
            if (targetIndex < syncedTargets.size()) {
                this.targetButtons[row].visible = true;

                TargetView target = syncedTargets.get(targetIndex);
                if (target.isWideChest()) {
                    guiGraphics.blit(
                            RenderPipelines.GUI_TEXTURED,
                            WIDE_CHEST_TEXTURE,
                            xItemStart, yRow + 1,
                            0, 0,
                            16, 16,
                            256, 256,
                            256, 256
                    );
                } else {
                    guiGraphics.renderFakeItem(target.itemStack(), xItemStart, yRow + 1);
                }

                guiGraphics.drawString(
                        this.font,
                        this.font.plainSubstrByWidth(target.displayName(), maxTextWidth),
                        xTextStart,
                        yRow + 6,
                        ARGB_SCROLL_BUTTON_LABEL,
                        true);
            } else {
                this.targetButtons[row].visible = false;
            }

            yRow += SCROLLBOX_ROW_HEIGHT;
        }
    }

    protected void renderPricingScrollBox(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Render scroller in correct position or disabled.
        this.renderScroller(guiGraphics, mouseX, mouseY, 0, this.pricingScrolledOff);
    }

    @Override
    protected void renderLabels(@NonNull GuiGraphics guiGraphics, int x, int y) {
        switch (this.selectedTab) {
            case STATUS -> {
                guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, ARGB_SCREEN_TEXT, false);
                this.renderStatusLabels(guiGraphics);
                guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, ARGB_SCREEN_TEXT, false);
            }
            case TARGETS -> {
                this.renderTargetLabels(guiGraphics);
            }
            case PRICING -> {
                this.renderPricingLabels(guiGraphics);
                guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, ARGB_SCREEN_TEXT, false);
            }
        }
    }

    protected void renderStatusLabels(@NonNull GuiGraphics guiGraphics) {
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

        int paymentsLabelWidth = this.font.width(PAYMENTS_LABEL);
        guiGraphics.drawString(
                this.font,
                PAYMENTS_LABEL,
                PAYMENTS_BOX_X + ((PAYMENTS_BOX_WIDTH - paymentsLabelWidth) / 2),
                PAYMENTS_LABEL_Y,
                ARGB_SCREEN_TEXT,
                false
        );

        guiGraphics.drawString(
                this.font,
                TARGET_KEY_LABEL,
                TARGET_KEY_LABEL_X,
                TARGET_KEY_LABEL_Y,
                ARGB_SCREEN_TEXT,
                false
        );

        guiGraphics.drawString(
                this.font,
                ENTRY_FEE_LABEL,
                ENTRY_FEE_LABEL_X,
                ENTRY_FEE_LABEL_Y,
                ARGB_SCREEN_TEXT,
                false
        );

        guiGraphics.drawString(
                this.font,
                STATUS_LABEL,
                STATUS_LABEL_X,
                STATUS_LABEL_Y,
                ARGB_SCREEN_TEXT,
                false
        );
    }

    protected void renderTargetLabels(@NonNull GuiGraphics guiGraphics) {
        int numTargets = this.menu.getSyncedTargets().size();
        Component targetsTitle = TARGETS_SCREEN_TITLE.copy().append(" (" + numTargets + ")");
        int targetsLabelWidth = this.font.width(targetsTitle);
        guiGraphics.drawString(
                this.font,
                targetsTitle,
                SCROLLBOX_ROW_X + ((SCROLLBOX_WIDTH - targetsLabelWidth) / 2),
                SCROLLBOX_LABEL_Y,
                ARGB_SCREEN_TEXT,
                false
        );

        if (this.selectedTargetIndex >= 0 && this.selectedTargetIndex < numTargets) {
            TargetView targetView = this.menu.getSyncedTargets().get(this.selectedTargetIndex);
            String targetDetails = (this.selectedTargetIndex + 1) + ") " + targetView.displayName();
            int maxDetailWidth = this.imageWidth - TARGET_DETAILS_LABEL_X - 5;
            guiGraphics.drawString(
                    this.font,
                    this.font.plainSubstrByWidth(targetDetails, maxDetailWidth),
                    TARGET_DETAILS_LABEL_X,
                    TARGET_DETAILS_LABEL_Y,
                    ARGB_SCREEN_TEXT,
                    false
            );

            guiGraphics.drawString(
                    this.font,
                    "@ " + targetView.pos().toShortString(),
                    TARGET_DETAILS_LABEL_X + 20,
                    TARGET_DETAILS_LABEL_Y + 11,
                    ARGB_SCREEN_TEXT,
                    false
            );
        }

        guiGraphics.drawString(
                this.font,
                TARGET_ORDERED_LABEL,
                TARGET_ORDERED_LABEL_X,
                TARGET_ORDERED_LABEL_Y,
                ARGB_SCREEN_TEXT,
                false
        );
    }

    protected void renderPricingLabels(@NonNull GuiGraphics guiGraphics) {
        int numPricings = 0; // this.menu.getSyncedPricings().size();
        Component pricingsTitle = PRICING_SCREEN_TITLE.copy().append(" (" + numPricings + ")");
        int pricingLabelWidth = this.font.width(pricingsTitle);
        guiGraphics.drawString(
                this.font,
                pricingsTitle,
                SCROLLBOX_ROW_X + ((SCROLLBOX_WIDTH - pricingLabelWidth) / 2),
                SCROLLBOX_LABEL_Y,
                ARGB_SCREEN_TEXT,
                false
        );
    }

    private boolean canScroll() {
        return switch (this.selectedTab) {
            case STATUS -> false;
            case TARGETS -> this.menu.getSyncedTargets().size() > SCROLLBOX_ROWS;
            case PRICING -> false;
        };
    }

    private boolean checkTabClicked(TabDisplay tab, double x, double y) {
        int tabX = tab.getTabX();
        int tabY = tab.getTabY();
        return x >= tabX && x <= tabX + TAB_WIDTH && y >= tabY && y <= tabY + TAB_HEIGHT;
    }

    private boolean checkTabHovering(GuiGraphics guiGraphics, TabDisplay tab, int mouseX, int mouseY) {
        int tabX = tab.getTabX();
        int tabY = tab.getTabY();
        if (this.isHovering(tabX + 3, tabY + 3, TAB_WIDTH - 5, TAB_HEIGHT - 5, mouseX, mouseY)) {
            guiGraphics.setTooltipForNextFrame(this.font, tab.getTitle(), mouseX, mouseY);
            return true;
        } else {
            return false;
        }
    }

    private boolean isInsideScrollbar(double x, double y) {
        return x >= SCROLLER_TRACK_X &&
                x < SCROLLER_TRACK_X + SCROLLER_WIDTH &&
                y >= SCROLLER_TRACK_TOP_Y &&
                y <= SCROLLER_TRACK_BOTTOM_Y;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubleClick) {
        if (mouseButtonEvent.button() == 0) {
            double relativeX = mouseButtonEvent.x() - this.leftPos;
            double relativeY = mouseButtonEvent.y() - this.topPos;

            for (TabDisplay tab : TabDisplay.values()) {
                if (this.checkTabClicked(tab, relativeX, relativeY)) {
                    return true;
                }
            }

            if (this.selectedTab.canScroll() && this.isInsideScrollbar(relativeX, relativeY)) {
                this.isScrolling = this.canScroll();
                return true;
            }
        }

        return super.mouseClicked(mouseButtonEvent, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent mouseButtonEvent) {
        if (mouseButtonEvent.button() == 0) {
            this.isScrolling = false;

            double relativeX = mouseButtonEvent.x() - this.leftPos;
            double relativeY = mouseButtonEvent.y() - this.topPos;

            for (TabDisplay tab : TabDisplay.values()) {
                if (this.checkTabClicked(tab, relativeX, relativeY)) {
                    this.selectTab(tab);
                    return true;
                }
            }
        }

        return super.mouseReleased(mouseButtonEvent);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent mouseButtonEvent, double dx, double dy) {
        if (!this.isScrolling) {
            return super.mouseDragged(mouseButtonEvent, dx, dy);
        }

        int totalRows = this.selectedTab == TabDisplay.TARGETS ? this.menu.getSyncedTargets().size() : 0;
        int maxScrolledOff = totalRows - SCROLLBOX_ROWS;
        if (maxScrolledOff <= 0) {
            if (this.selectedTab == TabDisplay.TARGETS) {
                this.targetsScrolledOff = 0;
                this.updateRowFocusForSelectedTarget();
            } else if (this.selectedTab == TabDisplay.PRICING) {
                this.pricingScrolledOff = 0;
                this.updateRowFocusForSelectedPricing();
            }
            return true;
        }

        int yScrollBarTop = this.topPos + SCROLLER_TRACK_TOP_Y;
        float scrollableTrackLength = SCROLLER_TRACK_LENGTH - SCROLLER_HEIGHT;
        float scrollerCenterY = (float)mouseButtonEvent.y() - yScrollBarTop - ((float) SCROLLER_HEIGHT / 2.0F);

        // Convert the mouse's Y position on the scrollbar into a number from 0 to maxScrolledOff.
        float scrollFraction = scrollerCenterY / scrollableTrackLength;
        int rowOffset = (int)(scrollFraction * maxScrolledOff + 0.5F); // rounded to nearest integer
        int scrolledOff = Mth.clamp(rowOffset, 0, maxScrolledOff);

        if (this.selectedTab == TabDisplay.TARGETS) {
            this.targetsScrolledOff = scrolledOff;
            this.updateRowFocusForSelectedTarget();
        } else if (this.selectedTab == TabDisplay.PRICING) {
            this.pricingScrolledOff = scrolledOff;
            this.updateRowFocusForSelectedPricing();
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (super.mouseScrolled(x, y, scrollX, scrollY)) {
            return true;
        }

        if (this.canScroll()) {
            if (this.selectedTab == TabDisplay.TARGETS) {
                int totalRows = this.menu.getSyncedTargets().size();
                int maxScrolledOff = totalRows - SCROLLBOX_ROWS;
                this.targetsScrolledOff = Mth.clamp((int)(this.targetsScrolledOff - scrollY), 0, maxScrolledOff);
                this.updateRowFocusForSelectedTarget();
            } else if (this.selectedTab == TabDisplay.PRICING) {
                int totalRows = 0;
                int maxScrolledOff = totalRows - SCROLLBOX_ROWS;
                this.pricingScrolledOff = Mth.clamp((int)(this.pricingScrolledOff - scrollY), 0, maxScrolledOff);
                this.updateRowFocusForSelectedPricing();
            }
            return true;
        }

        return false;
    }

    private void selectTab(TabDisplay tab) {
        this.selectedTab = tab;
        this.menu.setSelectedTab(tab.getMenuTab());

        this.removeAllButtons();
        switch (tab) {
            case STATUS -> this.addStatusToggleButton();
            case TARGETS -> this.addTargetButtons();
            case PRICING -> this.addPricingButtons();
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(
                    this.menu.getContainerId(),
                    ShoppingExperienceMenu.buttonForTab(tab.getMenuTab())
            );
        }
    }

    private void selectPricingRow(int selectedRow) {
        this.selectedPricingIndex = selectedRow + this.pricingScrolledOff;
        this.updateRowFocusForSelectedPricing();
    }

    private void updateRowFocusForSelectedPricing() {
        for (var button : this.pricingButtons) {
            button.setFocused(false);
        }

        int numPricings = 0; // this.menu.getSyncedPricings().size();

        if (this.selectedPricingIndex < 0 || this.selectedPricingIndex >= numPricings) {
            this.selectedPricingIndex = -1;
            return;
        }

        int visibleRows = Math.min(numPricings, SCROLLBOX_ROWS);
        int firstVisible = this.pricingScrolledOff;
        int lastVisible = this.pricingScrolledOff + visibleRows - 1;

        if (this.selectedPricingIndex < firstVisible || this.selectedPricingIndex > lastVisible) {
            return;
        }

        int visibleRow = this.selectedPricingIndex - firstVisible;
        this.pricingButtons[visibleRow].setFocused(true);
    }

    private void selectTargetIndex(int selectedTargetIndex) {
        this.selectedTargetIndex = selectedTargetIndex;
        this.updateRowFocusForSelectedTarget();
    }

    private void selectTargetRow(int selectedRow) {
        this.selectedTargetIndex = selectedRow + this.targetsScrolledOff;
        this.updateRowFocusForSelectedTarget();
    }

    private void updateRowFocusForSelectedTarget() {
        for (var button : this.targetButtons) {
            button.setFocused(false);
        }

        int numTargets = this.menu.getSyncedTargets().size();

        if (this.selectedTargetIndex < 0 || this.selectedTargetIndex >= numTargets) {
            this.selectedTargetIndex = -1;
            return;
        }

        int visibleRows = Math.min(numTargets, SCROLLBOX_ROWS);
        int firstVisible = this.targetsScrolledOff;
        int lastVisible = this.targetsScrolledOff + visibleRows - 1;

        if (this.selectedTargetIndex < firstVisible || this.selectedTargetIndex > lastVisible) {
            return;
        }

        int visibleRow = this.selectedTargetIndex - firstVisible;
        this.targetButtons[visibleRow].setFocused(true);
    }
}
