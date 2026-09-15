package org.bensam.touristry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public class ModSounds {
    public static final SoundEvent TOURIST_TAKING_PHOTO = register("tourist_camera");

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
