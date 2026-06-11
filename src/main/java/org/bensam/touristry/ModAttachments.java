package org.bensam.touristry;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public final class ModAttachments {
    private ModAttachments() {}

    public static final AttachmentType<UUID> LECTERN_TOURIST_BEACON_UUID =
            AttachmentRegistry.createPersistent(
                    Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "lectern_tourist_beacon_uuid"),
                    UUIDUtil.CODEC
            );

    public static void initialize() {
        Touristry.LOGGER.debug("Registering Fabric attachments");
    }
}
