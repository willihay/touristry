package org.bensam.touristry.tourism;

import net.minecraft.world.level.block.entity.LecternBlockEntity;
import org.bensam.touristry.ModAttachments;
import org.bensam.touristry.tourism.experience.SightseeingExperience;

import java.util.UUID;

public class LecternTarget {
    private LecternTarget() {}

    public static boolean registerLecternIfLinked(LecternBlockEntity lectern) {
        if (lectern.isRemoved()) {
            return false;
        }

        UUID beaconUUID = lectern.getAttached(ModAttachments.LECTERN_TOURIST_BEACON_UUID);
        if (beaconUUID == null) {
            return false;
        }

        TourismManager.registerTouristExperience(new SightseeingExperience(beaconUUID, lectern.getBlockPos()));
        return true;
    }

    public static void unregisterLectern(LecternBlockEntity lectern) {
        UUID beaconUUID = lectern.getAttached(ModAttachments.LECTERN_TOURIST_BEACON_UUID);
        if (beaconUUID == null) {
            return;
        }

        TourismManager.unregisterTouristExperience(lectern.getBlockPos(), beaconUUID);
    }
}
