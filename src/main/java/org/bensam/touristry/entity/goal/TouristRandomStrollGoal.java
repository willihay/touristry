package org.bensam.touristry.entity.goal;

import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;
import org.bensam.touristry.block.entity.TouristBeaconBlockEntity;
import org.bensam.touristry.entity.TouristEntity;
import org.bensam.touristry.tourism.TourismManager;
import org.jspecify.annotations.Nullable;

public class TouristRandomStrollGoal extends RandomStrollGoal {

    private final TouristEntity tourist;

    public TouristRandomStrollGoal(TouristEntity tourist, double speedModifier) {
        super(tourist, speedModifier);
        this.tourist = tourist;
    }

    @Override
    public boolean canUse() {
        return super.canUse() && this.tourist.isWandering();
    }

    @Override
    public boolean canContinueToUse() {
        return super.canContinueToUse() && this.tourist.isWandering();
    }

    @Override
    public void start() {
        super.start();

        if (this.tourist.level().isClientSide()) return;

        if (this.tourist.isCurrentActivityAtBeacon()) {
            String beaconName = "unknown beacon";
            TouristBeaconBlockEntity beaconBlockEntity = TourismManager.getBeaconBlockEntity(this.tourist.level(), this.tourist.getBeaconTarget());
            if (beaconBlockEntity != null) {
                beaconName = beaconBlockEntity.getPlainTextName();
            }
            TouristEntity.logActivity("Starting TouristRandomStrollGoal to wander around " + beaconName);
        } else {
            TouristEntity.logActivity("Starting TouristRandomStrollGoal to wander in the world");
        }
    }

    @Nullable
    @Override
    protected Vec3 getPosition() {
        if (this.mob.isInWater()) {
            Vec3 vec3 = LandRandomPos.getPos(this.mob, 15, 7);
            return vec3 == null ? super.getPosition() : vec3;
        } else {
            return this.tourist.avoidWater() || (this.mob.getRandom().nextFloat() >= 0.5f) ? LandRandomPos.getPos(this.mob, 10, 7) : super.getPosition();
        }
    }
}
