package org.bensam.touristry.client.render.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Exposes Tourist-specific render state (e.g. clothing variant) to layers that need it, without forcing
 * those layers to bind to the concrete {@link TouristRenderState} type. Mirrors vanilla's
 * VillagerDataHolderRenderState pattern used by VillagerProfessionLayer.
 */
@Environment(EnvType.CLIENT)
public interface TouristDataHolderRenderState {
    int getClothingVariantIndex();
}
