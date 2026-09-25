package org.bensam.touristry.client.screen.tabs;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.bensam.touristry.Touristry;
import org.bensam.touristry.client.screen.AbstractTabbedExperienceScreen;
import org.bensam.touristry.client.screen.buttons.ExperienceScrollBoxButton;
import org.bensam.touristry.client.screen.buttons.NoFocusImageButton;
import org.bensam.touristry.client.screen.components.AbstractScrollBox;
import org.bensam.touristry.menu.AbstractExperienceMenu;
import org.bensam.touristry.menu.PricingMenu;
import org.bensam.touristry.network.ExperienceScreenActionC2SPayload;
import org.bensam.touristry.tourism.experience.ExperienceScreenAction;
import org.bensam.touristry.tourism.experience.ItemPrice;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class PricingTab<T extends Enum<T>> implements ScreenTab<T> {
    private static final Component TAB_TITLE = Component.translatable("screen.touristry.tourist_block.tab.pricing");

    //region Constants: Textures and sprites
    private static final Identifier BACKGROUND_TEXTURE = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "textures/gui/tourist_experience_pricing.png");
    private static final Identifier ACCEPT_SPRITE = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "accept");
    private static final Identifier ACCEPT_HIGHLIGHTED_SPRITE = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "accept_highlighted");
    private static final Identifier CANCEL_SPRITE = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "cancel");
    private static final Identifier CANCEL_HIGHLIGHTED_SPRITE = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "cancel_highlighted");
    private static final Identifier FREE_COST_SPRITE = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "free_cost");
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
    private static final Identifier TRADE_ARROW_SPRITE = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "trade_arrow");
    private static final Identifier TRASH_SPRITE = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "trash");
    private static final Identifier TRASH_HIGHLIGHTED_SPRITE = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "trash_highlighted");
    //endregion

    //region Constants: Labels and positions
    private static final boolean PRICING_SHOW_REMOVE_ALL_BUTTON = false;
    private static final Component PRICING_IMPORT_TOOLTIP = Component.translatable("screen." + Touristry.MOD_ID + ".tourist_block.pricing.import.tooltip");
    private static final Component PRICING_DEFAULT_LABEL = Component.translatable("screen." + Touristry.MOD_ID + ".tourist_block.pricing.default.label");
    private static final Component PRICING_DEFAULT_LABEL_TOOLTIP = Component.translatable("screen." + Touristry.MOD_ID + ".tourist_block.pricing.default.label.tooltip");
    private static final Component PRICING_RESET_DEFAULT_TOOLTIP = Component.translatable("screen." + Touristry.MOD_ID + ".tourist_block.pricing.reset_default_cost.tooltip");
    private static final Component PRICING_REMOVE_TOOLTIP = Component.translatable("screen." + Touristry.MOD_ID + ".tourist_block.pricing.remove.tooltip");
    private static final Component PRICING_ACCEPT_TOOLTIP = Component.translatable("screen." + Touristry.MOD_ID + ".tourist_block.pricing.accept.tooltip");
    private static final Component PRICING_CANCEL_TOOLTIP = Component.translatable("screen." + Touristry.MOD_ID + ".tourist_block.pricing.cancel.tooltip");
    private static final Component PRICING_REMOVE_ALL_TOOLTIP = Component.translatable("screen." + Touristry.MOD_ID + ".tourist_block.pricings.remove_all.tooltip");
    private static final Component PRICING_REMOVE_DEFAULTS_TOOLTIP = Component.translatable("screen." + Touristry.MOD_ID + ".tourist_block.pricings.remove_defaults.tooltip");
    private static final int ARGB_SCROLLBOX_BUTTON_TEXT_COLOR = 0xFFFFFFFF; // white
    private static final int ARGB_DIRTY_MARKER_COLOR = 0xFFFF0000; // red
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
    private static final int PRICING_IMPORT_BUTTON_X = 107;
    private static final int PRICING_IMPORT_BUTTON_Y = SCROLLBOX_TOP_Y;
    private static final int PRICING_DEFAULT_LABEL_RIGHT_X = 210;
    private static final int PRICING_DEFAULT_LABEL_Y = 22;
    private static final int PRICING_RESET_DEFAULT_BUTTON_X = 234;
    private static final int PRICING_RESET_DEFAULT_BUTTON_Y = 26;
    private static final int PRICING_REMOVE_DEFAULTS_BUTTON_X = 107;
    private static final int PRICING_REMOVE_DEFAULTS_BUTTON_Y = PRICING_SHOW_REMOVE_ALL_BUTTON ? 37 : 55;
    private static final int PRICING_REMOVE_ALL_BUTTON_X = 107;
    private static final int PRICING_REMOVE_ALL_BUTTON_Y = 55;
    private static final int PRICING_CHANGE_FOR_SALE_QTY_BUTTON_X = 180;
    private static final int PRICING_CHANGE_FOR_SALE_QTY_BUTTON_Y = 50;
    private static final int PRICING_CHANGE_COST_QTY_BUTTON_X = 234;
    private static final int PRICING_CHANGE_COST_QTY_BUTTON_Y = 50;
    private static final int PRICING_ACCEPT_BUTTON_X = 243;
    private static final int PRICING_ACCEPT_BUTTON_Y = 50;
    private static final int PRICING_CANCEL_BUTTON_X = 243;
    private static final int PRICING_CANCEL_BUTTON_Y = 59;
    private static final int PRICING_REMOVE_BUTTON_X = 255;
    private static final int PRICING_REMOVE_BUTTON_Y = 55;
    private static final int INVENTORY_LABEL_X = 107;
    private static final int INVENTORY_LABEL_Y = 72;
    //endregion

    private final T tabEnum;
    private final ItemStack tabIcon;
    private int tabOrder;
    private final ScrollBox scrollBox = new ScrollBox();

    private ItemStack focusItemForSale = ItemStack.EMPTY;
    private int lastItemPricesRevision;
    private ImageButton itemImportButton;
    private ImageButton itemPriceRemoveButton;
    private ImageButton itemPriceResetDefaultButton;
    private ImageButton itemPriceRemoveDefaultsButton;
    private ImageButton itemPriceRemoveAllButton;
    private ImageButton itemPriceForSaleQtyUpButton;
    private ImageButton itemPriceForSaleQtyDownButton;
    private ImageButton itemPriceCostQtyUpButton;
    private ImageButton itemPriceCostQtyDownButton;
    private ImageButton itemPriceAcceptButton;
    private ImageButton itemPriceCancelButton;

    public PricingTab(T tabEnum, Item tabIcon) {
        this.tabEnum = tabEnum;
        this.tabIcon = new ItemStack(tabIcon);
    }

    // Screen setup handlers
    @Override
    public void init(AbstractExperienceMenu<?> menu) {
        // Request sync-to-client of all item prices.
        ClientPlayNetworking.send(new ExperienceScreenActionC2SPayload(
                menu.getContainerId(),
                ExperienceScreenAction.REQUEST_ITEM_PRICES,
                -1,
                -1
        ));
    }

    @Override
    public void onSelected(AbstractTabbedExperienceScreen<?, ?> screen, AbstractExperienceMenu<?> menu) {
        if (this.scrollBox.getPricingButton(0) != null) {
            // already populated
            return;
        }

        if (!(menu instanceof PricingMenu pricingMenu)) {
            return;
        }

        // Add pricing row buttons for scroll box.
        int buttonX = screen.getScreenLeft() + SCROLLBOX_ROW_X;
        int buttonY = screen.getScreenTop() + SCROLLBOX_TOP_Y;

        for (int m = 0; m < SCROLLBOX_ROWS; m++) {
            this.scrollBox.setPricingButton(m, screen.addButton(new ExperienceScrollBoxButton(m, buttonX, buttonY, button -> {
                if (button instanceof ExperienceScrollBoxButton selectedButton) {
                    this.scrollBox.selectScrollBoxRow(selectedButton.getIndex(), pricingMenu.getSyncedItemPrices().size(), menu.getContainerId());
                }
            })));
            buttonY += SCROLLBOX_ROW_HEIGHT;
        }

        // Set the focus if there already is one.
        this.scrollBox.updateRowFocusForSelectedContent(pricingMenu.getSyncedItemPrices().size());

        // Add pricing import button.
        buttonX = screen.getScreenLeft() + PRICING_IMPORT_BUTTON_X;
        buttonY = screen.getScreenTop() + PRICING_IMPORT_BUTTON_Y;
        this.itemImportButton = screen.addButton(new NoFocusImageButton(
                buttonX, buttonY,
                32, 16,
                new WidgetSprites(IMPORT_FROM_CONTAINER_SPRITE, IMPORT_FROM_CONTAINER_HIGHLIGHTED_SPRITE),
                button -> {
                    ClientPlayNetworking.send(new ExperienceScreenActionC2SPayload(
                            menu.getContainerId(),
                            ExperienceScreenAction.IMPORT_ITEMS_FROM_TARGETS,
                            -1,
                            -1
                    ));
                    this.scrollBox.deselectContentRows(pricingMenu.getSyncedItemPrices().size());
                    this.scrollBox.scrollTo(0, pricingMenu.getSyncedItemPrices().size());
                },
                PRICING_IMPORT_TOOLTIP
        ));

        // Add reset button for default price.
        buttonX = screen.getScreenLeft() + PRICING_RESET_DEFAULT_BUTTON_X;
        buttonY = screen.getScreenTop() + PRICING_RESET_DEFAULT_BUTTON_Y;
        this.itemPriceResetDefaultButton = screen.addButton(new NoFocusImageButton(
                buttonX, buttonY,
                8, 8,
                new WidgetSprites(RESET_DEFAULT_SPRITE, RESET_DEFAULT_HIGHLIGHTED_SPRITE),
                button -> {
                    ClientPlayNetworking.send(new ExperienceScreenActionC2SPayload(
                            menu.getContainerId(),
                            ExperienceScreenAction.RESET_DEFAULT_COST,
                            -1,
                            -1
                    ));
                },
                PRICING_RESET_DEFAULT_TOOLTIP
        ));

        // Add button to remove all item prices that have default values.
        buttonX = screen.getScreenLeft() + PRICING_REMOVE_DEFAULTS_BUTTON_X;
        buttonY = screen.getScreenTop() + PRICING_REMOVE_DEFAULTS_BUTTON_Y;
        this.itemPriceRemoveDefaultsButton = screen.addButton(new NoFocusImageButton(
                buttonX, buttonY,
                32, 16,
                new WidgetSprites(REMOVE_ALL_SPRITE, REMOVE_ALL_HIGHLIGHTED_SPRITE),
                button -> {
                    if (pricingMenu.getSyncedItemPrices().isEmpty()) {
                        // No item prices to remove.
                        return;
                    }
                    ClientPlayNetworking.send(new ExperienceScreenActionC2SPayload(
                            menu.getContainerId(),
                            ExperienceScreenAction.REMOVE_DEFAULT_ITEM_PRICES,
                            -1,
                            -1
                    ));
                    this.scrollBox.deselectContentRows(pricingMenu.getSyncedItemPrices().size());
                    this.scrollBox.scrollTo(0, pricingMenu.getSyncedItemPrices().size());
                },
                PRICING_REMOVE_DEFAULTS_TOOLTIP
        ));

        // Add button to remove all item prices.
        buttonX = screen.getScreenLeft() + PRICING_REMOVE_ALL_BUTTON_X;
        buttonY = screen.getScreenTop() + PRICING_REMOVE_ALL_BUTTON_Y;
        this.itemPriceRemoveAllButton = screen.addButton(new NoFocusImageButton(
                buttonX, buttonY,
                32, 16,
                new WidgetSprites(REMOVE_ALL_SPRITE, REMOVE_ALL_HIGHLIGHTED_SPRITE),
                button -> {
                    if (pricingMenu.getSyncedItemPrices().isEmpty()) {
                        // No item prices to remove.
                        return;
                    }
                    ClientPlayNetworking.send(new ExperienceScreenActionC2SPayload(
                            menu.getContainerId(),
                            ExperienceScreenAction.REMOVE_ALL_ITEM_PRICES,
                            -1,
                            -1
                    ));
                    this.scrollBox.deselectContentRows(pricingMenu.getSyncedItemPrices().size());
                    this.scrollBox.scrollTo(0, pricingMenu.getSyncedItemPrices().size());
                },
                PRICING_REMOVE_ALL_TOOLTIP
        ));
        this.itemPriceRemoveAllButton.visible = PRICING_SHOW_REMOVE_ALL_BUTTON;

        // Add button to increment the quantity of the item for sale in the item pricing slot.
        buttonX = screen.getScreenLeft() + PRICING_CHANGE_FOR_SALE_QTY_BUTTON_X;
        buttonY = screen.getScreenTop() + PRICING_CHANGE_FOR_SALE_QTY_BUTTON_Y;
        int u = 16;
        int v = 0;
        this.itemPriceForSaleQtyUpButton = screen.addButton(new NoFocusImageButton(
                buttonX, buttonY,
                8, 8,
                u, v,
                16, 16,
                new WidgetSprites(MOVE_UP_SPRITE, MOVE_UP_HIGHLIGHTED_SPRITE),
                button -> {
                    if (pricingMenu.getFocusedItemForSale().isEmpty()) {
                        // No item in for sale slot.
                        return;
                    }
                    ClientPlayNetworking.send(new ExperienceScreenActionC2SPayload(
                            menu.getContainerId(),
                            ExperienceScreenAction.ADD_TO_FOR_SALE_QTY,
                            1,
                            -1
                    ));
                }
        ));

        // Add button to decrement the quantity of the item for sale in the item pricing slot.
        buttonY += 9;
        v = 16;
        this.itemPriceForSaleQtyDownButton = screen.addButton(new NoFocusImageButton(
                buttonX, buttonY,
                8, 8,
                u, v,
                16, 16,
                new WidgetSprites(MOVE_DOWN_SPRITE, MOVE_DOWN_HIGHLIGHTED_SPRITE),
                button -> {
                    if (pricingMenu.getFocusedItemForSale().isEmpty()) {
                        // No item in for sale slot.
                        return;
                    }
                    ClientPlayNetworking.send(new ExperienceScreenActionC2SPayload(
                            menu.getContainerId(),
                            ExperienceScreenAction.ADD_TO_FOR_SALE_QTY,
                            -1,
                            -1
                    ));
                }
        ));

        // Add button to increment the cost of the item for sale in the item pricing slot.
        buttonX = screen.getScreenLeft() + PRICING_CHANGE_COST_QTY_BUTTON_X;
        buttonY = screen.getScreenTop() + PRICING_CHANGE_COST_QTY_BUTTON_Y;
        v = 0;
        this.itemPriceCostQtyUpButton = screen.addButton(new NoFocusImageButton(
                buttonX, buttonY,
                8, 8,
                u, v,
                16, 16,
                new WidgetSprites(MOVE_UP_SPRITE, MOVE_UP_HIGHLIGHTED_SPRITE),
                button -> {
                    ClientPlayNetworking.send(new ExperienceScreenActionC2SPayload(
                            menu.getContainerId(),
                            ExperienceScreenAction.ADD_TO_COST_QTY,
                            1,
                            -1
                    ));
                }
        ));

        // Add button to decrement the cost of the item for sale in the item pricing slot.
        buttonY += 9;
        v = 16;
        this.itemPriceCostQtyDownButton = screen.addButton(new NoFocusImageButton(
                buttonX, buttonY,
                8, 8,
                u, v,
                16, 16,
                new WidgetSprites(MOVE_DOWN_SPRITE, MOVE_DOWN_HIGHLIGHTED_SPRITE),
                button -> {
                    if (pricingMenu.getFocusedItemCost().isEmpty()) {
                        // No item in cost slot.
                        return;
                    }
                    ClientPlayNetworking.send(new ExperienceScreenActionC2SPayload(
                            menu.getContainerId(),
                            ExperienceScreenAction.ADD_TO_COST_QTY,
                            -1,
                            -1
                    ));
                }
        ));

        // Add button to accept the new cost and quantities of the item for sale in the item pricing slot.
        buttonX = screen.getScreenLeft() + PRICING_ACCEPT_BUTTON_X;
        buttonY = screen.getScreenTop() + PRICING_ACCEPT_BUTTON_Y;
        this.itemPriceAcceptButton = screen.addButton(new NoFocusImageButton(
                buttonX, buttonY,
                9, 9,
                new WidgetSprites(ACCEPT_SPRITE, ACCEPT_HIGHLIGHTED_SPRITE),
                button -> {
                    ClientPlayNetworking.send(new ExperienceScreenActionC2SPayload(
                            menu.getContainerId(),
                            ExperienceScreenAction.ACCEPT_ITEM_PRICE,
                            -1,
                            -1
                    ));
                },
                PRICING_ACCEPT_TOOLTIP
        ));

        // Add button to cancel any changes to the cost or quantities of the item for sale in the item pricing slot.
        buttonX = screen.getScreenLeft() + PRICING_CANCEL_BUTTON_X;
        buttonY = screen.getScreenTop() + PRICING_CANCEL_BUTTON_Y;
        this.itemPriceCancelButton = screen.addButton(new NoFocusImageButton(
                buttonX, buttonY,
                9, 9,
                new WidgetSprites(CANCEL_SPRITE, CANCEL_HIGHLIGHTED_SPRITE),
                button -> {
                    ClientPlayNetworking.send(new ExperienceScreenActionC2SPayload(
                            menu.getContainerId(),
                            ExperienceScreenAction.CLEAR_ITEM_PRICE,
                            -1,
                            -1
                    ));
                    this.scrollBox.deselectContentRows(pricingMenu.getSyncedItemPrices().size());
                },
                PRICING_CANCEL_TOOLTIP
        ));

        // Add button to remove the item pricing for the item in the item pricing slot.
        buttonX = screen.getScreenLeft() + PRICING_REMOVE_BUTTON_X;
        buttonY = screen.getScreenTop() + PRICING_REMOVE_BUTTON_Y;
        this.itemPriceRemoveButton = screen.addButton(new NoFocusImageButton(
                buttonX, buttonY,
                8, 8,
                new WidgetSprites(TRASH_SPRITE, TRASH_HIGHLIGHTED_SPRITE),
                button -> {
                    if (!this.scrollBox.isAnyContentRowSelected()) {
                        return;
                    }
                    ClientPlayNetworking.send(new ExperienceScreenActionC2SPayload(
                            menu.getContainerId(),
                            ExperienceScreenAction.REMOVE_ITEM_PRICE,
                            this.scrollBox.getSelectedPricingIndex(), 
                            -1
                    ));
                    // Adjust scroll position as needed.
                    int numTargets = menu.getSyncedTargets().size() - 1;
                    this.scrollBox.clampContentRowsScrolledOff(numTargets);
                    this.scrollBox.deselectContentRows(menu.getSyncedTargets().size());
                },
                PRICING_REMOVE_TOOLTIP
        ));
    }

    @Override
    public void onDeselected(AbstractTabbedExperienceScreen<?, ?> screen) {
        if (this.scrollBox.getPricingButton(0) == null) {
            // already cleared
            return;
        }

        for (var button : this.scrollBox.getPricingButtons()) {
            if (button != null) {
                screen.removeButton(button);
            }
        }
        Arrays.fill(this.scrollBox.getPricingButtons(), null);

        screen.removeButton(this.itemImportButton);
        screen.removeButton(this.itemPriceResetDefaultButton);
        screen.removeButton(this.itemPriceRemoveDefaultsButton);
        screen.removeButton(this.itemPriceRemoveAllButton);
        screen.removeButton(this.itemPriceForSaleQtyUpButton);
        screen.removeButton(this.itemPriceForSaleQtyDownButton);
        screen.removeButton(this.itemPriceCostQtyUpButton);
        screen.removeButton(this.itemPriceCostQtyDownButton);
        screen.removeButton(this.itemPriceAcceptButton);
        screen.removeButton(this.itemPriceCancelButton);
        screen.removeButton(this.itemPriceRemoveButton);
    }

    @Override
    public void tick(AbstractExperienceMenu<?> menu) {
        if (menu instanceof PricingMenu pricingMenu) {
            int revisionNum = pricingMenu.getSyncedItemPricesRevision();
            if (revisionNum != this.lastItemPricesRevision) {
                this.lastItemPricesRevision = revisionNum;

                ItemStack itemForSale = pricingMenu.getFocusedItemForSale();
                boolean changedItem = !ItemStack.isSameItemSameComponents(itemForSale, this.focusItemForSale);
                if (changedItem) {
                    if (itemForSale.isEmpty()) {
                        this.focusItemForSale = ItemStack.EMPTY;
                        this.scrollBox.deselectContentRows(pricingMenu.getSyncedItemPrices().size());
                    } else {
                        this.focusItemForSale = itemForSale.copy();
                        this.scrollBox.focusOnItemForSale(this.focusItemForSale, menu);
                    }
                }
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

    // Input methods
    @Override
    public boolean onMouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubleClick, double mouseLocalX, double mouseLocalY, AbstractExperienceMenu<?> menu) {
        if (menu instanceof PricingMenu pricingMenu) {
            if (mouseButtonEvent.button() == 0) {
                return this.scrollBox.checkIfScrolling(mouseLocalX, mouseLocalY, pricingMenu.getSyncedItemPrices().size());
            }
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

        if (menu instanceof PricingMenu pricingMenu) {
            this.scrollBox.onDragScroll(mouseLocalY, pricingMenu.getSyncedItemPrices().size());
            return true;
        }
        
        return false;
    }

    @Override
    public boolean onMouseScrolled(double x, double y, double scrollX, double scrollY, AbstractExperienceMenu<?> menu) {
        if (menu instanceof PricingMenu pricingMenu) {
            if (this.scrollBox.canScroll(pricingMenu.getSyncedItemPrices().size())) {
                return this.scrollBox.onScroll(scrollY, pricingMenu.getSyncedItemPrices().size());
            }
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
        if (menu instanceof PricingMenu pricingMenu) {
            this.scrollBox.renderContents(guiGraphics, mouseX, mouseY, screen, menu, pricingMenu.getSyncedItemPrices());
            
            // Render chest item for import button.
            guiGraphics.renderFakeItem(
                    new ItemStack(Items.CHEST),
                    screen.getScreenLeft() + PRICING_IMPORT_BUTTON_X + 16,
                    screen.getScreenTop() + PRICING_IMPORT_BUTTON_Y);

            // Render trash cans for remove defaults button and remove all button.
            guiGraphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    this.itemPriceRemoveDefaultsButton.isHovered() ? TRASH_HIGHLIGHTED_SPRITE : TRASH_SPRITE,
                    screen.getScreenLeft() + PRICING_REMOVE_DEFAULTS_BUTTON_X + 18,
                    screen.getScreenTop() + PRICING_REMOVE_DEFAULTS_BUTTON_Y,
                    12, 12
            );

            guiGraphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    this.itemPriceRemoveAllButton.visible && this.itemPriceRemoveAllButton.isHovered() ? TRASH_HIGHLIGHTED_SPRITE : TRASH_SPRITE,
                    screen.getScreenLeft() + PRICING_REMOVE_ALL_BUTTON_X + 18,
                    screen.getScreenTop() + PRICING_REMOVE_ALL_BUTTON_Y,
                    12, 12
            );

            // Render FREE in default slot when applicable.
            if (pricingMenu.isDefaultCostFree()) {
                guiGraphics.blitSprite(
                        RenderPipelines.GUI_TEXTURED,
                        FREE_COST_SPRITE,
                        screen.getScreenLeft() + pricingMenu.getDefaultCostSlot().x,
                        screen.getScreenTop() + pricingMenu.getDefaultCostSlot().y,
                        16, 16
                );
            }

            // Render FREE item costs.
            if (pricingMenu.getFocusedItemCost().isEmpty() &&
                    !pricingMenu.getFocusedItemForSale().isEmpty() &&
                    !(this.scrollBox.isValidContentRowSelected(pricingMenu.getSyncedItemPrices().size()) &&
                            pricingMenu.getSyncedItemPrices().get(this.scrollBox.getSelectedPricingIndex()).cost() == null &&
                            !pricingMenu.isDefaultCostFree()) // special case where we don't draw FREE when selected item price is null (meaning: use default cost) and default cost is not free
            ) {
                guiGraphics.blitSprite(
                        RenderPipelines.GUI_TEXTURED,
                        FREE_COST_SPRITE,
                        screen.getScreenLeft() + pricingMenu.getFocusedItemCostSlot().x,
                        screen.getScreenTop() + pricingMenu.getFocusedItemCostSlot().y,
                        16, 16
                );
            }
        }
    }

    @Override
    public void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY, AbstractTabbedExperienceScreen<?, ?> screen, AbstractExperienceMenu<?> menu) {
        guiGraphics.drawString(screen.getFont(), screen.getPlayerInventoryTitle(), INVENTORY_LABEL_X, INVENTORY_LABEL_Y, AbstractTabbedExperienceScreen.ARGB_SCREEN_TEXT_COLOR, false);

        if (!(menu instanceof PricingMenu pricingMenu)) {
            return;
        }

        // Render pricing tab title, including number of item prices in table.
        int numItemPrices = pricingMenu.getSyncedItemPrices().size();
        Component pricingTitle = TAB_TITLE.copy().append(" (" + numItemPrices + ")");
        int pricingLabelWidth = screen.getFont().width(pricingTitle);
        guiGraphics.drawString(
                screen.getFont(),
                pricingTitle,
                SCROLLBOX_ROW_X + ((SCROLLBOX_WIDTH - pricingLabelWidth) / 2),
                SCROLLBOX_LABEL_Y,
                AbstractTabbedExperienceScreen.ARGB_SCREEN_TEXT_COLOR,
                false
        );

        // Render default cost label and tooltip.
        int defaultItemPriceLabelWidth = screen.getFont().width(PRICING_DEFAULT_LABEL);
        int xDefaultCostLabel = PRICING_DEFAULT_LABEL_RIGHT_X - defaultItemPriceLabelWidth;
        guiGraphics.drawString(
                screen.getFont(),
                PRICING_DEFAULT_LABEL,
                xDefaultCostLabel,
                PRICING_DEFAULT_LABEL_Y,
                AbstractTabbedExperienceScreen.ARGB_SCREEN_TEXT_COLOR,
                false
        );

        if (screen.isHovering(xDefaultCostLabel, PRICING_DEFAULT_LABEL_Y, defaultItemPriceLabelWidth, screen.getFont().lineHeight, mouseX, mouseY)) {
            guiGraphics.setTooltipForNextFrame(
                    screen.getFont(),
                    PRICING_DEFAULT_LABEL_TOOLTIP,
                    mouseX,
                    mouseY
            );
        }
    }

    public class ScrollBox extends AbstractScrollBox<ItemPrice> {
        public ScrollBox() {
            super(new ExperienceScrollBoxButton[SCROLLBOX_ROWS], SCROLLER_TRACK_X, SCROLLER_TRACK_TOP_Y, SCROLLER_TRACK_LENGTH);
        }

        public void focusOnItemForSale(ItemStack itemForSale, AbstractExperienceMenu<?> menu) {
            if (menu instanceof PricingMenu pricingMenu) {
                List<ItemPrice> itemPrices = pricingMenu.getSyncedItemPrices();

                for (int i = 0; i < itemPrices.size(); i++) {
                    if (ItemStack.isSameItemSameComponents(itemPrices.get(i).itemForSale(), itemForSale)) {
                        this.selectContentRow(i, itemPrices.size());
                        this.scrollTo(i, itemPrices.size());
                        return;
                    }
                }

                this.deselectContentRows(itemPrices.size());
            }
        }

        public @Nullable ExperienceScrollBoxButton getPricingButton(int index) {
            if (index < SCROLLBOX_ROWS) {
                return (ExperienceScrollBoxButton) this.rowWidgets[index];
            }
            return null;
        }

        public int getSelectedPricingIndex() {
            return this.selectedContentRow;
        }

        public ExperienceScrollBoxButton[] getPricingButtons() {
            return (ExperienceScrollBoxButton[]) this.rowWidgets;
        }

        private boolean isSelectedItemPriceDirty(@NonNull ItemPrice selectedItemPrice, PricingMenu pricingMenu) {
            ItemStack slotItemForSale = pricingMenu.getFocusedItemForSale();

            if (!ItemStack.isSameItemSameComponents(selectedItemPrice.itemForSale(), slotItemForSale)) {
                // Player is working on a different pricing than the selected item. Just ignore.
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

            ItemStack slotItemCost = pricingMenu.getFocusedItemCost();
            return !ItemStack.isSameItemSameComponents(selectedItemCost, slotItemCost) || selectedItemCost.getCount() != slotItemCost.getCount();
        }

        public void selectScrollBoxRow(int selectedRow, int numContentRows, int menuContainerId) {
            this.selectScrollBoxRow(selectedRow, numContentRows);
            
            if (this.selectedContentRow >= 0) {
                ClientPlayNetworking.send(new ExperienceScreenActionC2SPayload(
                        menuContainerId,
                        ExperienceScreenAction.SELECT_ITEM_PRICE,
                        this.selectedContentRow,
                        -1
                ));
            }
        }

        public void setPricingButton(int index, ExperienceScrollBoxButton button) {
            if (index < SCROLLBOX_ROWS) {
                this.rowWidgets[index] = button;
            }
        }

        @Override
        protected void renderRow(GuiGraphics guiGraphics, int mouseX, int mouseY, int row, AbstractTabbedExperienceScreen<?, ?> screen, AbstractExperienceMenu<?> menu, @NonNull ItemPrice itemPrice) {
            if (!(menu instanceof PricingMenu pricingMenu)) {
                return;
            }

            int screenLeft = screen.getScreenLeft();
            int screenTop = screen.getScreenTop();
            int yItem = screenTop + SCROLLBOX_TOP_Y + 1 + (row * SCROLLBOX_ROW_HEIGHT);
            int xItemForSale = screenLeft + 10;
            int xTradeArrow = screenLeft + 40;
            int xItemCost = screenLeft + 63;
            int xDirtyMarker = screenLeft + SCROLLER_TRACK_X - 8;

            ItemStack itemForSale = itemPrice.itemForSale();
            ItemStack itemCost = itemPrice.cost();

            // Render item for sale.
            guiGraphics.renderFakeItem(itemForSale, xItemForSale, yItem);
            guiGraphics.renderItemDecorations(screen.getFont(), itemForSale, xItemForSale, yItem);

            // Render item tooltip if mouse is hovering over item for sale.
            if (screen.isHovering(xItemForSale - screenLeft, yItem - screenTop, 16, 16, mouseX, mouseY)) {
                guiGraphics.setTooltipForNextFrame(
                        screen.getFont(),
                        screen.getTooltipFromContainerItem(itemForSale),
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
            if (itemCost == null) {
                // Use default cost.
                ItemStack defaultCost = pricingMenu.getDefaultCost();

                // Render parentheses before and after default cost to indicate this cost is from the default cost slot.
                guiGraphics.drawString(screen.getFont(), "(", xItemCost - 5, yItem + 5, ARGB_SCROLLBOX_BUTTON_TEXT_COLOR);

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
                    guiGraphics.renderItemDecorations(screen.getFont(), defaultCost, xItemCost, yItem);

                    // Render item tooltip if mouse is hovering over item cost.
                    if (screen.isHovering(xItemCost - screenLeft, yItem - screenTop, 16, 16, mouseX, mouseY)) {
                        guiGraphics.setTooltipForNextFrame(
                                screen.getFont(),
                                screen.getTooltipFromContainerItem(defaultCost),
                                defaultCost.getTooltipImage(),
                                mouseX,
                                mouseY,
                                defaultCost.get(DataComponents.TOOLTIP_STYLE)
                        );
                    }
                }

                // Render parentheses before and after default cost to indicate this cost is from the default cost slot.
                guiGraphics.drawString(screen.getFont(), ")", xItemCost + 18, yItem + 5, ARGB_SCROLLBOX_BUTTON_TEXT_COLOR);
            } else if (itemCost == ItemStack.EMPTY) {
                guiGraphics.blitSprite(
                        RenderPipelines.GUI_TEXTURED,
                        FREE_COST_SPRITE,
                        xItemCost,
                        yItem,
                        16, 16
                );
            } else {
                guiGraphics.renderFakeItem(itemCost, xItemCost, yItem);
                guiGraphics.renderItemDecorations(screen.getFont(), itemCost, xItemCost, yItem);

                // Render item tooltip if mouse is hovering over item cost.
                if (screen.isHovering(xItemCost - screenLeft, yItem - screenTop, 16, 16, mouseX, mouseY)) {
                    guiGraphics.setTooltipForNextFrame(
                            screen.getFont(),
                            screen.getTooltipFromContainerItem(itemCost),
                            itemCost.getTooltipImage(),
                            mouseX,
                            mouseY,
                            itemCost.get(DataComponents.TOOLTIP_STYLE)
                    );
                }
            }

            // Render dirty marker if applicable.
            int itemPriceIndex = this.contentRowsScrolledOff + row;
            if (itemPriceIndex == this.selectedContentRow && this.isSelectedItemPriceDirty(itemPrice, pricingMenu)) {
                guiGraphics.drawString(screen.getFont(), "*", xDirtyMarker, yItem + 2, ARGB_DIRTY_MARKER_COLOR);
            }
        }
    }
}
