package org.bensam.touristry.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import org.bensam.touristry.Touristry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Client resource reload listener that gathers clothing textures from the resource manager.
 * Registered with net.fabricmc.fabric.api.resource.v1.ResourceLoader (see TouristryClient.onInitializeClient()),
 * so it always runs with a fully populated ResourceManager rather than being called eagerly at mod init
 * (Minecraft.getInstance().getResourceManager() is not yet available at that point).
 */
@Environment(EnvType.CLIENT)
public final class ModResources implements PreparableReloadListener {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "clothing_textures");
    private static final String CLOTHING_FOLDER = "textures/entity/clothes";

    public static List<Identifier> CLOTHING_TEXTURES = new ArrayList<>();

    ModResources() {}

    @Override
    public CompletableFuture<Void> reload(PreparableReloadListener.SharedState sharedState, Executor prepareExecutor,
                                           PreparableReloadListener.PreparationBarrier preparationBarrier, Executor applyExecutor) {
        ResourceManager resourceManager = sharedState.resourceManager();
        return CompletableFuture.supplyAsync(() -> prepare(resourceManager, Profiler.get()), prepareExecutor)
                .thenCompose(preparationBarrier::wait)
                .thenAcceptAsync(textures -> apply(textures, Profiler.get()), applyExecutor);
    }

    private static List<Identifier> prepare(ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        Map<Identifier, Resource> resourceMap = resourceManager.listResources(CLOTHING_FOLDER, path -> path.getPath().endsWith(".png"));
        return new ArrayList<>(resourceMap.keySet());
    }

    private static void apply(List<Identifier> textures, ProfilerFiller profilerFiller) {
        CLOTHING_TEXTURES.clear();
        CLOTHING_TEXTURES.addAll(textures);
    }
}
