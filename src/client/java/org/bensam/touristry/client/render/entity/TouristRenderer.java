package org.bensam.touristry.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.npc.VillagerModel;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.client.renderer.entity.layers.CrossedArmsItemLayer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.state.HoldingEntityRenderState;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.resources.Identifier;
import org.bensam.touristry.Touristry;
import org.bensam.touristry.entity.TouristEntity;
import org.jspecify.annotations.NonNull;

@Environment(EnvType.CLIENT)
public class TouristRenderer extends AgeableMobRenderer<TouristEntity, VillagerRenderState, VillagerModel> {
    private static final Identifier TOURIST_BASE_SKIN = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "textures/entity/tourist.png");

    public TouristRenderer(EntityRendererProvider.Context context) {
        super(context, new VillagerModel(context.bakeLayer(ModelLayers.WANDERING_TRADER)), new VillagerModel(context.bakeLayer(ModelLayers.VILLAGER_BABY)), 0.5F); // 0.5 shadow radius
        this.addLayer(new CustomHeadLayer<>(this, context.getModelSet(), context.getPlayerSkinRenderCache()));
        this.addLayer(new CrossedArmsItemLayer<>(this));
    }

    @Override
    public @NonNull VillagerRenderState createRenderState() {
        return new VillagerRenderState();
    }

    @Override
    protected float getShadowRadius(VillagerRenderState touristRenderState) {
        float f = super.getShadowRadius(touristRenderState);
        return touristRenderState.isBaby ? f * 0.5F : f;
    }

    @Override
    public @NonNull Identifier getTextureLocation(VillagerRenderState touristRenderState) {
        return TOURIST_BASE_SKIN;
    }

    // extractRenderState(...) is where you would put things like:
    // - is arriving
    // - is depositing
    // - beacon target present
    // - custom outfit variant
    // - teleport effect progress
    // if those affect visuals.
    @Override
    public void extractRenderState(@NonNull TouristEntity touristEntity, @NonNull VillagerRenderState touristRenderState, float partialTicks) {
        super.extractRenderState(touristEntity, touristRenderState, partialTicks);
        HoldingEntityRenderState.extractHoldingEntityRenderState(touristEntity, touristRenderState, this.itemModelResolver);
    }
}
