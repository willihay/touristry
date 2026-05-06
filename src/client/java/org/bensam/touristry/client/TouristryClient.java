package org.bensam.touristry.client;

import net.fabricmc.api.ClientModInitializer;
import org.bensam.touristry.client.config.ModClientConfigManager;
import org.bensam.touristry.client.network.ConfigClientPackets;
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

		// Register packet receivers.
		ConfigClientPackets.registerClientReceivers();
	}
}