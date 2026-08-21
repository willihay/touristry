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
import org.bensam.touristry.client.render.entity.layers.TouristOuterwearLayer;
import org.bensam.touristry.client.render.entity.state.TouristRenderState;
import org.bensam.touristry.entity.TouristEntity;
import org.jspecify.annotations.NonNull;

import java.util.*;

@Environment(EnvType.CLIENT)
public class TouristRenderer extends AgeableMobRenderer<TouristEntity, TouristRenderState, TouristModel> {
    private static final String TOURIST_BASE_PATH = "textures/entity/base/base_";
    private Map<UUID, Variant> entityVariants = new HashMap<>();

    private record Variant(int base, String clothing) {}

    public TouristRenderer(EntityRendererProvider.Context context) {
        super(context, new TouristModel(context.bakeLayer(TouristModel.LAYER)), new TouristModel(context.bakeLayer(TouristModel.BABY_LAYER)), 0.5F); // 0.5 shadow radius
        this.addLayer(new CustomHeadLayer<>(this, context.getModelSet(), context.getPlayerSkinRenderCache()));
        this.addLayer(new CrossedArmsItemLayer<>(this));
        this.addLayer(new TouristOuterwearLayer<>(this));
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
        return Identifier.fromNamespaceAndPath(Touristry.MOD_ID, TOURIST_BASE_PATH + touristRenderState.baseModelVariant + ".png");
    }

    @Override
    public void extractRenderState(@NonNull TouristEntity touristEntity, @NonNull TouristRenderState touristRenderState, float partialTicks) {
        super.extractRenderState(touristEntity, touristRenderState, partialTicks);
        HoldingEntityRenderState.extractHoldingEntityRenderState(touristEntity, touristRenderState, this.itemModelResolver);

        // Extract custom outfit and color variants.
        touristRenderState.baseModelVariant = touristEntity.getBaseModelVariant();
        touristRenderState.clothingVariantKey = touristEntity.getClothingVariant();

        /*
        if (!entityVariants.containsKey(touristEntity.getUUID())) {
            Touristry.LOGGER.info("[DEBUG-VARIANT] Rendering tourist {} base={} clothing={}", touristEntity.getUUID(), touristRenderState.baseModelVariant, touristRenderState.clothingVariantKey);
            entityVariants.put(touristEntity.getUUID(), new Variant(touristRenderState.baseModelVariant, touristRenderState.clothingVariantKey));
        } else {
            Variant variants = entityVariants.get(touristEntity.getUUID());
            if (variants.base() != touristRenderState.baseModelVariant || !variants.clothing().equals(touristRenderState.clothingVariantKey)) {
                Touristry.LOGGER.info("[DEBUG-VARIANT] Rendering update for tourist {} base={} clothing={}", touristEntity.getUUID(), touristRenderState.baseModelVariant, touristRenderState.clothingVariantKey);
                entityVariants.put(touristEntity.getUUID(), new Variant(touristRenderState.baseModelVariant, touristRenderState.clothingVariantKey));
            }
        }
         */

        // Extract general mood.
        touristRenderState.isUnhappy = touristEntity.getUnhappyCounter() > 0;

        // Extract waving state.
        touristRenderState.isWaving = touristEntity.isWaving() && !touristRenderState.isUnhappy && touristRenderState.heldItem.isEmpty();
    }
}
