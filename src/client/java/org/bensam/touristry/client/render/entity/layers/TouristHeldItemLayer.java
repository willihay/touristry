package org.bensam.touristry.client.render.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.VillagerLikeModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.CrossedArmsItemLayer;
import org.bensam.touristry.client.model.entity.CameraHoldingModel;
import org.bensam.touristry.client.render.entity.state.TouristRenderState;

public class TouristHeldItemLayer<S extends TouristRenderState, M extends EntityModel<S> & VillagerLikeModel<S> & CameraHoldingModel<S>>
        extends CrossedArmsItemLayer<S, M> {

    public TouristHeldItemLayer(RenderLayerParent<S, M> renderLayerParent) {
        super(renderLayerParent);
    }

    @Override
    protected void applyTranslation(S holdingEntityRenderState, PoseStack poseStack) {
        if (holdingEntityRenderState.isHoldingCamera) {
            this.getParentModel().translateToCameraHold(holdingEntityRenderState, poseStack);
            poseStack.mulPose(Axis.YP.rotation((float) Math.PI));
            poseStack.mulPose(Axis.XP.rotation(1.45F));
            poseStack.scale(1.3F, 1.3F, 1.3F);
            poseStack.translate(0.0F, 0.1F, 0.11F);
        } else {
            super.applyTranslation(holdingEntityRenderState, poseStack);
        }
    }
}
