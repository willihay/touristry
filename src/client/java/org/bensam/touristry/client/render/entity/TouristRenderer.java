package org.bensam.touristry.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.CrossedArmsItemLayer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.state.HoldingEntityRenderState;
import net.minecraft.resources.Identifier;
import org.bensam.touristry.Touristry;
import org.bensam.touristry.client.model.entity.TouristModel;
import org.bensam.touristry.client.render.entity.state.TouristRenderState;
import org.bensam.touristry.entity.TouristEntity;
import org.jspecify.annotations.NonNull;

@Environment(EnvType.CLIENT)
public class TouristRenderer extends AgeableMobRenderer<TouristEntity, TouristRenderState, TouristModel> {
    private static final Identifier TOURIST_BASE_SKIN = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "textures/entity/tourist.png");

    public TouristRenderer(EntityRendererProvider.Context context) {
        super(context, new TouristModel(context.bakeLayer(TouristModel.LAYER)), new TouristModel(context.bakeLayer(TouristModel.BABY_LAYER)), 0.5F); // 0.5 shadow radius
        this.addLayer(new CustomHeadLayer<>(this, context.getModelSet(), context.getPlayerSkinRenderCache()));
        this.addLayer(new CrossedArmsItemLayer<>(this));
    }

    @Override
    public @NonNull TouristRenderState createRenderState() {
        return new TouristRenderState();
    }

    @Override
    protected float getShadowRadius(TouristRenderState touristRenderState) {
        float f = super.getShadowRadius(touristRenderState);
        return touristRenderState.isBaby ? f * 0.5F : f;
    }

    @Override
    public @NonNull Identifier getTextureLocation(TouristRenderState touristRenderState) {
        return TOURIST_BASE_SKIN;
    }

    @Override
    public void extractRenderState(@NonNull TouristEntity touristEntity, @NonNull TouristRenderState touristRenderState, float partialTicks) {
        super.extractRenderState(touristEntity, touristRenderState, partialTicks);
        HoldingEntityRenderState.extractHoldingEntityRenderState(touristEntity, touristRenderState, this.itemModelResolver);

        // TODO: Extract custom outfit and color variants.

        // Extract general mood.
        touristRenderState.isUnhappy = touristEntity.getUnhappyCounter() > 0;

        // Extract waving state.
        touristRenderState.isWaving = touristEntity.isWaving() && !touristRenderState.isUnhappy && touristRenderState.heldItem.isEmpty();
    }
}
