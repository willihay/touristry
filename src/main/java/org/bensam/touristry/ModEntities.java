package org.bensam.touristry;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import org.bensam.touristry.entity.TouristEntity;

import java.util.function.Supplier;

public final class ModEntities {
    private static EntityType<TouristEntity> touristEntity;
    public static final Supplier<EntityType<TouristEntity>> TOURIST = () -> touristEntity;

    private ModEntities() {}

    public static void initialize() {
        ResourceKey<EntityType<?>> entityKey = ResourceKey.create(
                Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "tourist")
        );

        touristEntity = Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                entityKey,
                EntityType.Builder.<TouristEntity>of(TouristEntity::new, MobCategory.CREATURE)
                        .sized(0.6F, 1.95F)
                        .eyeHeight(1.62F)
                        .build(entityKey)
        );

        FabricDefaultAttributeRegistry.register(touristEntity, TouristEntity.createTouristAttributes());
    }
}
