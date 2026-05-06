package org.bensam.touristry;

public final class ModComponents {
    private ModComponents() {}

//    public static final DataComponentType<Integer> WAND_CHARGES_COMPONENT = Registry.register(
//            BuiltInRegistries.DATA_COMPONENT_TYPE,
//            Identifier.fromNamespaceAndPath(ArcaneRelics.MOD_ID, "wand_charges"),
//            DataComponentType.<Integer>builder()
//                    .persistent(Codec.INT)
//                    .build()
//    );

    public static void initialize() {
        Touristry.LOGGER.debug("Registering components");
    }
}
