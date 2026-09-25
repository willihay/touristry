package org.bensam.touristry.client.screen;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.bensam.touristry.Touristry;
import org.bensam.touristry.client.screen.tabs.ScreenTab;
import org.bensam.touristry.menu.AbstractExperienceMenu;
import org.jspecify.annotations.NonNull;

import java.util.List;

public abstract class AbstractTabbedExperienceScreen<M extends AbstractExperienceMenu<T>, T extends Enum<T>> extends AbstractContainerScreen<M> {
    public static final int ARGB_SCREEN_TEXT_COLOR = 0xFF404040; // gray
    private static final int TAB_WIDTH = 26;
    private static final int TAB_HEIGHT = 32;

    // Tab textures
    protected static final Identifier[] UNSELECTED_TOP_TABS = new Identifier[]{
            Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "tab_top_unselected_left"),
            Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "tab_top_unselected_middle"),
            Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "tab_top_unselected_right")
    };
    protected static final Identifier[] SELECTED_TOP_TABS = new Identifier[]{
            Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "tab_top_selected_left"),
            Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "tab_top_selected_middle"),
            Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "tab_top_selected_right")
    };

    protected boolean isScrolling;
    private ScreenTab<T> selectedTab;
    protected final List<ScreenTab<T>> tabs;

    public AbstractTabbedExperienceScreen(M containerMenu, Inventory inventory, Component title, List<ScreenTab<T>> tabs) {
        super(containerMenu, inventory, title);
        this.tabs = tabs;

        if (!tabs.isEmpty()) {
            this.selectedTab = this.tabs.getFirst();
        }

        for (int i = 0; i < tabs.size(); ++i) {
            this.tabs.get(i).setTabOrder(i);
        }
    }

    @Override
    protected void init() {
        super.init();

        if (this.selectedTab != null) {
            this.selectTab(this.selectedTab);
        }

        for (ScreenTab<T> tab : this.tabs) {
            tab.init(this.menu);
        }
    }

    @Override
    protected void containerTick() {
        if (this.selectedTab != null) {
            this.selectedTab.tick(this.menu);
        }
    }

    // Helpers
    public <B extends GuiEventListener & Renderable & NarratableEntry> B addButton(B button) {
        return this.addRenderableWidget(button);
    }

    private boolean checkTabClicked(double mouseLocalX, double mouseLocalY, ScreenTab<T> tab) {
        int tabX = this.getTabLeft(tab);
        int tabY = this.getTabTop();
        return mouseLocalX >= tabX && mouseLocalX <= tabX + TAB_WIDTH && mouseLocalY >= tabY && mouseLocalY <= tabY + TAB_HEIGHT;
    }

    private void checkTabHovering(GuiGraphics guiGraphics, int mouseX, int mouseY, ScreenTab<T> tab) {
        int tabX = this.getTabLeft(tab);
        int tabY = this.getTabTop();
        if (this.isHovering(tabX + 3, tabY + 3, TAB_WIDTH - 5, TAB_HEIGHT - 5, mouseX, mouseY)) {
            guiGraphics.setTooltipForNextFrame(this.font, tab.getTabTitle(), mouseX, mouseY);
        }
    }

    protected int getBackgroundTextureWidth() {
        return BACKGROUND_TEXTURE_WIDTH;
    }

    protected int getBackgroundTextureHeight() {
        return BACKGROUND_TEXTURE_HEIGHT;
    }

    public Component getPlayerInventoryTitle() {
        return this.playerInventoryTitle;
    }

    public int getScreenLeft() {
        return this.leftPos;
    }

    public int getScreenTop() {
        return this.topPos;
    }

    public abstract int getScreenWidth();

    public abstract int getScreenHeight();

    private int getTabLeft(ScreenTab<T> tab) {
        return (TAB_WIDTH + 1) * tab.getTabOrder();
    }

    private int getTabTop() {
        return -(TAB_HEIGHT - 4);
    }

    private Identifier getTabTexture(boolean isSelected, int tabPosition) {
        int index = 0; // leftmost tab
        if (tabPosition == this.tabs.size() - 1) {
            index = 2; // rightmost tab
        } else if (tabPosition > 0) {
            index = 1; // a middle tab
        }
        return isSelected ? SELECTED_TOP_TABS[index] : UNSELECTED_TOP_TABS[index];
    }

    @Override
    public List<Component> getTooltipFromContainerItem(ItemStack itemStack) {
        return super.getTooltipFromContainerItem(itemStack);
    }

    @Override
    public boolean isHovering(int left, int top, int width, int height, double mouseX, double mouseY) {
        return super.isHovering(left, top, width, height, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubleClick) {
        double mouseLocalX = mouseButtonEvent.x() - this.leftPos;
        double mouseLocalY = mouseButtonEvent.y() - this.topPos;

        if (mouseButtonEvent.button() == 0) {
            for (ScreenTab<T> tab : this.tabs) {
                if (this.checkTabClicked(mouseLocalX, mouseLocalY, tab)) {
                    return true;
                }
            }
        }

        if (this.selectedTab != null && this.selectedTab.onMouseClicked(mouseButtonEvent, doubleClick, mouseLocalX, mouseLocalY, this.menu)) {
            return true;
        }

        return super.mouseClicked(mouseButtonEvent, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent mouseButtonEvent) {
        boolean handled = false;
        double mouseLocalX = mouseButtonEvent.x() - this.leftPos;
        double mouseLocalY = mouseButtonEvent.y() - this.topPos;

        if (mouseButtonEvent.button() == 0) {
            for (ScreenTab<T> tab : this.tabs) {
                if (this.checkTabClicked(mouseLocalX, mouseLocalY, tab)) {
                    this.selectTab(tab);
                    handled = true;
                    break;
                }
            }
        }

        if (this.selectedTab != null && this.selectedTab.onMouseReleased(mouseButtonEvent, mouseLocalX, mouseLocalY, this.menu)) {
            handled = true;
        }

        return handled || super.mouseReleased(mouseButtonEvent);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent mouseButtonEvent, double dx, double dy) {
        double mouseLocalX = mouseButtonEvent.x() - this.leftPos;
        double mouseLocalY = mouseButtonEvent.y() - this.topPos;
        if (this.selectedTab != null && this.selectedTab.onMouseDragged(mouseButtonEvent, dx, dy, mouseLocalX, mouseLocalY, this.menu)) {
            return true;
        }

        return super.mouseDragged(mouseButtonEvent, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (this.selectedTab != null && this.selectedTab.onMouseScrolled(x, y, scrollX, scrollY, this.menu)) {
            return true;
        }

        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    public void removeButton(GuiEventListener button) {
        this.removeWidget(button);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float a) {
        super.render(guiGraphics, mouseX, mouseY, a);

        for (ScreenTab<T> tab : this.tabs) {
            this.checkTabHovering(guiGraphics, mouseX, mouseY, tab);
        }

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float a, int mouseX, int mouseY) {
        // Render unselected tabs.
        for (ScreenTab<T> tab : this.tabs) {
            if (tab != this.selectedTab) {
                this.renderTabButton(guiGraphics, mouseX, mouseY, tab, false);
            }
        }

        // Render selected tab.
        if (this.selectedTab != null) {
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
                    this.getBackgroundTextureWidth(),
                    this.getBackgroundTextureHeight()
            );

            // Render tab in forefront.
            this.renderTabButton(guiGraphics, mouseX, mouseY, this.selectedTab, true);
        }
    }

    private void renderTabButton(GuiGraphics guiGraphics, int mouseX, int mouseY, ScreenTab<T> tab, boolean isSelectedTab) {
        int xTab = this.leftPos + this.getTabLeft(tab);
        int yTab = this.topPos + this.getTabTop();
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, this.getTabTexture(isSelectedTab, tab.getTabOrder()), xTab, yTab, 26, 32);

        if (!isSelectedTab && mouseX > xTab && mouseY > yTab && mouseX < xTab + TAB_WIDTH && mouseY < yTab + TAB_HEIGHT) {
            guiGraphics.requestCursor(CursorTypes.POINTING_HAND);
        }

        int xIcon = xTab + 5;
        int yIcon = yTab + 9;
        guiGraphics.renderItem(tab.getTabIcon(), xIcon, yIcon);
    }

    @Override
    public void renderContents(GuiGraphics guiGraphics, int mouseX, int mouseY, float a) {
        super.renderContents(guiGraphics, mouseX, mouseY, a);
        if (this.selectedTab != null) {
            this.selectedTab.renderContents(guiGraphics, mouseX, mouseY, this, this.menu);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (this.selectedTab != null) {
            this.selectedTab.renderLabels(guiGraphics, mouseX, mouseY, this, this.menu);
        }
    }

    protected void selectTab(@NonNull ScreenTab<T> tab) {
        if (this.selectedTab != null) {
            this.selectedTab.onDeselected(this);
        }

        this.selectedTab = tab;
        this.menu.setSelectedTab(tab.getTabName());
        this.selectedTab.onSelected(this, this.menu);

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(
                    this.menu.getContainerId(),
                    tab.getTabName().ordinal()
            );
        }
    }
}
