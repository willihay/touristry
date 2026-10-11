package org.bensam.touristry.entity.goal;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.animal.Animal;
import org.bensam.touristry.config.Verbosity;
import org.bensam.touristry.entity.TouristEntity;

public class TouristLookAtEntityGoal extends LookAtPlayerGoal {
    private static final double WAVING_FOV = 1.0 - Math.cos(Math.toRadians(30));

    private final boolean canUseCamera;
    private final TouristEntity tourist;

    private boolean canWaveAtEntity;
    private int startUsingCameraTicks;
    private int stopUsingCameraTicks;
    private int takePictureTicks;
    private int tickCount;
    private boolean willUseCamera;

    public TouristLookAtEntityGoal(TouristEntity tourist, Class<? extends LivingEntity> class_, boolean canUseCamera, float lookDistance, float probability) {
        super(tourist, class_, lookDistance, probability);
        this.canUseCamera = canUseCamera;
        this.tourist = tourist;
    }

    @Override
    public void start() {
        super.start();
        this.canWaveAtEntity = false;
        this.tickCount = 0;
        this.willUseCamera = false;

        if (this.lookAt != null) {
            this.canWaveAtEntity = TouristEntity.wouldWaveAt(this.lookAt) && !this.tourist.isEating();
            this.willUseCamera = this.canUseCamera && !this.tourist.isEating() && this.tourist.getRandom().nextDouble() < 0.2;
            this.startUsingCameraTicks = this.adjustedTickDelay(this.tourist.getRandom().nextInt(20));
            this.takePictureTicks = this.startUsingCameraTicks + this.adjustedTickDelay(5 + this.tourist.getRandom().nextInt(10));
            this.stopUsingCameraTicks = this.startUsingCameraTicks + this.adjustedTickDelay(40);

            if (this.willUseCamera) {
                TouristEntity.logActivity(Verbosity.LEVEL_2_DIAGNOSTICS, "[{}] Taking a picture of {}",
                        this.getClass().getSimpleName(),
                        this.lookAt.getDisplayName().getString()
                );
            } else {
                TouristEntity.logActivity(Verbosity.LEVEL_2_DIAGNOSTICS, "[{}] Looking at {}",
                        this.getClass().getSimpleName(),
                        this.lookAt.getDisplayName().getString()
                );
            }
        }
    }

    @Override
    public void stop() {
        this.tourist.setCrouching(false);
        this.tourist.setUsingCamera(false);
        this.tourist.setWavingAtEntity(this.lookAt, false);

        super.stop();
    }

    @Override
    public void tick() {
        super.tick();
        this.tickCount++;

        if (this.lookAt == null) {
            return;
        }

        if (this.canWaveAtEntity && !this.tourist.isWaving() && !this.tourist.isUsingCamera() && this.lookAt instanceof LivingEntity entity) {
            if (this.tourist.isLookingAtMe(entity, WAVING_FOV, true, true, this.tourist.getEyeY()) &&
                    entity.isLookingAtMe(this.tourist, WAVING_FOV, true, true, entity.getEyeY())
            ) {
                this.tourist.setWavingAtEntity(entity, true);
            }
        }

        if (this.lookAt instanceof Animal) {
            double eyeLevelDifference = this.tourist.getEyeY() - this.lookAt.getEyeY();
            if (eyeLevelDifference >= 1.0 && !this.tourist.isCrouching() && !this.tourist.isEating()) {
                if (this.tourist.distanceToSqr(this.lookAt) <= 16.0) {
                    this.tourist.setCrouching(true);
                }
            } else if (eyeLevelDifference < 0.0 && this.tourist.isCrouching()) {
                this.tourist.setCrouching(false);
            }
        }

        if (this.willUseCamera && !this.tourist.isUsingCamera() && !this.tourist.isWaving() && !this.tourist.isEating()) {
            if (this.tickCount >= this.startUsingCameraTicks) {
                this.tourist.setUsingCamera(true);
            }
        }

        if (this.tourist.isUsingCamera() && this.tickCount == this.takePictureTicks) {
            this.tourist.takePicture();
        }

        if (this.tourist.isUsingCamera() && this.tickCount >= this.stopUsingCameraTicks) {
            this.tourist.setUsingCamera(false);
            this.willUseCamera = false;
        }
    }
}
