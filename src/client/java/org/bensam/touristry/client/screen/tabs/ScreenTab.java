package org.bensam.touristry.client.screen.tabs;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.bensam.touristry.client.screen.AbstractTabbedExperienceScreen;
import org.bensam.touristry.menu.AbstractExperienceMenu;

public interface ScreenTab<T extends Enum<T>> {
    // Screen setup handlers
    default void init(AbstractExperienceMenu<?> menu) {}
    void onSelected(AbstractTabbedExperienceScreen<?, ?> screen, AbstractExperienceMenu<?> menu);
    void onDeselected(AbstractTabbedExperienceScreen<?, ?> screen);
    default void tick(AbstractExperienceMenu<?> menu) {}

    // Tab properties
    T getTabEnum();
    int getTabOrder();
    Component getTabTitle();
    void setTabOrder(int tabOrder);

    // Input methods
    default boolean onMouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubleClick, double mouseLocalX, double mouseLocalY, AbstractExperienceMenu<?> menu) { return false; }
    default boolean onMouseReleased(MouseButtonEvent mouseButtonEvent, double mouseLocalX, double mouseLocalY, AbstractExperienceMenu<?> menu) { return false; }
    default boolean onMouseDragged(MouseButtonEvent mouseButtonEvent, double dx, double dy, double mouseLocalX, double mouseLocalY, AbstractExperienceMenu<?> menu) { return false; }
    default boolean onMouseScrolled(double x, double y, double scrollX, double scrollY, AbstractExperienceMenu<?> menu) { return false; }

    // Render methods
    Identifier getBackground();
    ItemStack getTabIcon();
    void renderContents(GuiGraphics guiGraphics, int mouseX, int mouseY, AbstractTabbedExperienceScreen<?, ?> screen, AbstractExperienceMenu<?> menu);
    void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY, AbstractTabbedExperienceScreen<?, ?> screen, AbstractExperienceMenu<?> menu);
}
