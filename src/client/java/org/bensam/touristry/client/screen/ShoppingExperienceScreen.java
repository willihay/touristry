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
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.bensam.touristry.ModBlocks;
import org.bensam.touristry.ModItems;
import org.bensam.touristry.Touristry;
import org.bensam.touristry.menu.ShoppingExperienceMenu;
import org.bensam.touristry.network.ExperienceScreenActionC2SPayload;
import org.bensam.touristry.tourism.experience.ExperienceScreenAction;
import org.bensam.touristry.tourism.experience.ItemPrice;
import org.bensam.touristry.tourism.experience.TargetView;
import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.List;

public class ShoppingExperienceScreen extends AbstractContainerScreen<ShoppingExperienceMenu> {
    //region Constants: Sprites & Textures
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
    private static final Identifier OUT_OF_STOCK_TEXTURE = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "textures/gui/out_of_stock.png");
    private static final Identifier WIDE_CHEST_TEXTURE = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "textures/gui/wide_chest.png");
    private static final Identifier FREE_COST_SPRITE = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "free_cost");
    private static final Identifier TRADE_ARROW_SPRITE = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "trade_arrow");

    // Button sprites
    private static final Identifier ACCEPT_SPRITE = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "accept");
    private static final Identifier ACCEPT_HIGHLIGHTED_SPRITE = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "accept_highlighted");
    private static final Identifier CANCEL_SPRITE = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "cancel");
    private static final Identifier CANCEL_HIGHLIGHTED_SPRITE = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "cancel_highlighted");
    private static final Identifier IMPORT_FROM_CONTAINER_SPRITE = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "import_from_container");
    private static final Identifier IMPORT_FROM_CONTAINER_HIGHLIGHTED_SPRITE = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "import_from_container_highlighted");
    private static final Identifier MOVE_UP_SPRITE = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "move_up");
    private static final Identifier MOVE_UP_HIGHLIGHTED_SPRITE = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "move_up_highlighted");
    private static final Identifier MOVE_DOWN_SPRITE = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "move_down");
    private static final Identifier MOVE_DOWN_HIGHLIGHTED_SPRITE = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "move_down_highlighted");
    private static final Identifier REMOVE_ALL_SPRITE = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "take_out");
    private static final Identifier REMOVE_ALL_HIGHLIGHTED_SPRITE = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "take_out_highlighted");
    private static final Identifier RESET_DEFAULT_SPRITE = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "reset");
    private static final Identifier RESET_DEFAULT_HIGHLIGHTED_SPRITE = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "reset_highlighted");
    private static final Identifier SCROLLER_SPRITE = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "scroller");
    private static final Identifier SCROLLER_DISABLED_SPRITE = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "scroller_disabled");
    private static final Identifier SWEEP_SPRITE = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "sweep");
    private static final Identifier SWEEP_HIGHLIGHTED_SPRITE = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "sweep_highlighted");
    private static final Identifier TRASH_SPRITE = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "trash");
    private static final Identifier TRASH_HIGHLIGHTED_SPRITE = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "trash_highlighted");
    //endregion

    //region Constants: Common
    // Common constants
    private static final int ARGB_SCREEN_TEXT_COLOR = 0xFF404040;
    private static final int ARGB_SCROLLBOX_BUTTON_TEXT_COLOR = 0xFFFFFFFF;
    private static final int ARGB_DIRTY_MARKER_COLOR = 0xFFFF0000;
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
    //endregion

    //region Constants: Status Tab
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
    //endregion

    //region Constants: Targets Tab
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
    //endregion

    //region Constants: Pricing Tab
    // Pricing screen constants
    private static final Component PRICING_SCREEN_TITLE = Component.translatable("screen.touristry.tourist_block.tab.pricing");
    private static final Component PRICING_IMPORT_TOOLTIP = Component.translatable("screen." + Touristry.MOD_ID + ".tourist_block.pricing.import.tooltip");
    private static final Component PRICING_DEFAULT_LABEL = Component.translatable("screen." + Touristry.MOD_ID + ".tourist_block.pricing.default.label");
    private static final Component PRICING_DEFAULT_LABEL_TOOLTIP = Component.translatable("screen." + Touristry.MOD_ID + ".tourist_block.pricing.default.label.tooltip");
    private static final Component PRICING_RESET_DEFAULT_TOOLTIP = Component.translatable("screen." + Touristry.MOD_ID + ".tourist_block.pricing.reset_default_cost.tooltip");
    private static final Component PRICING_REMOVE_TOOLTIP = Component.translatable("screen." + Touristry.MOD_ID + ".tourist_block.pricing.remove.tooltip");
    private static final Component PRICING_ACCEPT_TOOLTIP = Component.translatable("screen." + Touristry.MOD_ID + ".tourist_block.pricing.accept.tooltip");
    private static final Component PRICING_CANCEL_TOOLTIP = Component.translatable("screen." + Touristry.MOD_ID + ".tourist_block.pricing.cancel.tooltip");
    private static final Component PRICING_REMOVE_ALL_TOOLTIP = Component.translatable("screen." + Touristry.MOD_ID + ".tourist_block.pricings.remove_all.tooltip");
    private static final int PRICING_IMPORT_BUTTON_X = 107;
    private static final int PRICING_IMPORT_BUTTON_Y = SCROLLBOX_TOP_Y;
    private static final int PRICING_DEFAULT_LABEL_RIGHT_X = 211;
    private static final int PRICING_DEFAULT_LABEL_Y = 22;
    private static final int PRICING_RESET_DEFAULT_BUTTON_X = 238;
    private static final int PRICING_RESET_DEFAULT_BUTTON_Y = 26;
    private static final int PRICING_ACCEPT_BUTTON_X = 242;
    private static final int PRICING_ACCEPT_BUTTON_Y = 50;
    private static final int PRICING_CANCEL_BUTTON_X = 242;
    private static final int PRICING_CANCEL_BUTTON_Y = 59;
    private static final int PRICING_REMOVE_BUTTON_X = 254;
    private static final int PRICING_REMOVE_BUTTON_Y = 53;
    private static final int PRICING_REMOVE_ALL_BUTTON_X = 107;
    private static final int PRICING_REMOVE_ALL_BUTTON_Y = 51;
    //endregion

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
    private int defaultItemPriceLabelWidth;
    private ItemStack focusItemForSale = ItemStack.EMPTY;
    private int selectedItemPriceIndex;
    private int pricesScrolledOff;
    private int lastItemPricesRevision;
    private final ExperienceScrollBoxButton[] itemPriceButtons = new ExperienceScrollBoxButton[SCROLLBOX_ROWS];
    private ImageButton itemImportButton;
    private ImageButton itemPriceRemoveButton;
    private ImageButton itemPriceResetDefaultButton;
    private ImageButton itemPriceAcceptButton;
    private ImageButton itemPriceCancelButton;
    private ImageButton itemPriceRemoveAllButton;

    public ShoppingExperienceScreen(ShoppingExperienceMenu container, Inventory inventory, Component title) {
        super(container, inventory, title);
        this.imageWidth = BG_SCREEN_WIDTH;
        this.inventoryLabelX = 107;
        this.selectedItemPriceIndex = -1;
        this.selectedTargetIndex = -1;
    }

    @Override
    protected void init() {
        super.init();
        this.selectTab(this.selectedTab);
        this.defaultItemPriceLabelWidth = this.font.width(PRICING_DEFAULT_LABEL);

        // Request sync-to-client of all experience targets and item prices.
        ClientPlayNetworking.send(new ExperienceScreenActionC2SPayload(
                this.menu.getContainerId(),
                ExperienceScreenAction.REQUEST_TARGETS,
                -1,
                -1
        ));
        ClientPlayNetworking.send(new ExperienceScreenActionC2SPayload(
                this.menu.getContainerId(),
                ExperienceScreenAction.REQUEST_ITEM_PRICES,
                -1,
                -1
        ));
    }

    @Override
    protected void containerTick() {
        super.containerTick();

        if (this.menu.getSyncedItemPricesRevision() != this.lastItemPricesRevision) {
            this.lastItemPricesRevision = this.menu.getSyncedItemPricesRevision();

            ItemStack itemForSale = this.menu.getSlot(ShoppingExperienceMenu.SHOPPING_ITEM_FOR_SALE_SLOT).getItem();
            boolean changedItem = !ItemStack.isSameItemSameComponents(itemForSale, this.focusItemForSale);
            if (changedItem) {
                if (itemForSale.isEmpty()) {
                    this.focusItemForSale = ItemStack.EMPTY;
                    this.selectPricingIndex(-1);
                } else {
                    this.focusItemForSale = itemForSale.copy();
                    this.focusOnItemForSale(this.focusItemForSale);
                }
            }
        }
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
                    if (this.selectedTargetIndex < 0 || this.selectedTargetIndex >= (this.menu.getSyncedTargets().size() - 1)) {
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
                new WidgetSprites(TRASH_SPRITE, TRASH_HIGHLIGHTED_SPRITE),
                button -> {
                    if (!this.isTargetSelected()) {
                        return;
                    }
                    ClientPlayNetworking.send(new ExperienceScreenActionC2SPayload(
                            this.menu.getContainerId(),
                            ExperienceScreenAction.REMOVE_TARGET,
                            this.selectedTargetIndex,
                            -1
                    ));
                    // Adjust scroll position as needed.
                    int numTargets = this.menu.getSyncedTargets().size() - 1;
                    if ((numTargets - this.targetsScrolledOff) < SCROLLBOX_ROWS) {
                        this.targetsScrolledOff = Math.max(0, this.targetsScrolledOff - 1);
                    }
                    this.selectTargetIndex(-1);
                }
        ));
        this.targetRemoveButton.setTooltip(Tooltip.create(TARGET_REMOVE_TOOLTIP));

        buttonX = this.leftPos + TARGET_REMOVE_ALL_BUTTON_X;
        buttonY = this.topPos + TARGET_REMOVE_ALL_BUTTON_Y;
        this.targetRemoveAllButton = this.addRenderableWidget(new NoFocusImageButton(
                buttonX, buttonY,
                16, 16,
                new WidgetSprites(SWEEP_SPRITE, SWEEP_HIGHLIGHTED_SPRITE),
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
        if (this.itemPriceButtons[0] != null) {
            // already populated
            return;
        }

        int buttonX = this.leftPos + SCROLLBOX_ROW_X;
        int buttonY = this.topPos + SCROLLBOX_TOP_Y;

        for (int m = 0; m < SCROLLBOX_ROWS; m++) {
            this.itemPriceButtons[m] = this.addRenderableWidget(new ExperienceScrollBoxButton(m, buttonX, buttonY, button -> {
                if (button instanceof ExperienceScrollBoxButton selectedButton) {
                    this.selectPricingRow(selectedButton.getIndex());
                }
            }));
            buttonY += SCROLLBOX_ROW_HEIGHT;
        }

        this.updateRowFocusForSelectedItemPrice();

        buttonX = this.leftPos + PRICING_IMPORT_BUTTON_X;
        buttonY = this.topPos + PRICING_IMPORT_BUTTON_Y;
        this.itemImportButton = this.addRenderableWidget(new NoFocusImageButton(
                buttonX, buttonY,
                32, 16,
                new WidgetSprites(IMPORT_FROM_CONTAINER_SPRITE, IMPORT_FROM_CONTAINER_HIGHLIGHTED_SPRITE),
                button -> {
                    ClientPlayNetworking.send(new ExperienceScreenActionC2SPayload(
                            this.menu.getContainerId(),
                            ExperienceScreenAction.IMPORT_ITEMS_FROM_TARGETS,
                            -1,
                            -1
                    ));
                    this.pricesScrolledOff = 0;
                    this.selectPricingIndex(-1);
                }
        ));
        this.itemImportButton.setTooltip(Tooltip.create(PRICING_IMPORT_TOOLTIP));

        buttonX = this.leftPos + PRICING_RESET_DEFAULT_BUTTON_X;
        buttonY = this.topPos + PRICING_RESET_DEFAULT_BUTTON_Y;
        this.itemPriceResetDefaultButton = this.addRenderableWidget(new NoFocusImageButton(
                buttonX, buttonY,
                8, 8,
                new WidgetSprites(RESET_DEFAULT_SPRITE, RESET_DEFAULT_HIGHLIGHTED_SPRITE),
                button -> {
                    ClientPlayNetworking.send(new ExperienceScreenActionC2SPayload(
                            this.menu.getContainerId(),
                            ExperienceScreenAction.RESET_DEFAULT_COST,
                            -1,
                            -1
                    ));
                }
        ));
        this.itemPriceResetDefaultButton.setTooltip(Tooltip.create(PRICING_RESET_DEFAULT_TOOLTIP));

        buttonX = this.leftPos + PRICING_ACCEPT_BUTTON_X;
        buttonY = this.topPos + PRICING_ACCEPT_BUTTON_Y;
        this.itemPriceAcceptButton = this.addRenderableWidget(new NoFocusImageButton(
                buttonX, buttonY,
                9, 9,
                new WidgetSprites(ACCEPT_SPRITE, ACCEPT_HIGHLIGHTED_SPRITE),
                button -> {
                    ClientPlayNetworking.send(new ExperienceScreenActionC2SPayload(
                            this.menu.getContainerId(),
                            ExperienceScreenAction.ACCEPT_ITEM_PRICE,
                            -1,
                            -1
                    ));
                }
        ));
        this.itemPriceAcceptButton.setTooltip(Tooltip.create(PRICING_ACCEPT_TOOLTIP));

        buttonX = this.leftPos + PRICING_CANCEL_BUTTON_X;
        buttonY = this.topPos + PRICING_CANCEL_BUTTON_Y;
        this.itemPriceCancelButton = this.addRenderableWidget(new NoFocusImageButton(
                buttonX, buttonY,
                9, 9,
                new WidgetSprites(CANCEL_SPRITE, CANCEL_HIGHLIGHTED_SPRITE),
                button -> {
                    ClientPlayNetworking.send(new ExperienceScreenActionC2SPayload(
                            this.menu.getContainerId(),
                            ExperienceScreenAction.CLEAR_ITEM_PRICE,
                            -1,
                            -1
                    ));
                    this.selectPricingIndex(-1);
                }
        ));
        this.itemPriceCancelButton.setTooltip(Tooltip.create(PRICING_CANCEL_TOOLTIP));

        buttonX = this.leftPos + PRICING_REMOVE_BUTTON_X;
        buttonY = this.topPos + PRICING_REMOVE_BUTTON_Y;
        this.itemPriceRemoveButton = this.addRenderableWidget(new NoFocusImageButton(
                buttonX, buttonY,
                12, 12,
                new WidgetSprites(TRASH_SPRITE, TRASH_HIGHLIGHTED_SPRITE),
                button -> {
                    if (!this.isItemPriceSelected()) {
                        return;
                    }
                    ClientPlayNetworking.send(new ExperienceScreenActionC2SPayload(
                            this.menu.getContainerId(),
                            ExperienceScreenAction.REMOVE_ITEM_PRICE,
                            this.selectedItemPriceIndex,
                            -1
                    ));
                    // Adjust scroll position as needed.
                    int numPrices = this.menu.getSyncedItemPrices().size() - 1;
                    if ((numPrices - this.pricesScrolledOff) < SCROLLBOX_ROWS) {
                        this.pricesScrolledOff = Math.max(0, this.pricesScrolledOff - 1);
                    }
                    this.selectPricingIndex(-1);
                }
        ));
        this.itemPriceRemoveButton.setTooltip(Tooltip.create(PRICING_REMOVE_TOOLTIP));

        buttonX = this.leftPos + PRICING_REMOVE_ALL_BUTTON_X;
        buttonY = this.topPos + PRICING_REMOVE_ALL_BUTTON_Y;
        this.itemPriceRemoveAllButton = this.addRenderableWidget(new NoFocusImageButton(
                buttonX, buttonY,
                32, 16,
                new WidgetSprites(REMOVE_ALL_SPRITE, REMOVE_ALL_HIGHLIGHTED_SPRITE),
                button -> {
                    if (this.menu.getSyncedItemPrices().isEmpty()) {
                        // No item prices to remove.
                        return;
                    }
                    ClientPlayNetworking.send(new ExperienceScreenActionC2SPayload(
                            this.menu.getContainerId(),
                            ExperienceScreenAction.REMOVE_ALL_ITEM_PRICES,
                            -1,
                            -1
                    ));
                    this.pricesScrolledOff = 0;
                    this.selectPricingIndex(-1);
                }
        ));
        this.itemPriceRemoveAllButton.setTooltip(Tooltip.create(PRICING_REMOVE_ALL_TOOLTIP));
    }

    private void removePricingButtons() {
        if (this.itemPriceButtons[0] == null) {
            // already cleared
            return;
        }

        for (var button : this.itemPriceButtons) {
            if (button != null) {
                this.removeWidget(button);
            }
        }
        Arrays.fill(this.itemPriceButtons, null);

        this.removeWidget(this.itemImportButton);
        this.removeWidget(this.itemPriceResetDefaultButton);
        this.removeWidget(this.itemPriceAcceptButton);
        this.removeWidget(this.itemPriceCancelButton);
        this.removeWidget(this.itemPriceRemoveButton);
        this.removeWidget(this.itemPriceRemoveAllButton);
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

        int xIcon = xTab + 5;
        int yIcon = yTab + 9;
        guiGraphics.renderItem(tab.getIcon(), xIcon, yIcon);
    }

    @Override
    public void renderContents(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float a) {
        super.renderContents(guiGraphics, mouseX, mouseY, a);

        if (this.selectedTab == TabDisplay.TARGETS) {
            this.renderTargetsScrollBoxContents(guiGraphics, mouseX, mouseY);
        } else if (this.selectedTab == TabDisplay.PRICING) {
            this.renderPricingScrollBoxContents(guiGraphics, mouseX, mouseY);

            // Render chest item for import button.
            guiGraphics.renderFakeItem(
                    new ItemStack(Items.CHEST),
                    this.leftPos + PRICING_IMPORT_BUTTON_X + 16,
                    this.topPos + PRICING_IMPORT_BUTTON_Y);

            // Render trash can for remove all button.
            guiGraphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    this.itemPriceRemoveAllButton.isHovered() ? TRASH_HIGHLIGHTED_SPRITE : TRASH_SPRITE,
                    this.leftPos + PRICING_REMOVE_ALL_BUTTON_X + 18,
                    this.topPos + PRICING_REMOVE_ALL_BUTTON_Y,
                    12, 12
            );

            // Render FREE in default slot when applicable.
            if (this.menu.isDefaultCostFree()) {
                guiGraphics.blitSprite(
                        RenderPipelines.GUI_TEXTURED,
                        FREE_COST_SPRITE,
                        this.leftPos + ShoppingExperienceMenu.SHOPPING_DEFAULT_COST_SLOT_X,
                        this.topPos + ShoppingExperienceMenu.SHOPPING_DEFAULT_COST_SLOT_Y,
                        16, 16
                );
            }

            // Render FREE item costs.
            if (!this.menu.getSlot(ShoppingExperienceMenu.SHOPPING_COST_SLOT).hasItem() &&
                    this.menu.getSlot(ShoppingExperienceMenu.SHOPPING_ITEM_FOR_SALE_SLOT).hasItem() &&
                    !(this.isItemPriceSelected() &&
                            this.menu.getSyncedItemPrices().get(this.selectedItemPriceIndex).cost() == null &&
                            !this.menu.isDefaultCostFree()) // special case where we don't draw FREE when selected item price is null (meaning: use default cost) and default cost is not free
            ) {
                guiGraphics.blitSprite(
                        RenderPipelines.GUI_TEXTURED,
                        FREE_COST_SPRITE,
                        this.leftPos + ShoppingExperienceMenu.SHOPPING_COST_SLOT_X,
                        this.topPos + ShoppingExperienceMenu.SHOPPING_COST_SLOT_Y,
                        16, 16
                );
            }
        }
    }

    protected void renderTargetsScrollBoxContents(GuiGraphics guiGraphics, int mouseX, int mouseY) {
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
        int yItem = this.topPos + SCROLLBOX_TOP_Y + 1;
        int xItemStart = this.leftPos + 10;
        int xTextStart = this.leftPos + 30;
        int maxTextWidth = SCROLLBOX_ROW_WIDTH - 30;

        for (int row = 0; row < SCROLLBOX_ROWS; row++) {
            if (this.targetButtons[row] == null) {
                Touristry.LOGGER.error("{}: Button in an experience screen is null", getClass().getSimpleName());
                break;
            }

            int targetIndex = this.targetsScrolledOff + row;
            if (targetIndex < syncedTargets.size()) {
                this.targetButtons[row].visible = true;

                TargetView target = syncedTargets.get(targetIndex);
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
                        this.font,
                        this.font.plainSubstrByWidth(target.displayName(), maxTextWidth),
                        xTextStart,
                        yItem + 5,
                        ARGB_SCROLLBOX_BUTTON_TEXT_COLOR,
                        true);
            } else {
                this.targetButtons[row].visible = false;
            }

            yItem += SCROLLBOX_ROW_HEIGHT;
        }
    }

    protected void renderPricingScrollBoxContents(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Render scroller in correct position or disabled.
        List<ItemPrice> itemPrices = this.menu.getSyncedItemPrices();
        this.renderScroller(guiGraphics, mouseX, mouseY, itemPrices.size(), this.pricesScrolledOff);

        // Render target buttons for visible targets.
        int yItem = this.topPos + SCROLLBOX_TOP_Y + 1;
        int xItemForSale = this.leftPos + 10;
        int xTradeArrow = this.leftPos + 40;
        int xItemCost = this.leftPos + 63;
        int xDirtyMarker = this.leftPos + SCROLLER_TRACK_X - 8;

        for (int row = 0; row < SCROLLBOX_ROWS; row++) {
            if (this.itemPriceButtons[row] == null) {
                Touristry.LOGGER.error("{}: The first button in itemPriceButtons is null", getClass().getSimpleName());
                break;
            }

            int itemPriceIndex = this.pricesScrolledOff + row;
            if (itemPriceIndex < itemPrices.size()) {
                this.itemPriceButtons[row].visible = true;

                ItemPrice itemPrice = itemPrices.get(itemPriceIndex);
                ItemStack itemForSale = itemPrice.itemForSale();

                // Render item for sale.
                guiGraphics.renderFakeItem(itemForSale, xItemForSale, yItem);
                guiGraphics.renderItemDecorations(this.font, itemForSale, xItemForSale, yItem);

                // Render item tooltip if needed, based on mouse position.
                if (this.isHovering(xItemForSale - this.leftPos, yItem - this.topPos, 16, 16, mouseX, mouseY)) {
                    guiGraphics.setTooltipForNextFrame(
                            this.font,
                            this.getTooltipFromContainerItem(itemForSale),
                            itemForSale.getTooltipImage(),
                            mouseX,
                            mouseY,
                            itemForSale.get(DataComponents.TOOLTIP_STYLE)
                    );
                }

                // Render trade arrow.
                guiGraphics.blitSprite(
                        RenderPipelines.GUI_TEXTURED,
                        TRADE_ARROW_SPRITE,
                        xTradeArrow,
                        yItem + 3,
                        10, 9
                );

                // Render cost to tourists.
                if (itemPrice.cost() == null) {
                    // Use default cost.
                    ItemStack defaultCost = this.menu.getSlot(ShoppingExperienceMenu.SHOPPING_DEFAULT_COST_SLOT).getItem();

                    // Render parentheses before and after default cost to indicate this cost is from the default cost slot.
                    guiGraphics.drawString(this.font, "(", xItemCost - 5, yItem + 5, ARGB_SCROLLBOX_BUTTON_TEXT_COLOR);

                    if (defaultCost.isEmpty()) {
                        guiGraphics.blitSprite(
                                RenderPipelines.GUI_TEXTURED,
                                FREE_COST_SPRITE,
                                xItemCost,
                                yItem,
                                16, 16
                        );
                    } else {
                        guiGraphics.renderFakeItem(defaultCost, xItemCost, yItem);
                        guiGraphics.renderItemDecorations(this.font, defaultCost, xItemCost, yItem);
                    }

                    // Render parentheses before and after default cost to indicate this cost is from the default cost slot.
                    guiGraphics.drawString(this.font, ")", xItemCost + 18, yItem + 5, ARGB_SCROLLBOX_BUTTON_TEXT_COLOR);
                } else if (itemPrice.cost() == ItemStack.EMPTY) {
                    guiGraphics.blitSprite(
                            RenderPipelines.GUI_TEXTURED,
                            FREE_COST_SPRITE,
                            xItemCost,
                            yItem,
                            16, 16
                    );
                } else {
                    guiGraphics.renderFakeItem(itemPrice.cost(), xItemCost, yItem);
                    guiGraphics.renderItemDecorations(this.font, itemPrice.cost(), xItemCost, yItem);
                }

                // Render dirty marker if applicable.
                if (itemPriceIndex == this.selectedItemPriceIndex && this.isSelectedItemPriceDirty()) {
                    guiGraphics.drawString(this.font, "*", xDirtyMarker, yItem + 2, ARGB_DIRTY_MARKER_COLOR);
                }
            } else {
                this.itemPriceButtons[row].visible = false;
            }

            yItem += SCROLLBOX_ROW_HEIGHT;
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

    @Override
    protected void renderLabels(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        switch (this.selectedTab) {
            case STATUS -> {
                guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, ARGB_SCREEN_TEXT_COLOR, false);
                this.renderStatusLabels(guiGraphics);
                guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, ARGB_SCREEN_TEXT_COLOR, false);
            }
            case TARGETS -> {
                this.renderTargetLabels(guiGraphics);
            }
            case PRICING -> {
                this.renderPricingLabels(guiGraphics, mouseX, mouseY);
                guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, ARGB_SCREEN_TEXT_COLOR, false);
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
                ARGB_SCREEN_TEXT_COLOR,
                false
        );

        guiGraphics.drawString(
                this.font,
                TARGET_KEY_LABEL,
                TARGET_KEY_LABEL_X,
                TARGET_KEY_LABEL_Y,
                ARGB_SCREEN_TEXT_COLOR,
                false
        );

        guiGraphics.drawString(
                this.font,
                ENTRY_FEE_LABEL,
                ENTRY_FEE_LABEL_X,
                ENTRY_FEE_LABEL_Y,
                ARGB_SCREEN_TEXT_COLOR,
                false
        );

        guiGraphics.drawString(
                this.font,
                STATUS_LABEL,
                STATUS_LABEL_X,
                STATUS_LABEL_Y,
                ARGB_SCREEN_TEXT_COLOR,
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
                ARGB_SCREEN_TEXT_COLOR,
                false
        );

        if (this.isTargetSelected()) {
            TargetView targetView = this.menu.getSyncedTargets().get(this.selectedTargetIndex);
            String targetDetails = (this.selectedTargetIndex + 1) + ") " + targetView.displayName();
            int maxDetailWidth = this.imageWidth - TARGET_DETAILS_LABEL_X - 5;
            guiGraphics.drawString(
                    this.font,
                    this.font.plainSubstrByWidth(targetDetails, maxDetailWidth),
                    TARGET_DETAILS_LABEL_X,
                    TARGET_DETAILS_LABEL_Y,
                    ARGB_SCREEN_TEXT_COLOR,
                    false
            );

            guiGraphics.drawString(
                    this.font,
                    "@ " + targetView.pos().toShortString(),
                    TARGET_DETAILS_LABEL_X + 20,
                    TARGET_DETAILS_LABEL_Y + 11,
                    ARGB_SCREEN_TEXT_COLOR,
                    false
            );
        }

        guiGraphics.drawString(
                this.font,
                TARGET_ORDERED_LABEL,
                TARGET_ORDERED_LABEL_X,
                TARGET_ORDERED_LABEL_Y,
                ARGB_SCREEN_TEXT_COLOR,
                false
        );
    }

    protected void renderPricingLabels(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Render pricing tab title, including number of item prices in table.
        int numItemPrices = this.menu.getSyncedItemPrices().size();
        Component pricingsTitle = PRICING_SCREEN_TITLE.copy().append(" (" + numItemPrices + ")");
        int pricingLabelWidth = this.font.width(pricingsTitle);
        guiGraphics.drawString(
                this.font,
                pricingsTitle,
                SCROLLBOX_ROW_X + ((SCROLLBOX_WIDTH - pricingLabelWidth) / 2),
                SCROLLBOX_LABEL_Y,
                ARGB_SCREEN_TEXT_COLOR,
                false
        );

        // Render default cost label and tooltip.
        int xDefaultCostLabel = PRICING_DEFAULT_LABEL_RIGHT_X - this.defaultItemPriceLabelWidth;
        guiGraphics.drawString(
                this.font,
                PRICING_DEFAULT_LABEL,
                xDefaultCostLabel,
                PRICING_DEFAULT_LABEL_Y,
                ARGB_SCREEN_TEXT_COLOR,
                false
        );

        if (this.isHovering(xDefaultCostLabel, PRICING_DEFAULT_LABEL_Y, this.defaultItemPriceLabelWidth, this.font.lineHeight, mouseX, mouseY)) {
            guiGraphics.setTooltipForNextFrame(
                    this.font,
                    PRICING_DEFAULT_LABEL_TOOLTIP,
                    mouseX,
                    mouseY
            );
        }
    }

    private boolean canScroll() {
        return switch (this.selectedTab) {
            case STATUS -> false;
            case TARGETS -> this.menu.getSyncedTargets().size() > SCROLLBOX_ROWS;
            case PRICING -> this.menu.getSyncedItemPrices().size() > SCROLLBOX_ROWS;
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

    private boolean isItemPriceSelected() {
        return this.selectedItemPriceIndex >= 0 && this.selectedItemPriceIndex < this.menu.getSyncedItemPrices().size();
    }

    private boolean isTargetSelected() {
        return this.selectedTargetIndex >= 0 && this.selectedTargetIndex < this.menu.getSyncedTargets().size();
    }

    private boolean isSelectedItemPriceDirty() {
        ItemStack slotItemForSale = this.menu.getSlot(ShoppingExperienceMenu.SHOPPING_ITEM_FOR_SALE_SLOT).getItem();
        ItemPrice selectedItemPrice = this.isItemPriceSelected()
                ? this.menu.getSyncedItemPrices().get(this.selectedItemPriceIndex)
                : null;

        if (selectedItemPrice == null || !ItemStack.isSameItemSameComponents(selectedItemPrice.itemForSale(), slotItemForSale)) {
            // No selected item or player is working on a different pricing than the selected item. Just ignore.
            return false;
        }

        if (selectedItemPrice.itemForSale().getCount() != slotItemForSale.getCount()) {
            // Player has changed the count of the item for sale.
            return true;
        }

        ItemStack selectedItemCost = selectedItemPrice.cost();

        if (selectedItemCost == null) {
            // Simple case where anything in the item cost slot will be different from a null cost in the selected item price.
            return true;
        }

        ItemStack slotItemCost = this.menu.getSlot(ShoppingExperienceMenu.SHOPPING_COST_SLOT).getItem();

        return !ItemStack.isSameItemSameComponents(selectedItemCost, slotItemCost) || selectedItemCost.getCount() != slotItemCost.getCount();
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

        int totalRows = 0;
        if (this.selectedTab == TabDisplay.TARGETS) {
            totalRows = this.menu.getSyncedTargets().size();
        } else if (this.selectedTab == TabDisplay.PRICING) {
            totalRows = this.menu.getSyncedItemPrices().size();
        }

        int maxScrolledOff = totalRows - SCROLLBOX_ROWS;
        if (maxScrolledOff <= 0) {
            if (this.selectedTab == TabDisplay.TARGETS) {
                this.targetsScrolledOff = 0;
                this.updateRowFocusForSelectedTarget();
            } else if (this.selectedTab == TabDisplay.PRICING) {
                this.pricesScrolledOff = 0;
                this.updateRowFocusForSelectedItemPrice();
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
            this.pricesScrolledOff = scrolledOff;
            this.updateRowFocusForSelectedItemPrice();
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
                int totalRows = this.menu.getSyncedItemPrices().size();
                int maxScrolledOff = totalRows - SCROLLBOX_ROWS;
                this.pricesScrolledOff = Mth.clamp((int)(this.pricesScrolledOff - scrollY), 0, maxScrolledOff);
                this.updateRowFocusForSelectedItemPrice();
            }
            return true;
        }

        return false;
    }

    private void focusOnItemForSale(ItemStack itemForSale) {
        List<ItemPrice> itemPrices = this.menu.getSyncedItemPrices();

        for (int i = 0; i < itemPrices.size(); i++) {
            if (ItemStack.isSameItemSameComponents(itemPrices.get(i).itemForSale(), itemForSale)) {
                this.selectedItemPriceIndex = i;
                this.scrollPricingIntoView(i);
                return;
            }
        }

        this.selectPricingIndex(-1);
    }

    private void scrollPricingIntoView(int index) {
        if (index < this.pricesScrolledOff) {
            this.pricesScrolledOff = index;
        } else if (index >= this.pricesScrolledOff + SCROLLBOX_ROWS) {
            this.pricesScrolledOff = index - SCROLLBOX_ROWS + 1;
        }
        this.updateRowFocusForSelectedItemPrice();
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

    private void selectPricingIndex(int selectedPricingIndex) {
        this.selectedItemPriceIndex = selectedPricingIndex;
        this.updateRowFocusForSelectedItemPrice();
    }

    private void selectPricingRow(int selectedRow) {
        this.selectedItemPriceIndex = selectedRow + this.pricesScrolledOff;
        this.updateRowFocusForSelectedItemPrice();
        if (this.selectedItemPriceIndex >= 0) {
            ClientPlayNetworking.send(new ExperienceScreenActionC2SPayload(
                    this.menu.getContainerId(),
                    ExperienceScreenAction.SELECT_ITEM_PRICE,
                    this.selectedItemPriceIndex,
                    -1
            ));
        }
    }

    private void updateRowFocusForSelectedItemPrice() {
        for (var button : this.itemPriceButtons) {
            button.setFocused(false);
        }

        if (!this.isItemPriceSelected()) {
            this.selectedItemPriceIndex = -1;
            return;
        }

        int numItemPrices = this.menu.getSyncedItemPrices().size();
        int visibleRows = Math.min(numItemPrices, SCROLLBOX_ROWS);
        int firstVisible = this.pricesScrolledOff;
        int lastVisible = this.pricesScrolledOff + visibleRows - 1;

        if (this.selectedItemPriceIndex < firstVisible || this.selectedItemPriceIndex > lastVisible) {
            return;
        }

        int visibleRow = this.selectedItemPriceIndex - firstVisible;
        this.itemPriceButtons[visibleRow].setFocused(true);
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

        if (!this.isTargetSelected()) {
            this.selectedTargetIndex = -1;
            return;
        }

        int numTargets = this.menu.getSyncedTargets().size();
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
