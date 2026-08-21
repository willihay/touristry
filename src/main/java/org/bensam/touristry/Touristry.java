package org.bensam.touristry;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.bensam.touristry.block.TouristBeaconBlock;
import org.bensam.touristry.block.TouristExperienceBlock;
import org.bensam.touristry.command.TourCommand;
import org.bensam.touristry.config.ClothingOptionsLoader;
import org.bensam.touristry.config.ModServerConfigManager;
import org.bensam.touristry.config.ModServerConfigSync;
import org.bensam.touristry.config.SyncedClientConfig;
import org.bensam.touristry.item.ExperienceTargetKeyItem;
import org.bensam.touristry.network.ExperienceServerPackets;
import org.bensam.touristry.tourism.ExperienceTargetOverlaySyncManager;
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
		ModAttachments.initialize();
		ModComponents.initialize();
		ModItems.initialize();
		ModBlocks.initialize();
		ModBlockEntities.initialize();
		ModEntities.initialize();
		ModMenus.initialize();
		ModCreativeTab.initialize();

		// Register network payloads.
		ModNetworks.initialize();

		// Register server-side player connection event handlers and network packet receiver handlers.
		SyncedClientConfig.initialize();
		ExperienceServerPackets.registerServerReceivers();
		ExperienceTargetOverlaySyncManager.initialize();

		// Register server-side resource loader.
		ResourceLoader.get(PackType.SERVER_DATA).registerReloader(ClothingOptionsLoader.ID, new ClothingOptionsLoader());

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
			ExperienceTargetOverlaySyncManager.tick(server.overworld());
		});

		/*
		ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register((blockEntity, serverLevel) -> {
            if (blockEntity instanceof LecternBlockEntity lectern) {
				// Defer Tourist Experience registration to ensure all serialized data is ready, per Java doc note for BLOCK_ENTITY_LOAD.
				serverLevel.getServer().execute(() -> {
					LecternTarget.registerLecternIfLinked(lectern);
				});
            }
        });

		ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register((blockEntity, serverLevel) -> {
			if (blockEntity instanceof LecternBlockEntity lectern) {
				LecternTarget.unregisterLectern(lectern);
			}
		});
		*/

		UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
			ItemStack itemStack = player.getItemInHand(hand);

			if (!level.isClientSide()
					&& level.dimension() != Level.OVERWORLD
					&& itemStack.getItem() instanceof BlockItem blockItem
					&& (blockItem.getBlock() instanceof TouristBeaconBlock || blockItem.getBlock() instanceof TouristExperienceBlock)) {
				player.displayClientMessage(net.minecraft.network.chat.Component.literal("Tourism blocks can only be placed in the overworld"), true);
				return InteractionResult.FAIL;
			}

			if (!(itemStack.getItem() instanceof ExperienceTargetKeyItem experienceTargetKeyItem)) {
				return InteractionResult.PASS;
			}

			if (!level.isClientSide() && !player.isSpectator()) {
				return experienceTargetKeyItem.useOnBlock((ServerLevel) level, player, itemStack, hitResult);
			}

			return InteractionResult.SUCCESS;
		});

		UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
			ItemStack itemStack = player.getItemInHand(hand);

			if (!(itemStack.getItem() instanceof ExperienceTargetKeyItem experienceTargetKeyItem)) {
				return InteractionResult.PASS;
			}

			if (!level.isClientSide() && !player.isSpectator()) {
				return experienceTargetKeyItem.useOnEntity((ServerLevel) level, player, itemStack, entity, hitResult);
			}

			return InteractionResult.SUCCESS;
		});

		CommandRegistrationCallback.EVENT.register(TourCommand::register);

		LOGGER.debug("onInitialize complete");
	}
}
