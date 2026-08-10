package org.bensam.touristry.entity.goal;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import org.bensam.touristry.config.Verbosity;
import org.bensam.touristry.entity.TouristEntity;

public class TouristLookAtEntityGoal extends LookAtPlayerGoal {
    private static final double WAVING_FOV = 1.0 - Math.cos(Math.toRadians(30));

    private boolean canWaveAtEntity;
    private final TouristEntity tourist;

    public TouristLookAtEntityGoal(TouristEntity tourist, Class<? extends LivingEntity> class_, float lookDistance, float probability) {
        super(tourist, class_, lookDistance, probability);
        this.tourist = tourist;
    }

    @Override
    public void start() {
        super.start();

        if (this.tourist.level().isClientSide()) return;

        if (this.lookAt != null) {
            this.canWaveAtEntity = TouristEntity.wouldWaveAt(this.lookAt);
            TouristEntity.logActivity(Verbosity.LEVEL_2_DIAGNOSTICS, "Starting TouristLookAtEntityGoal to look at " + this.lookAt.getDisplayName().getString());
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (this.canWaveAtEntity && this.lookAt != null && !this.tourist.isWaving() && this.lookAt instanceof LivingEntity entity) {
            if (this.tourist.isLookingAtMe(entity, WAVING_FOV, true, true, this.tourist.getEyeY())) {
                this.tourist.setWavingAtEntity(entity, true);
            }
        }
    }

    @Override
    public void stop() {
        super.stop();

        if (this.canWaveAtEntity) {
            this.tourist.setWavingAtEntity(this.lookAt, false);
        }
    }
}
