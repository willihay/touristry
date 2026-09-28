package org.bensam.touristry.client.screen.components;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.bensam.touristry.Touristry;
import org.bensam.touristry.client.screen.AbstractTabbedExperienceScreen;
import org.bensam.touristry.menu.AbstractExperienceMenu;
import org.jspecify.annotations.NonNull;

import java.util.List;

public abstract class AbstractScrollBox<R> {
    private static final Identifier SCROLLER_SPRITE = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "scroller");
    private static final Identifier SCROLLER_DISABLED_SPRITE = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "scroller_disabled");
    private static final int SCROLLER_WIDTH = 6;
    private static final int SCROLLER_HEIGHT = 27;
    private static final float SCROLLER_HALF_HEIGHT = SCROLLER_HEIGHT / 2.0F;

    protected final AbstractWidget[] rowWidgets;
    protected final int rows;
    protected final int scrollerTrackLength;
    protected final int scrollerTrackX;
    protected final int scrollerTrackTopY;
    protected final int scrollerTrackBottomY;

    protected boolean isScrolling;
    protected int contentRowsScrolledOff;
    protected int selectedContentRow;

    public AbstractScrollBox(AbstractWidget[] rowWidgets, int scrollerTrackX, int scrollerTrackTopY, int scrollerTrackLength) {
        this.rowWidgets = rowWidgets;
        this.rows = rowWidgets.length;
        this.scrollerTrackLength = scrollerTrackLength;
        this.scrollerTrackX = scrollerTrackX;
        this.scrollerTrackTopY = scrollerTrackTopY;
        this.scrollerTrackBottomY = scrollerTrackTopY + scrollerTrackLength - 1;
        this.selectedContentRow = -1;
    }

    public boolean canScroll(int numContentRows) {
        return numContentRows > this.rows;
    }

    public void clampContentRowsScrolledOff(int numContentRows) {
        this.contentRowsScrolledOff = Math.max(0, Math.min(this.contentRowsScrolledOff, numContentRows - this.rows)); // cannot use Math.clamp here because (numContentRows - rows) might be negative
    }

    public void deselectContentRows(int numContentRows) {
        this.selectedContentRow = -1;
        this.updateRowFocusForSelectedContent(numContentRows);
    }

    protected int getScrollerOffset(int numContentRows) {
        int scrollSteps = numContentRows - this.rows;
        if (scrollSteps <= 0) {
            return 0;
        }
        
        // Compute how far the scroller can travel.
        int maxScrollerOffset = this.scrollerTrackLength - SCROLLER_HEIGHT;

        // Compute the actual scroller travel.
        int scrollerOffset = 0;

        // Check for the simple case.
        if (this.contentRowsScrolledOff == scrollSteps) {
            scrollerOffset = maxScrollerOffset;
        } else {
            // Distribute the track height across all scroll steps.
            float pixelsPerStep = (float) maxScrollerOffset / scrollSteps;

            // Compute the scroller's offset based on how many rows have been scrolled off.
            scrollerOffset = Math.round(pixelsPerStep * this.contentRowsScrolledOff);
        }

        return scrollerOffset;
    }

    public boolean isAnyContentRowSelected() {
        return this.selectedContentRow >= 0;
    }

    protected boolean isInsideScrollTrack(double mouseLocalX, double mouseLocalY) {
        return mouseLocalX >= this.scrollerTrackX &&
                mouseLocalX < this.scrollerTrackX + SCROLLER_WIDTH &&
                mouseLocalY >= this.scrollerTrackTopY &&
                mouseLocalY <= this.scrollerTrackBottomY;
    }

    protected boolean isInsideScroller(double mouseLocalX, double mouseLocalY, int numContentRows) {
        int scrollerOffset = this.getScrollerOffset(numContentRows);
        int scrollerY = this.scrollerTrackTopY + scrollerOffset;
        return mouseLocalX >= this.scrollerTrackX &&
                mouseLocalX < this.scrollerTrackX + SCROLLER_WIDTH &&
                mouseLocalY >= scrollerY &&
                mouseLocalY < scrollerY + SCROLLER_HEIGHT;
    }

    public boolean isScrolling() {
        return this.isScrolling;
    }

    public boolean isValidContentRowSelected(int numContentRows) {
        return this.selectedContentRow >= 0 && this.selectedContentRow < numContentRows;
    }

    public boolean onClick(double mouseLocalX, double mouseLocalY, int numContentRows) {
        boolean handled = this.canScroll(numContentRows) && this.isInsideScrollTrack(mouseLocalX, mouseLocalY);
        if (handled) {
            if (this.isInsideScroller(mouseLocalX, mouseLocalY, numContentRows)) {
                this.isScrolling = true;
                return true;
            }

            // Mouse clicked inside scroller track but outside scroller.
            // Move the scroller to that step position.
            int scrollSteps = numContentRows - this.rows;
            float clickedOffset = (float) mouseLocalY - this.scrollerTrackTopY;
            int maxScrollerOffset = this.scrollerTrackLength - SCROLLER_HEIGHT;
            float pixelsPerStep = (float) maxScrollerOffset / scrollSteps;
            int newContentRow = (int) (clickedOffset / pixelsPerStep);
            if (this.contentRowsScrolledOff == newContentRow) {
                newContentRow++; // if we're not scrolling up, always scroll down by at least one (since we know the player clicked outside the scroller)
            }
            this.contentRowsScrolledOff = Math.clamp(newContentRow, 0, scrollSteps);
            this.updateRowFocusForSelectedContent(numContentRows);
        }

        this.isScrolling = false;
        return handled;
    }

    public void onDragScroll(double mouseLocalY, int numContentRows) {
        int maxScrolledOff = numContentRows - this.rows;
        if (maxScrolledOff <= 0) {
            this.contentRowsScrolledOff = 0;
            this.updateRowFocusForSelectedContent(numContentRows);
            return;
        }

        // Convert the mouse's Y position on the scrollbar into a number from 0 to maxScrolledOff.
        float scrollableTrackLength = this.scrollerTrackLength - SCROLLER_HEIGHT;
        float scrollerCenterY = ((float) mouseLocalY) - this.scrollerTrackTopY - SCROLLER_HALF_HEIGHT;
        float scrollFraction = scrollerCenterY / scrollableTrackLength;
        int rowOffset = (int)(scrollFraction * maxScrolledOff + 0.5F); // rounded to nearest integer
        this.contentRowsScrolledOff = Mth.clamp(rowOffset, 0, maxScrolledOff);
        this.updateRowFocusForSelectedContent(numContentRows);
    }

    public boolean onScroll(double scrollY, int numContentRows) {
        int previousScrolledOff = this.contentRowsScrolledOff;
        int maxScrolledOff = numContentRows - this.rows;
        this.contentRowsScrolledOff = Mth.clamp((int)(this.contentRowsScrolledOff - scrollY), 0, maxScrolledOff);
        this.updateRowFocusForSelectedContent(numContentRows);
        return this.contentRowsScrolledOff != previousScrolledOff;
    }

    public void renderContents(GuiGraphics guiGraphics, int mouseX, int mouseY, AbstractTabbedExperienceScreen<?, ?> screen, AbstractExperienceMenu<?> menu, List<R> scrollBoxContents) {
        // Render visible rows of scroll box.
        for (int row = 0; row < this.rows; row++) {
            if (this.rowWidgets[row] == null) {
                Touristry.LOGGER.error("{}: Scroll box widget in row {} is null", this.getClass().getSimpleName(), row);
                break;
            }

            int contentIndex = this.contentRowsScrolledOff + row;
            if (contentIndex < scrollBoxContents.size()) {
                this.rowWidgets[row].visible = true;
                this.renderRow(guiGraphics, mouseX, mouseY, row, screen, menu, scrollBoxContents.get(contentIndex));
            } else {
                this.rowWidgets[row].visible = false;
            }
        }

        // Render scroller in correct position or disabled.
        this.renderScroller(guiGraphics, mouseX, mouseY, screen.getScreenLeft(), screen.getScreenTop(), scrollBoxContents.size());
    }

    protected abstract void renderRow(GuiGraphics guiGraphics, int mouseX, int mouseY, int row, AbstractTabbedExperienceScreen<?, ?> screen, AbstractExperienceMenu<?> menu, @NonNull R content);

    protected void renderScroller(GuiGraphics guiGraphics, int mouseX, int mouseY, int screenLeft, int screenTop, int numContentRows) {
        int scrollSteps = numContentRows - this.rows;
        int scrollerX = screenLeft + this.scrollerTrackX;
        int trackTopY = screenTop + this.scrollerTrackTopY;

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

        // Draw the scroller in the correct position.
        int scrollerY = trackTopY + this.getScrollerOffset(numContentRows);
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
                        mouseX < scrollerX + SCROLLER_WIDTH &&
                        mouseY >= scrollerY &&
                        mouseY < scrollerY + SCROLLER_HEIGHT;
        if (mouseOverScroller) {
            guiGraphics.requestCursor(this.isScrolling ? CursorTypes.RESIZE_NS : CursorTypes.POINTING_HAND);
        }
    }

    public void scrollTo(int contentRow, int numContentRows) {
        if (contentRow < this.contentRowsScrolledOff) {
            this.contentRowsScrolledOff = contentRow;
        } else if (contentRow >= this.contentRowsScrolledOff + this.rows) {
            this.contentRowsScrolledOff = contentRow - this.rows + 1;
        }
        this.updateRowFocusForSelectedContent(numContentRows);
    }

    public void selectContentRow(int contentRow, int numContentRows) {
        this.selectedContentRow = contentRow;
        this.updateRowFocusForSelectedContent(numContentRows);
    }

    public void selectScrollBoxRow(int selectedRow, int numContentRows) {
        this.selectedContentRow = selectedRow + this.contentRowsScrolledOff;
        this.updateRowFocusForSelectedContent(numContentRows);
    }

    public void setIsScrolling(boolean isScrolling) {
        this.isScrolling = isScrolling;
    }

    public void updateRowFocusForSelectedContent(int numContentRows) {
        for (var widget : this.rowWidgets) {
            widget.setFocused(false);
        }

        if (!this.isValidContentRowSelected(numContentRows)) {
            this.selectedContentRow = -1;
            return;
        }

        int visibleRows = Math.min(numContentRows, this.rows);
        int firstVisible = this.contentRowsScrolledOff;
        int lastVisible = this.contentRowsScrolledOff + visibleRows - 1;

        if (this.selectedContentRow < firstVisible || this.selectedContentRow > lastVisible) {
            return;
        }

        int visibleRow = this.selectedContentRow - firstVisible;
        this.rowWidgets[visibleRow].setFocused(true);
    }
}
