package org.bensam.touristry;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import org.bensam.touristry.command.TourCommand;
import org.bensam.touristry.config.ModServerConfigManager;
import org.bensam.touristry.config.ModServerConfigSync;
import org.bensam.touristry.config.SyncedClientConfig;
import org.bensam.touristry.item.BeaconKeyItem;
import org.bensam.touristry.tourism.TourismManager;
import org.bensam.touristry.tourism.TouristExperience;
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
		ModAttachments.initialize();
		ModComponents.initialize();
		ModItems.initialize();
		ModBlocks.initialize();
		ModBlockEntities.initialize();
		ModEntities.initialize();
		ModMenus.initialize();
		ModNetworks.initialize();
		SyncedClientConfig.initialize();
		ModCreativeTab.initialize();

		ServerWorldEvents.LOAD.register((server, serverLevel) -> {
			if (serverLevel == server.overworld()) {
				ModServerConfigManager.initialize(server);
				TourismManager.initialize(serverLevel);
			}
		});
		ServerWorldEvents.UNLOAD.register((server, serverLevel) -> {
			if (serverLevel == server.overworld()) {
				TourismManager.shutdown();
			}
		});
		ModServerConfigSync.initialize();

		ServerTickEvents.START_SERVER_TICK.register(server -> {
			TourismManager.tick(server.overworld());
		});

		ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register((blockEntity, serverLevel) -> {
            if (blockEntity instanceof LecternBlockEntity lectern) {
				// Defer Tourist Experience registration to ensure all serialized data is ready, per Java doc note for BLOCK_ENTITY_LOAD.
				serverLevel.getServer().execute(() -> {
					TouristExperience.registerLecternIfLinked(lectern);
				});
            }
        });

		ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register((blockEntity, serverLevel) -> {
			if (blockEntity instanceof LecternBlockEntity lectern) {
				// TODO: Remove log output.
				LOGGER.info("***** Unloading LecternBlockEntity");
				TouristExperience.unregisterLectern(lectern);
			}
		});

		UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
			ItemStack itemStack = player.getItemInHand(hand);

			if (!(itemStack.getItem() instanceof BeaconKeyItem beaconKeyItem)) {
				return InteractionResult.PASS;
			}

			if (!(level.getBlockEntity(hitResult.getBlockPos()) instanceof LecternBlockEntity lectern)) {
				return InteractionResult.PASS;
			}

			if (!level.isClientSide()) {
				return beaconKeyItem.useOnLectern((ServerLevel) level, player, itemStack, lectern);
			}

			return InteractionResult.SUCCESS;
		});

		CommandRegistrationCallback.EVENT.register(TourCommand::register);

		LOGGER.debug("onInitialize complete");
	}
}
