package org.bensam.touristry.tourism;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.bensam.touristry.ModComponents;
import org.bensam.touristry.item.ExperienceTargetKeyItem;
import org.bensam.touristry.network.SyncTargetOverlayViewS2CPayload;
import org.bensam.touristry.tourism.experience.TargetOverlayView;
import org.bensam.touristry.tourism.experience.TouristExperience;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ExperienceTargetOverlaySyncManager {
    private static final UUID UNLINKED_KEY_UUID = new UUID(0, 0);
    private static final int RESYNC_INTERVAL_TICKS = 20;
    private static final Map<UUID, PlayerOverlayState> playerOverlayStates = new HashMap<>();
    private static int ticksSinceResync;

    private static final class PlayerOverlayState {
        private final Set<UUID> heldKeyUUIDs = new HashSet<>(2);
        private final Map<UUID, List<TargetOverlayView>> lastSentTargets = new HashMap<>();
    }

    private ExperienceTargetOverlaySyncManager() {}

    public static void initialize() {
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                playerOverlayStates.remove(handler.player.getUUID()));
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> clear());
    }

    public static void clear() {
        playerOverlayStates.clear();
        ticksSinceResync = 0;
    }

    public static void refreshPlayersHolding(ServerLevel serverLevel, UUID experienceUUID) {
        for (ServerPlayer player : serverLevel.getServer().getPlayerList().getPlayers()) {
            PlayerOverlayState state = playerOverlayStates.get(player.getUUID());
            if (player.level() == serverLevel && state != null && state.heldKeyUUIDs.contains(experienceUUID)) {
                sendOverlay(player, serverLevel, experienceUUID, state, true);
            }
        }
    }

    public static void tick(ServerLevel serverLevel) {
        // Track when it's time for a periodic resync.
        boolean resync = ++ticksSinceResync >= RESYNC_INTERVAL_TICKS;
        if (resync) {
            ticksSinceResync = 0;
        }

        // Iterate over all players currently connected to the server.
        for (ServerPlayer player : serverLevel.getServer().getPlayerList().getPlayers()) {
            // Spectators should not receive target overlay information.
            if (player.isSpectator() || player.level() != serverLevel) {
                playerOverlayStates.remove(player.getUUID());
                continue;
            }

            // Retrieve (or create) the overlay state object that tracks what overlays this player has.
            PlayerOverlayState state = playerOverlayStates.computeIfAbsent(
                    player.getUUID(),
                    ignored -> new PlayerOverlayState()
            );

            // Determine which target keys the player is currently holding.
            Set<UUID> heldKeyUUIDs = getHeldKeyUUIDs(player);

            // Compute which target keys are *newly* held this tick.
            // (newlyHeld = currentHeld minus previouslyHeld)
            Set<UUID> newlyHeldExperienceUUIDs = new HashSet<>(heldKeyUUIDs);
            newlyHeldExperienceUUIDs.removeAll(state.heldKeyUUIDs);

            // Update the player's overlay state to reflect what they are holding now.
            // 1. Remove any target keys that are no longer held.
            state.heldKeyUUIDs.retainAll(heldKeyUUIDs);
            // 2. Also remove stale entries from lastSentTargets for target keys no longer held.
            state.lastSentTargets.keySet().retainAll(heldKeyUUIDs);
            // 3. Get any newly held target keys into the set.
            state.heldKeyUUIDs.addAll(heldKeyUUIDs);

            // For each newly held target key, send an overlay immediately.
            // Force sync for these newly held keys.
            for (UUID experienceUUID : newlyHeldExperienceUUIDs) {
                sendOverlay(player, serverLevel, experienceUUID, state, true);
            }

            // If it's time for a periodic resync, resend overlays for *all* held target keys.
            // The 'false' flag indicates this is a normal resync, not a newly-held event.
            if (resync) {
                for (UUID experienceUUID : state.heldKeyUUIDs) {
                    sendOverlay(player, serverLevel, experienceUUID, state, false);
                }
            }
        }
    }

    private static Set<UUID> getHeldKeyUUIDs(ServerPlayer player) {
        Set<UUID> experienceUUIDs = new HashSet<>(2);
        addLinkedExperienceUUID(player.getMainHandItem(), experienceUUIDs);
        addLinkedExperienceUUID(player.getOffhandItem(), experienceUUIDs);
        return experienceUUIDs;
    }

    private static void addLinkedExperienceUUID(ItemStack itemStack, Set<UUID> experienceUUIDs) {
        if (itemStack.getItem() instanceof ExperienceTargetKeyItem) {
            UUID experienceUUID = itemStack.getOrDefault(ModComponents.TOURIST_EXPERIENCE_KEY_UUID, UNLINKED_KEY_UUID);
            if (!UNLINKED_KEY_UUID.equals(experienceUUID)) {
                experienceUUIDs.add(experienceUUID);
            }
        }
    }

    private static void sendOverlay(
            ServerPlayer player,
            ServerLevel serverLevel,
            UUID experienceUUID,
            PlayerOverlayState state,
            boolean force
    ) {
        TouristExperience experience = TourismManager.getTouristExperienceById(experienceUUID);
        if (experience == null) {
            return;
        }

        List<TargetOverlayView> targets = List.copyOf(experience.getTargetOverlayViews(serverLevel));
        if (force || !targets.equals(state.lastSentTargets.get(experienceUUID))) {
            ServerPlayNetworking.send(player, new SyncTargetOverlayViewS2CPayload(experienceUUID, targets));
            state.lastSentTargets.put(experienceUUID, targets);
        }
    }
}
