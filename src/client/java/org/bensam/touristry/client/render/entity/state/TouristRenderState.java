package org.bensam.touristry.client.render.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.state.HoldingEntityRenderState;

@Environment(EnvType.CLIENT)
public class TouristRenderState extends HoldingEntityRenderState {
    public int baseModelVariant;
    public boolean isUnhappy;
    public boolean isWaving;

    public TouristRenderState() {
        super();
        this.baseModelVariant = 1;
    }
}
