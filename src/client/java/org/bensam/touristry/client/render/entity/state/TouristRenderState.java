package org.bensam.touristry.client.render.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.state.HoldingEntityRenderState;

@Environment(EnvType.CLIENT)
public class TouristRenderState extends HoldingEntityRenderState implements TouristDataHolderRenderState {
    public int baseModelVariant;
    public int clothingVariantIndex;
    public boolean isUnhappy;
    public boolean isWaving;

    public TouristRenderState() {
        super();
        this.baseModelVariant = 1;
    }

    @Override
    public int getClothingVariantIndex() {
        return this.clothingVariantIndex;
    }
}
