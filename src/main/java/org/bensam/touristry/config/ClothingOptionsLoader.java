package org.bensam.touristry.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.bensam.touristry.Touristry;

import java.util.List;
import java.util.Map;
import net.minecraft.util.RandomSource;

/**
 * Loads data\touristry\clothing\clothing_options.json (a single, non-registry data file bundled with the mod)
 * using the modern Codec-based SimpleJsonResourceReloadListener. Reloads whenever datapacks/resources reload.
 *
 * <p>Exposes an ordered list of stable clothing option keys (e.g. "sightseer2"). Each key corresponds directly to
 * a texture file named "&lt;key&gt;.png" under textures/entity/clothes on the client.</p>
 */
public class ClothingOptionsLoader extends SimpleJsonResourceReloadListener<ClothingOptionsLoader.ClothingData> {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "clothing_options");
    public static final String DEFAULT_KEY = "sightseer";

    public static List<String> CLOTHING_KEYS = List.of(DEFAULT_KEY);

    public ClothingOptionsLoader() {
        super(ClothingData.CODEC, FileToIdConverter.json("clothing"));
    }

    @Override
    protected void apply(Map<Identifier, ClothingData> data, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        ClothingData clothingData = data.get(ID);

        if (clothingData != null && !clothingData.keys().isEmpty()) {
            CLOTHING_KEYS = List.copyOf(clothingData.keys());
        } else {
            Touristry.LOGGER.warn("[ClothingOptionsLoader] {} not found (or empty) under data/{}/clothing", ID, Touristry.MOD_ID);
            CLOTHING_KEYS = List.of(DEFAULT_KEY);
        }
    }

    /** Picks a random stable clothing key from the currently loaded options. */
    public static String randomKey(RandomSource random) {
        List<String> keys = CLOTHING_KEYS;
        return keys.get(random.nextInt(keys.size()));
    }

    public record ClothingData(List<String> keys) {
        public static final Codec<ClothingData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.listOf().fieldOf("keys").forGetter(ClothingData::keys)
        ).apply(instance, ClothingData::new));
    }
}
