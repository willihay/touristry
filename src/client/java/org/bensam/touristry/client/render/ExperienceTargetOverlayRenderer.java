package org.bensam.touristry.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.List;

@Environment(EnvType.CLIENT)
public final class ExperienceTargetOverlayRenderer {
    private static final List<BlockPos> TEST_POSITIONS = List.of(
            new BlockPos(-33, 69, -129),
            new BlockPos(-32, 70, -123),
            new BlockPos(-30, 69, -123),
            new BlockPos(-29, 70, -123)
    );
    private static final float TEXT_SCALE = 0.025F;
    private static final double VERTICAL_OFFSET = 0.9D;
    private static final double MAX_RENDER_DISTANCE = 48.0D;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int BACKGROUND_COLOR = 0x40000000;
    private static final int FULL_BRIGHT_LIGHT = 0xF000F0;

    private ExperienceTargetOverlayRenderer() {}

    public static void initialize() {
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            PoseStack poseStack = context.matrices();
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null) {
                return;
            }

            CameraRenderState cameraRS = context.worldState().cameraRenderState;
            Vec3 cameraPosition = cameraRS.pos;
            Quaternionf cameraRotation = cameraRS.orientation;
            for (int index = 0; index < TEST_POSITIONS.size(); index++) {
                BlockPos blockPos = TEST_POSITIONS.get(index);
                Vec3 labelPosition = Vec3.atCenterOf(blockPos).add(0.0D, VERTICAL_OFFSET, 0.0D);
                if (minecraft.player.distanceToSqr(labelPosition) > MAX_RENDER_DISTANCE * MAX_RENDER_DISTANCE) {
                    continue;
                }

                renderNumber(poseStack, context, cameraPosition, cameraRotation, labelPosition, Integer.toString(index + 1));
            }
        });
    }

    private static void renderNumber(
            PoseStack poseStack,
            WorldRenderContext context,
            Vec3 cameraPosition,
            Quaternionf cameraRotation,
            Vec3 labelPosition,
            String number
    ) {
        Font font = Minecraft.getInstance().font;
        float textX = -font.width(number) / 2.0F;

        poseStack.pushPose();

        poseStack.translate(
                labelPosition.x - cameraPosition.x,
                labelPosition.y - cameraPosition.y,
                labelPosition.z - cameraPosition.z
        );

        // Billboard it.
        poseStack.mulPose(cameraRotation);

        // Vanilla in-world text renderers use positive X, negative Y, positive Z for billboarded text.
        poseStack.scale(TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);

        Matrix4f matrix = poseStack.last().pose();
        font.drawInBatch(
                number,
                textX,
                0.0F,
                TEXT_COLOR,
                true, // font shadow
                matrix,
                context.consumers(),
                Font.DisplayMode.NORMAL,
                BACKGROUND_COLOR,
                FULL_BRIGHT_LIGHT
        );

        poseStack.popPose();
    }
}
