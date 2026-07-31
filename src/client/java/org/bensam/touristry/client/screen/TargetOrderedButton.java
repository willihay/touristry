package org.bensam.touristry.client.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.bensam.touristry.Touristry;

@Environment(EnvType.CLIENT)
public class TargetOrderedButton extends Button {
    private static final int BUTTON_WIDTH = 76;
    private static final int BUTTON_HEIGHT = 18;
    private static final Component BUTTON_LABEL_ORDERED = Component.translatable("screen." + Touristry.MOD_ID + ".tourist_block.targets.ordered_button.label.ordered");
    private static final Component BUTTON_LABEL_RANDOMIZED = Component.translatable("screen." + Touristry.MOD_ID + ".tourist_block.targets.ordered_button.label.randomized");
    private static final Component BUTTON_TOOLTIP_ORDERED = Component.translatable("screen." + Touristry.MOD_ID + ".tourist_block.targets.ordered_button.tooltip.ordered");
    private static final Component BUTTON_TOOLTIP_RANDOMIZED = Component.translatable("screen." + Touristry.MOD_ID + ".tourist_block.targets.ordered_button.tooltip.randomized");

    private final Font font;
    private boolean isOrdered;

    public TargetOrderedButton(boolean isOrdered, int x, int y, OnPress onPress) {
        super(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, CommonComponents.EMPTY, onPress, DEFAULT_NARRATION);
        this.font = Minecraft.getInstance().font;
        this.setOrdered(isOrdered);
    }

    public void setOrdered(boolean isOrdered) {
        this.isOrdered = isOrdered;
        this.refreshTooltip();
    }

    private void refreshTooltip() {
        Component message = this.isOrdered ? BUTTON_TOOLTIP_ORDERED : BUTTON_TOOLTIP_RANDOMIZED;
        this.setTooltip(Tooltip.create(message));
    }

    @Override
    public void onPress(InputWithModifiers inputWithModifiers) {
        super.onPress(inputWithModifiers);
        this.setFocused(false);
    }

    @Override
    protected void renderContents(GuiGraphics guiGraphics, int i, int j, float f) {
        this.renderDefaultSprite(guiGraphics);

        Component label = this.isOrdered ? BUTTON_LABEL_ORDERED : BUTTON_LABEL_RANDOMIZED;
        int labelWidth = this.font.width(label);
        guiGraphics.drawString(
                this.font,
                label,
                this.getX() + ((BUTTON_WIDTH - labelWidth) / 2),
                this.getY() + 5,
                0xFFFFFFFF
        );
    }
}
