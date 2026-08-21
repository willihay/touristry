package org.bensam.touristry.client.render.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.VillagerLikeModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import org.bensam.touristry.client.ModResources;
import org.bensam.touristry.client.render.entity.state.TouristDataHolderRenderState;

@Environment(EnvType.CLIENT)
public class TouristOuterwearLayer<S extends LivingEntityRenderState & TouristDataHolderRenderState, M extends EntityModel<S> & VillagerLikeModel<S>>
        extends RenderLayer<S, M> {

    public TouristOuterwearLayer(RenderLayerParent<S, M> renderLayerParent) {
        super(renderLayerParent);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int i, S entityRenderState, float f, float g) {
        if (entityRenderState.isInvisible) {
            return;
        }

        int clothingIndex = entityRenderState.getClothingVariantIndex();
        if (clothingIndex >= 0 && clothingIndex < ModResources.CLOTHING_TEXTURES.size()) {
            M model = this.getParentModel();
            Identifier clothingVariant = ModResources.CLOTHING_TEXTURES.get(clothingIndex);
            renderColoredCutoutModel(model, clothingVariant, poseStack, submitNodeCollector, i, entityRenderState, -1, 1);
        }
    }
}
