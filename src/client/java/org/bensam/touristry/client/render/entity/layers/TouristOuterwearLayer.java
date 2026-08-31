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
import org.bensam.touristry.Touristry;
import org.bensam.touristry.client.render.entity.state.TouristDataHolderRenderState;

@Environment(EnvType.CLIENT)
public class TouristOuterwearLayer<S extends LivingEntityRenderState & TouristDataHolderRenderState, M extends EntityModel<S> & VillagerLikeModel<S>>
        extends RenderLayer<S, M> {

    private static final String CLOTHING_PATH = "textures/entity/clothes/";

    public TouristOuterwearLayer(RenderLayerParent<S, M> renderLayerParent) {
        super(renderLayerParent);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int i, S entityRenderState, float f, float g) {
        if (entityRenderState.isInvisible) {
            return;
        }

        String clothingKey = entityRenderState.getClothingVariantKey();
        if (clothingKey == null || clothingKey.isEmpty()) {
            return;
        }

        Identifier clothingVariant = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, CLOTHING_PATH + clothingKey + ".png");
        renderColoredCutoutModel(this.getParentModel(), clothingVariant, poseStack, submitNodeCollector, i, entityRenderState, -1, 1);
    }
}

