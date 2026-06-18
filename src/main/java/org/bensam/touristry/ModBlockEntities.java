package org.bensam.touristry;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.bensam.touristry.block.entity.SightseeingExperienceBlockEntity;
import org.bensam.touristry.block.entity.TouristBeaconBlockEntity;

import java.util.function.Supplier;

public final class ModBlockEntities {
    private ModBlockEntities() {}

    private static BlockEntityType<TouristBeaconBlockEntity> touristBeacon;
    public static final Supplier<BlockEntityType<TouristBeaconBlockEntity>> TOURIST_BEACON = () -> touristBeacon;

    private static BlockEntityType<SightseeingExperienceBlockEntity> sightseeingExperience;
    public static final Supplier<BlockEntityType<SightseeingExperienceBlockEntity>> SIGHTSEEING_EXPERIENCE = () -> sightseeingExperience;

    public static void initialize() {
        touristBeacon = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "tourist_beacon"),
                FabricBlockEntityTypeBuilder.create(
                        TouristBeaconBlockEntity::new,
                        ModBlocks.TOURIST_BEACON.get()
                ).build()
        );

        sightseeingExperience = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "sightseeing_experience"),
                FabricBlockEntityTypeBuilder.create(
                        SightseeingExperienceBlockEntity::new,
                        ModBlocks.SIGHTSEEING_EXPERIENCE.get()
                ).build()
        );
    }
}
