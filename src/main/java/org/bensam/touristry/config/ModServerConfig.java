package org.bensam.touristry.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class ModServerConfig {
    public static final int CURRENT_VERSION = 1;

    private int version = CURRENT_VERSION;
    private TourismManagerConfig tourismManagerConfig = new TourismManagerConfig();
    private TouristEntityConfig touristEntityConfig = new TouristEntityConfig();

    public static final Codec<ModServerConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("version").forGetter(ModServerConfig::version),
            TourismManagerConfig.CODEC.fieldOf("tourismManagerConfig").forGetter(ModServerConfig::tourismManager),
            TouristEntityConfig.CODEC.fieldOf("touristEntityConfig").forGetter(ModServerConfig::touristEntity)
    ).apply(instance, ModServerConfig::new));

    public ModServerConfig() {}

    public ModServerConfig(
            int version,
            TourismManagerConfig tourismManagerConfig,
            TouristEntityConfig touristEntityConfig
    ) {
        this.version = version;
        this.tourismManagerConfig = tourismManagerConfig;
        this.touristEntityConfig = touristEntityConfig;
    }

    public static ModServerConfig defaults() {
        return ModServerConfigDefaults.create();
    }

    public int version() {
        return this.version;
    }

    public TourismManagerConfig tourismManager() {
        return this.tourismManagerConfig;
    }

    public void setTourismManagerConfig(TourismManagerConfig tourismManagerConfig) {
        this.tourismManagerConfig = tourismManagerConfig;
    }

    public TouristEntityConfig touristEntity() {
        return this.touristEntityConfig;
    }
}
