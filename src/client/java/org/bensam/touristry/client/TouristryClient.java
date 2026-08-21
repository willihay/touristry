package org.bensam.touristry.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.EntityRenderers;
import org.bensam.touristry.ModEntities;
import org.bensam.touristry.ModMenus;
import org.bensam.touristry.client.config.ModClientConfigManager;
import org.bensam.touristry.client.network.ConfigClientPackets;
import org.bensam.touristry.client.network.ExperienceClientPackets;
import org.bensam.touristry.client.render.ExperienceTargetOverlayRenderer;
import org.bensam.touristry.client.render.entity.TouristRenderer;
import org.bensam.touristry.client.screen.ShoppingExperienceScreen;
import org.bensam.touristry.client.screen.SightseeingExperienceScreen;
import org.bensam.touristry.client.screen.TouristBeaconScreen;
import org.bensam.touristry.config.ConfigBridgeForClient;

public class TouristryClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Initialize client config manager.
		ModClientConfigManager.initialize();

		// Initialize bridge that provides access to both synced server config and client config in client-side logic in src/main.
		ConfigBridgeForClient.initialize(
				//someItem -> someItem.getBalanceConfig(SyncedServerConfig.get()), // not in use at this time
				() -> ModClientConfigManager.getConfig().verboseTooltips()
		);

		// Register model layers.
		ModModelLayers.initialize();

		// Register network packet receivers.
		ConfigClientPackets.registerClientReceivers();
		ExperienceClientPackets.registerClientReceivers();

		// Register screens.
		MenuScreens.register(ModMenus.TOURIST_BEACON_MENU.get(), TouristBeaconScreen::new);
		MenuScreens.register(ModMenus.SHOPPING_EXPERIENCE_MENU.get(), ShoppingExperienceScreen::new);
		MenuScreens.register(ModMenus.SIGHTSEEING_EXPERIENCE_MENU.get(), SightseeingExperienceScreen::new);

		// Register renderers.
		EntityRenderers.register(ModEntities.TOURIST.get(), TouristRenderer::new);
		ExperienceTargetOverlayRenderer.initialize();
	}
}