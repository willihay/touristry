package org.bensam.touristry.client.model.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

@Environment(EnvType.CLIENT)
public interface CameraHoldingModel<S extends EntityRenderState> {
    void translateToCameraHold(S entityRenderState, PoseStack poseStack);
}
