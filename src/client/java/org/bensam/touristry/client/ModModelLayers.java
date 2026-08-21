package org.bensam.touristry.client;

import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import org.bensam.touristry.client.model.entity.TouristModel;

public final class ModModelLayers {

    private ModModelLayers() {}

    public static void initialize() {
        EntityModelLayerRegistry.registerModelLayer(TouristModel.LAYER, TouristModel::createBodyLayer);
        EntityModelLayerRegistry.registerModelLayer(TouristModel.BABY_LAYER, TouristModel::createBabyBodyLayer);
    }
}
