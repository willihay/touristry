package org.bensam.touristry.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class ModServerConfig {
    public static final int CURRENT_VERSION = 1;

    public int version = CURRENT_VERSION;

    public static final Codec<ModServerConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("version").forGetter(ModServerConfig::version)
    ).apply(instance, ModServerConfig::new));

    public ModServerConfig() {}

    public ModServerConfig(
            int version
    ) {
        this.version = version;
    }

    public int version() {
        return this.version;
    }

    public static ModServerConfig defaults() {
        return ModServerConfigDefaults.create();
    }
}
