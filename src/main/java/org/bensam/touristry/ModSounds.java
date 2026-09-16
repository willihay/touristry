package org.bensam.touristry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public class ModSounds {
    public static final SoundEvent CAMERA = register("tourist_camera");
    public static final SoundEvent CAMERA_FLASH = register("tourist_camera_flash");
    public static final SoundEvent CAMERA_CELLPHONE = register("tourist_camera_cellphone");
    public static final SoundEvent CAMERA_CELLPHONE_FLASH = register("tourist_camera_cellphone_flash");
    public static final SoundEvent CAMERA_INSTAMATIC = register("tourist_camera_instamatic");
    public static final SoundEvent CAMERA_INSTAMATIC_FLASH = register("tourist_camera_instamatic_flash");

    private ModSounds() {}

    public static void initialize() {
        Touristry.LOGGER.debug("Registering sounds");
    }

    public static SoundEvent register(String id) {
        Identifier identifier = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, id);
        return Registry.register(
                BuiltInRegistries.SOUND_EVENT,
                identifier,
                SoundEvent.createVariableRangeEvent(identifier)
        );
    }
}
