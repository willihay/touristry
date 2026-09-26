package org.bensam.touristry.client.screen.tabs;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.bensam.touristry.Touristry;
import org.bensam.touristry.client.screen.AbstractTabbedExperienceScreen;
import org.bensam.touristry.client.screen.buttons.ExperienceScrollBoxButton;
import org.bensam.touristry.client.screen.buttons.NoFocusImageButton;
import org.bensam.touristry.client.screen.buttons.TargetOrderedButton;
import org.bensam.touristry.client.screen.components.AbstractScrollBox;
import org.bensam.touristry.menu.AbstractExperienceMenu;
import org.bensam.touristry.network.ExperienceScreenActionC2SPayload;
import org.bensam.touristry.tourism.experience.ExperienceScreenAction;
import org.bensam.touristry.tourism.experience.TargetView;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;

public class TargetsTab<T extends Enum<T>> implements ScreenTab<T> {
    private static final Component TAB_TITLE = Component.translatable("screen.touristry.tourist_block.tab.targets");

    //region Constants: Textures and sprites
    private static final Identifier BACKGROUND_TEXTURE = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "textures/gui/tourist_experience_targets.png");
    private static final Identifier WIDE_CHEST_TEXTURE = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "textures/gui/wide_chest.png");
    private static final Identifier MOVE_UP_SPRITE = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "move_up");
    private static final Identifier MOVE_UP_HIGHLIGHTED_SPRITE = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "move_up_highlighted");
    private static final Identifier MOVE_DOWN_SPRITE = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "move_down");
    private static final Identifier MOVE_DOWN_HIGHLIGHTED_SPRITE = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "move_down_highlighted");
    private static final Identifier SWEEP_SPRITE = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "sweep");
    private static final Identifier SWEEP_HIGHLIGHTED_SPRITE = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "sweep_highlighted");
    private static final Identifier TRASH_SPRITE = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "trash");
    private static final Identifier TRASH_HIGHLIGHTED_SPRITE = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "trash_highlighted");
    //endregion

    //region Constants: Labels and positions
    private static final Component TARGET_ORDERED_LABEL = Component.translatable("screen.touristry.tourist_block.targets.ordered_button.label");
    private static final Component TARGET_MOVE_UP_TOOLTIP = Component.translatable("screen.touristry.tourist_block.target.move_up.tooltip");
    private static final Component TARGET_MOVE_DOWN_TOOLTIP = Component.translatable("screen.touristry.tourist_block.target.move_down.tooltip");
    private static final Component TARGET_REMOVE_TOOLTIP = Component.translatable("screen.touristry.tourist_block.target.remove.tooltip");
    private static final Component TARGET_REMOVE_ALL_TOOLTIP = Component.translatable("screen.touristry.tourist_block.targets.remove_all.tooltip");
    private static final int ARGB_SCROLLBOX_BUTTON_TEXT_COLOR = 0xFFFFFFFF; // white
    private static final int SCROLLBOX_ROWS = 7;
    private static final int SCROLLBOX_WIDTH = 96;
    private static final int SCROLLBOX_LABEL_Y = 6;
    private static final int SCROLLBOX_ROW_X = 5;
    private static final int SCROLLBOX_TOP_Y = 18;
    private static final int SCROLLBOX_ROW_WIDTH = 88;
    private static final int SCROLLBOX_ROW_HEIGHT = 20;
    private static final int SCROLLER_TRACK_X = 94;
    private static final int SCROLLER_TRACK_TOP_Y = SCROLLBOX_TOP_Y;
    private static final int SCROLLER_TRACK_BOTTOM_Y = 157;
    private static final int SCROLLER_TRACK_LENGTH = SCROLLER_TRACK_BOTTOM_Y - SCROLLBOX_TOP_Y + 1;
    private static final int TARGET_ORDERED_LABEL_X = 127;
    private static final int TARGET_ORDERED_LABEL_Y = SCROLLBOX_TOP_Y + 5;
    private static final int TARGET_ORDERED_BUTTON_X = 193;
    private static final int TARGET_ORDERED_BUTTON_Y = SCROLLBOX_TOP_Y;
    private static final int TARGET_CHANGE_ORDER_BUTTON_X = 107;
    private static final int TARGET_CHANGE_ORDER_BUTTON_Y = 44;
    private static final int TARGET_DETAILS_LABEL_X = 127;
    private static final int TARGET_DETAILS_LABEL_Y = 50;
    private static final int TARGET_REMOVE_BUTTON_X = -22;
    private static final int TARGET_REMOVE_BUTTON_Y = TARGET_DETAILS_LABEL_Y + 9;
    private static final int TARGET_REMOVE_ALL_BUTTON_X = -22;
    private static final int TARGET_REMOVE_ALL_BUTTON_Y = -22;
    //endregion

    private final T tabEnum;
    private final ItemStack tabIcon;
    private int tabOrder;
    private final ScrollBox scrollBox = new ScrollBox();

    private TargetOrderedButton targetOrderedToggleButton;
    private ImageButton targetOrderUpButton;
    private ImageButton targetOrderDownButton;
    private ImageButton targetRemoveButton;
    private ImageButton targetRemoveAllButton;

    public TargetsTab(T tabEnum, Item tabIcon) {
        this.tabEnum = tabEnum;
        this.tabIcon = new ItemStack(tabIcon);
    }

    // Screen setup handlers
    @Override
    public void init(AbstractExperienceMenu<?> menu) {
        // Request sync-to-client of all experience targets.
        ClientPlayNetworking.send(new ExperienceScreenActionC2SPayload(
                menu.getContainerId(),
                ExperienceScreenAction.REQUEST_TARGETS,
                -1,
                -1
        ));
    }

    @Override
    public void onSelected(AbstractTabbedExperienceScreen<?, ?> screen, AbstractExperienceMenu<?> menu) {
        if (this.scrollBox.getTargetButton(0) != null) {
            // already populated
            return;
        }

        // Add target row buttons for scroll box.
        int buttonX = screen.getScreenLeft() + SCROLLBOX_ROW_X;
        int buttonY = screen.getScreenTop() + SCROLLBOX_TOP_Y;

        for (int m = 0; m < SCROLLBOX_ROWS; m++) {
            this.scrollBox.setTargetButton(m, screen.addButton(new ExperienceScrollBoxButton(m, buttonX, buttonY, button -> {
                if (button instanceof ExperienceScrollBoxButton selectedButton) {
                    this.scrollBox.selectScrollBoxRow(selectedButton.getIndex(), menu.getSyncedTargets().size());
                }
            })));
            buttonY += SCROLLBOX_ROW_HEIGHT;
        }

        // Set the focus if there already is one.
        this.scrollBox.updateRowFocusForSelectedContent(menu.getSyncedTargets().size());

        // Add target order toggle button.
        buttonX = screen.getScreenLeft() + TARGET_ORDERED_BUTTON_X;
        buttonY = screen.getScreenTop() + TARGET_ORDERED_BUTTON_Y;
        this.targetOrderedToggleButton = screen.addButton(new TargetOrderedButton(
                menu.getSyncedOrderedTargets(),
                buttonX,
                buttonY,
                button -> {
                    if (button instanceof TargetOrderedButton orderedButton) {
                        boolean isOrdered = !menu.getSyncedOrderedTargets();
                        orderedButton.setOrdered(isOrdered);
                        this.scrollBox.updateRowFocusForSelectedContent(menu.getSyncedTargets().size());

                        ClientPlayNetworking.send(new ExperienceScreenActionC2SPayload(
                                menu.getContainerId(),
                                ExperienceScreenAction.SET_ORDERED_TARGETS,
                                isOrdered ? 1 : 0,
                                -1
                        ));
                    }
                }
        ));

        // Add target move up button.
        buttonX = screen.getScreenLeft() + TARGET_CHANGE_ORDER_BUTTON_X;
        buttonY = screen.getScreenTop() + TARGET_CHANGE_ORDER_BUTTON_Y;
        int u = 16;
        int v = 0;
        this.targetOrderUpButton = screen.addButton(new NoFocusImageButton(
                buttonX, buttonY,
                10, 10,
                u, v,
                16, 16,
                new WidgetSprites(MOVE_UP_SPRITE, MOVE_UP_HIGHLIGHTED_SPRITE),
                button -> {
                    int selectedTarget = this.scrollBox.getSelectedTargetIndex();
                    if (selectedTarget <= 0 || selectedTarget >= menu.getSyncedTargets().size()) {
                        // Selected target index is invalid or is already at the top of the list.
                        return;
                    }
                    ClientPlayNetworking.send(new ExperienceScreenActionC2SPayload(
                            menu.getContainerId(),
                            ExperienceScreenAction.MOVE_TARGET,
                            selectedTarget,
                            selectedTarget - 1
                    ));
                    this.scrollBox.selectContentRow(selectedTarget - 1, menu.getSyncedTargets().size());
                },
                TARGET_MOVE_UP_TOOLTIP
        ));
        this.targetOrderUpButton.visible = false;

        // Add target move down button.
        buttonY += 10;
        v = 16;
        this.targetOrderDownButton = screen.addButton(new NoFocusImageButton(
                buttonX, buttonY,
                10, 10,
                u, v,
                16, 16,
                new WidgetSprites(MOVE_DOWN_SPRITE, MOVE_DOWN_HIGHLIGHTED_SPRITE),
                button -> {
                    int selectedTarget = this.scrollBox.getSelectedTargetIndex();
                    if (selectedTarget < 0 || selectedTarget >= (menu.getSyncedTargets().size() - 1)) {
                        // Selected target index is invalid or is already at the bottom of the list.
                        return;
                    }
                    ClientPlayNetworking.send(new ExperienceScreenActionC2SPayload(
                            menu.getContainerId(),
                            ExperienceScreenAction.MOVE_TARGET,
                            selectedTarget,
                            selectedTarget + 1
                    ));
                    this.scrollBox.selectContentRow(selectedTarget + 1, menu.getSyncedTargets().size());
                },
                TARGET_MOVE_DOWN_TOOLTIP
        ));
        this.targetOrderDownButton.visible = false;

        buttonX = screen.getScreenLeft() + screen.getScreenWidth() + TARGET_REMOVE_BUTTON_X;
        buttonY = screen.getScreenTop() + TARGET_REMOVE_BUTTON_Y;
        this.targetRemoveButton = screen.addButton(new NoFocusImageButton(
                buttonX, buttonY,
                10, 10,
                new WidgetSprites(TRASH_SPRITE, TRASH_HIGHLIGHTED_SPRITE),
                button -> {
                    if (!this.scrollBox.isAnyContentRowSelected()) {
                        return;
                    }
                    ClientPlayNetworking.send(new ExperienceScreenActionC2SPayload(
                            menu.getContainerId(),
                            ExperienceScreenAction.REMOVE_TARGET,
                            this.scrollBox.getSelectedTargetIndex(),
                            -1
                    ));
                    // Adjust scroll position as needed.
                    int numTargets = menu.getSyncedTargets().size() - 1;
                    this.scrollBox.clampContentRowsScrolledOff(numTargets);
                    this.scrollBox.deselectContentRows(menu.getSyncedTargets().size());
                },
                TARGET_REMOVE_TOOLTIP
        ));
        this.targetRemoveButton.visible = false;

        buttonX = screen.getScreenLeft() + screen.getScreenWidth() + TARGET_REMOVE_ALL_BUTTON_X;
        buttonY = screen.getScreenTop() + screen.getScreenHeight() + TARGET_REMOVE_ALL_BUTTON_Y;
        this.targetRemoveAllButton = screen.addButton(new NoFocusImageButton(
                buttonX, buttonY,
                16, 16,
                new WidgetSprites(SWEEP_SPRITE, SWEEP_HIGHLIGHTED_SPRITE),
                button -> {
                    if (menu.getSyncedTargets().isEmpty()) {
                        // No targets to remove.
                        return;
                    }
                    ClientPlayNetworking.send(new ExperienceScreenActionC2SPayload(
                            menu.getContainerId(),
                            ExperienceScreenAction.REMOVE_ALL_TARGETS,
                            -1,
                            -1
                    ));
                    this.scrollBox.clampContentRowsScrolledOff(0);
                    this.scrollBox.deselectContentRows(menu.getSyncedTargets().size());
                },
                TARGET_REMOVE_ALL_TOOLTIP
        ));
    }

    @Override
    public void onDeselected(AbstractTabbedExperienceScreen<?, ?> screen) {
        if (this.scrollBox.getTargetButton(0) == null) {
            // already cleared
            return;
        }

        for (var button : this.scrollBox.getTargetButtons()) {
            if (button != null) {
                screen.removeButton(button);
            }
        }
        Arrays.fill(this.scrollBox.getTargetButtons(), null);

        screen.removeButton(this.targetOrderedToggleButton);
        screen.removeButton(this.targetOrderUpButton);
        screen.removeButton(this.targetOrderDownButton);
        screen.removeButton(this.targetRemoveButton);
        screen.removeButton(this.targetRemoveAllButton);
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

    // Input methods
    @Override
    public boolean onMouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubleClick, double mouseLocalX, double mouseLocalY, AbstractExperienceMenu<?> menu) {
        if (mouseButtonEvent.button() == 0) {
            return this.scrollBox.checkIfScrolling(mouseLocalX, mouseLocalY, menu.getSyncedTargets().size());
        }
        return false;
    }

    @Override
    public boolean onMouseReleased(MouseButtonEvent mouseButtonEvent, double mouseLocalX, double mouseLocalY, AbstractExperienceMenu<?> menu) {
        if (mouseButtonEvent.button() == 0) {
            this.scrollBox.setIsScrolling(false);
        }
        return false;
    }

    @Override
    public boolean onMouseDragged(MouseButtonEvent mouseButtonEvent, double dx, double dy, double mouseLocalX, double mouseLocalY, AbstractExperienceMenu<?> menu) {
        if (!this.scrollBox.isScrolling()) {
            return false;
        }

        this.scrollBox.onDragScroll(mouseLocalY, menu.getSyncedTargets().size());
        return true;
    }

    @Override
    public boolean onMouseScrolled(double x, double y, double scrollX, double scrollY, AbstractExperienceMenu<?> menu) {
        if (this.scrollBox.canScroll(menu.getSyncedTargets().size())) {
            return this.scrollBox.onScroll(scrollY, menu.getSyncedTargets().size());
        }
        return false;
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
    public void renderContents(GuiGraphics guiGraphics, int mouseX, int mouseY, AbstractTabbedExperienceScreen<?, ?> screen, AbstractExperienceMenu<?> menu) {
        boolean isAnyTargetSelected = this.scrollBox.isAnyContentRowSelected();
        if (this.targetOrderUpButton != null) {
            this.targetOrderUpButton.visible = isAnyTargetSelected;
        }
        if (this.targetOrderDownButton != null) {
            this.targetOrderDownButton.visible = isAnyTargetSelected;
        }
        if (this.targetRemoveButton != null) {
            this.targetRemoveButton.visible = isAnyTargetSelected;
        }

        this.scrollBox.renderContents(guiGraphics, mouseX, mouseY, screen, menu, menu.getSyncedTargets());
    }

    @Override
    public void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY, AbstractTabbedExperienceScreen<?, ?> screen, AbstractExperienceMenu<?> menu) {
        // Render targets tab title, including number of targets.
        int numTargets = menu.getSyncedTargets().size();
        Component targetsTitle = TAB_TITLE.copy().append(" (" + numTargets + ")");
        int targetsLabelWidth = screen.getFont().width(targetsTitle);
        guiGraphics.drawString(
                screen.getFont(),
                targetsTitle,
                SCROLLBOX_ROW_X + ((SCROLLBOX_WIDTH - targetsLabelWidth) / 2),
                SCROLLBOX_LABEL_Y,
                AbstractTabbedExperienceScreen.ARGB_SCREEN_TEXT_COLOR,
                false
        );

        if (this.scrollBox.isValidContentRowSelected(numTargets)) {
            TargetView targetView = menu.getSyncedTargets().get(this.scrollBox.getSelectedTargetIndex());
            String targetDetails = (this.scrollBox.getSelectedTargetIndex() + 1) + ") " + targetView.displayName();
            int maxDetailWidth = screen.getScreenWidth() - TARGET_DETAILS_LABEL_X - 5;
            guiGraphics.drawString(
                    screen.getFont(),
                    screen.getFont().plainSubstrByWidth(targetDetails, maxDetailWidth),
                    TARGET_DETAILS_LABEL_X,
                    TARGET_DETAILS_LABEL_Y,
                    AbstractTabbedExperienceScreen.ARGB_SCREEN_TEXT_COLOR,
                    false
            );

            guiGraphics.drawString(
                    screen.getFont(),
                    "@ " + targetView.pos().toShortString(),
                    TARGET_DETAILS_LABEL_X + 20,
                    TARGET_DETAILS_LABEL_Y + 11,
                    AbstractTabbedExperienceScreen.ARGB_SCREEN_TEXT_COLOR,
                    false
            );
        }

        guiGraphics.drawString(
                screen.getFont(),
                TARGET_ORDERED_LABEL,
                TARGET_ORDERED_LABEL_X,
                TARGET_ORDERED_LABEL_Y,
                AbstractTabbedExperienceScreen.ARGB_SCREEN_TEXT_COLOR,
                false
        );
    }

    public class ScrollBox extends AbstractScrollBox<TargetView> {
        public ScrollBox() {
            super(new ExperienceScrollBoxButton[SCROLLBOX_ROWS], SCROLLER_TRACK_X, SCROLLER_TRACK_TOP_Y, SCROLLER_TRACK_LENGTH);
        }

        public @Nullable ExperienceScrollBoxButton getTargetButton(int index) {
            if (index < SCROLLBOX_ROWS) {
                return (ExperienceScrollBoxButton) this.rowWidgets[index];
            }
            return null;
        }

        public int getSelectedTargetIndex() {
            return this.selectedContentRow;
        }

        public ExperienceScrollBoxButton[] getTargetButtons() {
            return (ExperienceScrollBoxButton[]) this.rowWidgets;
        }

        public void setTargetButton(int index, ExperienceScrollBoxButton button) {
            if (index < SCROLLBOX_ROWS) {
                this.rowWidgets[index] = button;
            }
        }

        @Override
        protected void renderRow(GuiGraphics guiGraphics, int mouseX, int mouseY, int row, AbstractTabbedExperienceScreen<?, ?> screen, AbstractExperienceMenu<?> menu, @NonNull TargetView target) {
            int yItem = screen.getScreenTop() + SCROLLBOX_TOP_Y + 1 + (row * SCROLLBOX_ROW_HEIGHT);
            int xItemStart = screen.getScreenLeft() + 10;
            int xTextStart = screen.getScreenLeft() + 30;
            int maxTextWidth = SCROLLBOX_ROW_WIDTH - 30;

            if (target.isWideChest()) {
                guiGraphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        WIDE_CHEST_TEXTURE,
                        xItemStart, yItem,
                        0, 0,
                        16, 16,
                        256, 256,
                        256, 256
                );
            } else {
                guiGraphics.renderFakeItem(target.itemStack(), xItemStart, yItem);
            }

            guiGraphics.drawString(
                    screen.getFont(),
                    screen.getFont().plainSubstrByWidth(target.displayName(), maxTextWidth),
                    xTextStart,
                    yItem + 5,
                    ARGB_SCROLLBOX_BUTTON_TEXT_COLOR,
                    true);
        }
    }
}
