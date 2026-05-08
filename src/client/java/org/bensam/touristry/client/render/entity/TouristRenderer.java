package org.bensam.touristry.client.render.entity;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.npc.VillagerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.CrossedArmsItemLayer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.resources.Identifier;
import org.bensam.touristry.Touristry;
import org.bensam.touristry.entity.TouristEntity;
import org.jspecify.annotations.NonNull;

public class TouristRenderer extends MobRenderer<TouristEntity, VillagerRenderState, VillagerModel> {
    private static final Identifier TOURIST_BASE_SKIN = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "textures/entity/tourist.png");

    public TouristRenderer(EntityRendererProvider.Context context) {
        super(context, new VillagerModel(context.bakeLayer(ModelLayers.WANDERING_TRADER)), 0.5F); // 0.5 shadow radius
        this.addLayer(new CustomHeadLayer<>(this, context.getModelSet(), context.getPlayerSkinRenderCache()));
        this.addLayer(new CrossedArmsItemLayer<>(this));
    }

    @Override
    public @NonNull VillagerRenderState createRenderState() {
        return new VillagerRenderState();
    }

    @Override
    public @NonNull Identifier getTextureLocation(@NonNull VillagerRenderState livingEntityRenderState) {
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
    public void extractRenderState(@NonNull TouristEntity livingEntity, @NonNull VillagerRenderState livingEntityRenderState, float partialTicks) {
        super.extractRenderState(livingEntity, livingEntityRenderState, partialTicks);
    }
}
