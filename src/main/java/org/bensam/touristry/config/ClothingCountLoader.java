package org.bensam.touristry.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.bensam.touristry.Touristry;

import java.util.Map;

/**
 * Loads data\touristry\clothing\clothing_count.json (a single, non-registry data file bundled with the mod)
 * using the modern Codec-based SimpleJsonResourceReloadListener. Reloads whenever datapacks/resources reload.
 */
public class ClothingCountLoader extends SimpleJsonResourceReloadListener<ClothingCountLoader.ClothingData> {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "clothing_count");

    public static int CLOTHING_COUNT = 1;

    public ClothingCountLoader() {
        super(ClothingData.CODEC, FileToIdConverter.json("clothing"));
    }

    @Override
    protected void apply(Map<Identifier, ClothingData> data, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        ClothingData clothingData = data.get(ID);

        if (clothingData != null) {
            CLOTHING_COUNT = clothingData.count();
        } else {
            Touristry.LOGGER.warn("[ClothingCountLoader] {} not found under data/{}/clothing", ID, Touristry.MOD_ID);
        }
    }

    public record ClothingData(int count) {
        public static final Codec<ClothingData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("count").forGetter(ClothingData::count)
        ).apply(instance, ClothingData::new));
    }
}
