package org.bensam.touristry;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import org.bensam.touristry.config.ModServerConfigManager;
import org.bensam.touristry.config.ModServerConfigSync;
import org.bensam.touristry.config.SyncedClientConfig;
import org.bensam.touristry.tourism.TourismManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Touristry implements ModInitializer {
	public static final String MOD_ID = "touristry";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		LOGGER.debug("onInitialize start");

		ModAdvancements.initialize();
		ModStats.initialize();
		ModComponents.initialize();
		ModItems.initialize();
		ModBlocks.initialize();
		ModBlockEntities.initialize();
		ModEntities.initialize();
		ModMenus.initialize();
		ModNetworks.initialize();
		SyncedClientConfig.initialize();
		ModCreativeTab.initialize();

		ServerWorldEvents.LOAD.register((server, world) -> {
			if (world == server.overworld()) {
				ModServerConfigManager.initialize(server);
				TourismManager.initialize(world);
			}
		});
		ServerWorldEvents.UNLOAD.register((server, world) -> {
			if (world == server.overworld()) {
				TourismManager.shutdown();
			}
		});
		ModServerConfigSync.initialize();

		ServerTickEvents.START_SERVER_TICK.register(server -> {
			TourismManager.tick(server.overworld());
		});

		ModCommands.initialize();

		LOGGER.debug("onInitialize complete");
	}
}
