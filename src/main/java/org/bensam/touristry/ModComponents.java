package org.bensam.touristry;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.bensam.touristry.block.entity.ItemStackKey;
import org.bensam.touristry.tourism.experience.ExperienceTarget;
import org.bensam.touristry.tourism.experience.ItemPrice;
import org.bensam.touristry.tourism.experience.TouristLocationStats;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

public final class ModComponents {
    private ModComponents() {}

    public static final DataComponentType<UUID> TOURIST_BEACON_UUID = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "tourist_beacon_uuid"),
            DataComponentType.<UUID>builder()
                    .persistent(UUIDUtil.CODEC)
                    .build()
    );

    public static final DataComponentType<TouristLocationStats> TOURIST_BEACON_STATISTICS = Registry.register(
        BuiltInRegistries.DATA_COMPONENT_TYPE,
        Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "tourist_beacon_statistics"),
        DataComponentType.<TouristLocationStats>builder()
                .persistent(TouristLocationStats.CODEC)
                .build()
    );

    public static final DataComponentType<Boolean> TOURIST_BEACON_STATUS = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "tourist_beacon_status"),
            DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .build()
    );

    public static final DataComponentType<UUID> TOURIST_EXPERIENCE_KEY_UUID = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "tourist_experience_key_uuid"),
            DataComponentType.<UUID>builder()
                    .persistent(UUIDUtil.CODEC)
                    .build()
    );

    public static final DataComponentType<UUID> TOURIST_EXPERIENCE_UUID = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "tourist_experience_uuid"),
            DataComponentType.<UUID>builder()
                    .persistent(UUIDUtil.CODEC)
                    .build()
    );

    public static final DataComponentType<Boolean> TOURIST_EXPERIENCE_ORDERED_TARGETS = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "tourist_experience_ordered_targets"),
            DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .build()
    );

    public static final DataComponentType<TouristLocationStats> TOURIST_EXPERIENCE_STATISTICS = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "tourist_experience_statistics"),
            DataComponentType.<TouristLocationStats>builder()
                    .persistent(TouristLocationStats.CODEC)
                    .build()
    );

    public static final DataComponentType<Boolean> TOURIST_EXPERIENCE_STATUS = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "tourist_experience_status"),
            DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .build()
    );

    public static final DataComponentType<List<ExperienceTarget>> TOURIST_EXPERIENCE_TARGETS = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "tourist_experience_targets"),
            DataComponentType.<List<ExperienceTarget>>builder()
                    .persistent(ExperienceTarget.CODEC.listOf())
                    .networkSynchronized(ExperienceTarget.STREAM_CODEC.apply(ByteBufCodecs.list()))
                    .build()
    );

    public static final DataComponentType<ItemStack> TOURIST_EXPERIENCE_ITEM_DEFAULT_COST = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "tourist_experience_item_default_cost"),
            DataComponentType.<ItemStack>builder()
                    .persistent(ItemStack.OPTIONAL_CODEC)
                    .build()
    );

    public static final DataComponentType<LinkedHashMap<ItemStackKey, ItemPrice>> TOURIST_EXPERIENCE_ITEM_PRICES = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "tourist_experience_item_prices"),
            DataComponentType.<LinkedHashMap<ItemStackKey, ItemPrice>>builder()
                    .persistent(ItemPrice.MAP_CODEC)
                    .build()
    );

    public static void initialize() {
        Touristry.LOGGER.debug("Registering components");
    }
}
