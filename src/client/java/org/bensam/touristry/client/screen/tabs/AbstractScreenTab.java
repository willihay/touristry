package org.bensam.touristry.client.screen.tabs;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.bensam.touristry.client.screen.AbstractTabbedExperienceScreen;
import org.bensam.touristry.menu.AbstractExperienceMenu;

public abstract class AbstractScreenTab<T extends Enum<T>> {
    private final Component tabTitle;
    private final T tabEnum;
    private final ItemStack tabIcon;
    private final Identifier tabBackgroundTexture;

    private int tabOrder;

    public AbstractScreenTab(Component tabTitle, T tabEnum, Item tabIcon, Identifier tabBackgroundTexture) {
        this.tabTitle = tabTitle;
        this.tabEnum = tabEnum;
        this.tabIcon = new ItemStack(tabIcon);
        this.tabBackgroundTexture = tabBackgroundTexture;
    }

    // Screen setup handlers
    public void init(AbstractExperienceMenu<?> menu) {}
    public abstract void onSelected(AbstractTabbedExperienceScreen<?, ?> screen, AbstractExperienceMenu<?> menu);
    public abstract void onDeselected(AbstractTabbedExperienceScreen<?, ?> screen);
    public void tick(AbstractExperienceMenu<?> menu) {}

    // Tab properties
    public Component getTabTitle() {
        return this.tabTitle;
    }

    public T getTabEnum() {
        return this.tabEnum;
    }

    public int getTabOrder() {
        return this.tabOrder;
    }

    public void setTabOrder(int tabOrder) {
        this.tabOrder = tabOrder;
    }

    // Input methods
    public boolean onMouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubleClick, double mouseLocalX, double mouseLocalY, AbstractExperienceMenu<?> menu) { return false; }
    public boolean onMouseReleased(MouseButtonEvent mouseButtonEvent, double mouseLocalX, double mouseLocalY, AbstractExperienceMenu<?> menu) { return false; }
    public boolean onMouseDragged(MouseButtonEvent mouseButtonEvent, double dx, double dy, double mouseLocalX, double mouseLocalY, AbstractExperienceMenu<?> menu) { return false; }
    public boolean onMouseScrolled(double x, double y, double scrollX, double scrollY, AbstractExperienceMenu<?> menu) { return false; }

    // Render methods
    public Identifier getBackground() {
        return this.tabBackgroundTexture;
    }

    public ItemStack getTabIcon() {
        return this.tabIcon.copy();
    }

    public abstract void renderContents(GuiGraphics guiGraphics, int mouseX, int mouseY, AbstractTabbedExperienceScreen<?, ?> screen, AbstractExperienceMenu<?> menu);
    public abstract void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY, AbstractTabbedExperienceScreen<?, ?> screen, AbstractExperienceMenu<?> menu);
}
