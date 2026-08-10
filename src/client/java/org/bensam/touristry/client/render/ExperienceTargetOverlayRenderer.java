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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.bensam.touristry.ModComponents;
import org.bensam.touristry.item.ExperienceTargetKeyItem;
import org.bensam.touristry.menu.ShoppingExperienceMenu;
import org.bensam.touristry.tourism.experience.TargetOverlayView;
import org.bensam.touristry.tourism.experience.TargetView;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.*;

@Environment(EnvType.CLIENT)
public final class ExperienceTargetOverlayRenderer {
    private static final UUID UNLINKED_KEY_UUID = new UUID(0, 0);
    private static final float TEXT_SCALE = 0.025F;
    private static final double VERTICAL_OFFSET = 0.9D;
    private static final double MAX_RENDER_DISTANCE = 48.0D;
    private static final int[] TEXT_COLORS = { 0xFFFFFFFF, 0xFF0F77FF };
    private static final int BACKGROUND_COLOR = 0x40000000;
    private static final int FULL_BRIGHT_LIGHT = 0xF000F0;
    private static final Map<UUID, List<TargetOverlayView>> targetsByExperience = new HashMap<>();

    private ExperienceTargetOverlayRenderer() {}

    public static void initialize() {
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            PoseStack poseStack = context.matrices();
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null || minecraft.level == null || minecraft.level.dimension() != Level.OVERWORLD) {
                return;
            }

            CameraRenderState cameraRS = context.worldState().cameraRenderState;
            Vec3 cameraPosition = cameraRS.pos;
            Quaternionf cameraRotation = cameraRS.orientation;

            // Draw targets for the player if they have an experience block UI open.
            if (minecraft.player.hasContainerOpen() && minecraft.player.containerMenu instanceof ShoppingExperienceMenu menu) {
                List<TargetView> syncedTargets = menu.getSyncedTargets();
                if (syncedTargets.isEmpty()) {
                    return;
                }
                for (int i = 0; i < syncedTargets.size(); i++) {
                    TargetView target = syncedTargets.get(i);
                    Vec3 labelPosition = getLabelPosition(minecraft, target);
                    if (minecraft.player.distanceToSqr(labelPosition) <= MAX_RENDER_DISTANCE * MAX_RENDER_DISTANCE) {
                        renderNumber(
                                poseStack,
                                context,
                                cameraPosition,
                                cameraRotation,
                                labelPosition,
                                Integer.toString(i + 1),
                                TEXT_COLORS[0]);
                    }
                }
                return;
            }

            // If no experience block UI is open, draw targets for the player if they are holding an experience block's target key.
            boolean isFirstUUID = true;
            for (UUID experienceUUID : getHeldKeyUUIDs(minecraft.player.getMainHandItem(), minecraft.player.getOffhandItem())) {
                int textColor = isFirstUUID ? TEXT_COLORS[0] : TEXT_COLORS[1];
                for (TargetOverlayView target : targetsByExperience.getOrDefault(experienceUUID, List.of())) {
                    Vec3 labelPosition = getLabelPosition(minecraft, target);
                    if (minecraft.player.distanceToSqr(labelPosition) <= MAX_RENDER_DISTANCE * MAX_RENDER_DISTANCE) {
                        renderNumber(
                                poseStack,
                                context,
                                cameraPosition,
                                cameraRotation,
                                labelPosition,
                                Integer.toString(target.targetNumber()),
                                textColor);
                    }
                }
                isFirstUUID = false;
            }
        });
    }

    public static void setTargets(UUID experienceUUID, List<TargetOverlayView> targets) {
        targetsByExperience.put(experienceUUID, List.copyOf(targets));
    }

    private static Set<UUID> getHeldKeyUUIDs(ItemStack mainHandItem, ItemStack offhandItem) {
        Set<UUID> experienceUUIDs = new HashSet<>(2);
        addLinkedExperienceUUID(mainHandItem, experienceUUIDs);
        addLinkedExperienceUUID(offhandItem, experienceUUIDs);
        return experienceUUIDs;
    }

    private static void addLinkedExperienceUUID(ItemStack itemStack, Set<UUID> experienceUUIDs) {
        if (itemStack.getItem() instanceof ExperienceTargetKeyItem) {
            UUID experienceUUID = itemStack.getOrDefault(ModComponents.TOURIST_EXPERIENCE_KEY_UUID, UNLINKED_KEY_UUID);
            if (!UNLINKED_KEY_UUID.equals(experienceUUID)) {
                experienceUUIDs.add(experienceUUID);
            }
        }
    }

    private static Vec3 getLabelPosition(Minecraft minecraft, TargetView target) {
        return getLabelPosition(minecraft, target.entityUUID(), target.pos(), target.alternateOverlayDisplayPos());
    }

    private static Vec3 getLabelPosition(Minecraft minecraft, TargetOverlayView target) {
        return getLabelPosition(minecraft, target.entityUUID(), target.pos(), target.alternateOverlayDisplayPos());
    }

    private static Vec3 getLabelPosition(Minecraft minecraft, UUID entityUUID, BlockPos blockPos, BlockPos alternatePos) {
        if (entityUUID != null) {
            Entity entity = minecraft.level.getEntity(entityUUID);
            if (entity != null) {
                Vec3 labelPos = entity.position().add(0.0D, entity.getBbHeight() + VERTICAL_OFFSET, 0.0D);
                return getNonOccludingPosOrAlternate(minecraft, labelPos, alternatePos);
            }
        }
        Vec3 labelPos = Vec3.atCenterOf(blockPos).add(0.0D, VERTICAL_OFFSET, 0.0D);
        return getNonOccludingPosOrAlternate(minecraft, labelPos, alternatePos);
    }

    private static Vec3 getNonOccludingPosOrAlternate(Minecraft minecraft, Vec3 labelPos, BlockPos alternatePos) {
        if (!isOccluding(minecraft, labelPos)) {
            return labelPos;
        } else {
            return Vec3.atCenterOf(alternatePos).add(0.0D, 0.5D, 0.0D);
        }
    }

    private static boolean isOccluding(Minecraft minecraft, Vec3 pos) {
        BlockState blockState = minecraft.level.getBlockState(BlockPos.containing(pos));
        return blockState.isSolidRender();
    }

    private static void renderNumber(
            PoseStack poseStack,
            WorldRenderContext context,
            Vec3 cameraPosition,
            Quaternionf cameraRotation,
            Vec3 labelPosition,
            String number,
            int textColor
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
                textColor,
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
