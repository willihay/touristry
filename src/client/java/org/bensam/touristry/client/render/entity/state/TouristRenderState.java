package org.bensam.touristry.client.render.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.state.HoldingEntityRenderState;

@Environment(EnvType.CLIENT)
public class TouristRenderState extends HoldingEntityRenderState {
    public boolean isUnhappy;
    public boolean isWaving;
}
