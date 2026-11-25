package org.bensam.touristry.client.screen.tabs;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bensam.touristry.client.screen.AbstractTabbedExperienceScreen;
import org.bensam.touristry.menu.AbstractExperienceMenu;
import org.bensam.touristry.tourism.experience.TargetView;

public class BedTargetsTab<T extends Enum<T>> extends TargetsTab<T> {
    private static final int TARGET_DETAIL_LABEL_X = 125;
    private static final int TARGET_DETAIL_LABEL_LINE3_Y = 68;

    public BedTargetsTab(Component tabTitle, T tabEnum, Item tabIcon, boolean enableTargetNaming, Component targetNameEditBoxLabel, boolean showVisitOrderToggle) {
        super(tabTitle, tabEnum, tabIcon, enableTargetNaming, targetNameEditBoxLabel, showVisitOrderToggle);
    }

    private static int getOpaqueARGBColor(int rgbColor) {
        return 0xFF000000 | rgbColor;
    }

    @Override
    protected int getTargetDisplayNameColor(TargetView target) {
        return target.hasReservation() ? getOpaqueARGBColor(ChatFormatting.YELLOW.getColor()) : super.getTargetDisplayNameColor(target);
    }

    private boolean isBedOccupied(BlockPos bedPos) {
        Level level = Minecraft.getInstance().level;

        if (level != null) {
            BlockState blockState = level.getBlockState(bedPos);
            if (blockState.getBlock() instanceof BedBlock) {
                return blockState.getValue(BedBlock.OCCUPIED);
            }
        }

        return false;
    }

    @Override
    public void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY, AbstractTabbedExperienceScreen<?, ?> screen, AbstractExperienceMenu<?> menu) {
        super.renderLabels(guiGraphics, mouseX, mouseY, screen, menu);

        int numTargets = menu.getSyncedTargets().size();
        if (this.scrollBox.isValidContentRowSelected(numTargets)) {
            int targetIndex = this.scrollBox.getSelectedTargetIndex();
            TargetView targetView = menu.getSyncedTargets().get(targetIndex);

            Component reservationStatus = targetView.hasReservation()
                    ? Component.translatable("screen.touristry.tourist_block.target.bed_details.reserved")
                            .withStyle(ChatFormatting.YELLOW)
                    : Component.translatable("screen.touristry.tourist_block.target.bed_details.unreserved")
                            .withStyle(ChatFormatting.WHITE);


            Component occupiedStatus = this.isBedOccupied(targetView.pos())
                    ? Component.translatable("screen.touristry.tourist_block.target.bed_details.occupied")
                            .withStyle(ChatFormatting.GREEN)
                    : Component.translatable("screen.touristry.tourist_block.target.bed_details.vacant")
                            .withStyle(ChatFormatting.WHITE);

            guiGraphics.drawString(
                    screen.getFont(),
                    Component.translatable("screen.touristry.tourist_block.target.bed_details", reservationStatus, occupiedStatus),
                    TARGET_DETAIL_LABEL_X,
                    TARGET_DETAIL_LABEL_LINE3_Y,
                    AbstractTabbedExperienceScreen.ARGB_SCREEN_TEXT_COLOR,
                    false
            );
        }
    }


}
