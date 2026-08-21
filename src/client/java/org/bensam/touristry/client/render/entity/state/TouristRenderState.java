package org.bensam.touristry.client.render.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.state.HoldingEntityRenderState;

@Environment(EnvType.CLIENT)
public class TouristRenderState extends HoldingEntityRenderState implements TouristDataHolderRenderState {
    public int baseModelVariant;
    public String clothingVariantKey;
    public boolean isUnhappy;
    public boolean isWaving;

    public TouristRenderState() {
        super();
        this.baseModelVariant = 1;
    }

    @Override
    public String getClothingVariantKey() {
        return this.clothingVariantKey;
    }
}
