package org.bensam.touristry.entity.goal;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;
import org.bensam.touristry.config.Verbosity;
import org.bensam.touristry.entity.TouristEntity;
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

        if (this.tourist.isAtTouristLocation()) {
            Component blockName = this.tourist.getCurrentTouristLocationNameOrPos(this.tourist.level());
            TouristEntity.logActivity(Verbosity.LEVEL_2_DIAGNOSTICS, "Starting TouristRandomStrollGoal to wander around " + blockName.getString());
        } else {
            TouristEntity.logActivity(Verbosity.LEVEL_2_DIAGNOSTICS, "Starting TouristRandomStrollGoal to wander in the world");
        }
    }

    @Nullable
    @Override
    protected Vec3 getPosition() {
        if (this.tourist.isInWater()) {
            Vec3 vec3 = LandRandomPos.getPos(this.tourist, 15, 7);
            return vec3 == null ? super.getPosition() : vec3;
        } else {
            return this.tourist.getMind().avoidWater() || (this.tourist.getRandom().nextFloat() >= 0.5f) ? LandRandomPos.getPos(this.tourist, 10, 7) : super.getPosition();
        }
    }
}
