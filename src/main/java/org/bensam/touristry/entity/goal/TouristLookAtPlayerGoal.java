package org.bensam.touristry.entity.goal;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import org.bensam.touristry.config.Verbosity;
import org.bensam.touristry.entity.TouristEntity;

public class TouristLookAtPlayerGoal extends LookAtPlayerGoal {

    private final TouristEntity tourist;

    public TouristLookAtPlayerGoal(TouristEntity tourist, Class<? extends LivingEntity> class_, float lookDistance, float probability) {
        super(tourist, class_, lookDistance, probability);
        this.tourist = tourist;
    }

    @Override
    public void start() {
        super.start();

        if (this.tourist.level().isClientSide()) return;

        if (this.lookAt != null) {
            TouristEntity.logActivity(Verbosity.LEVEL_2_DIAGNOSTICS, "Starting TouristLookAtPlayerGoal to look at " + this.lookAt.getDisplayName().getString());
        }
    }
}
