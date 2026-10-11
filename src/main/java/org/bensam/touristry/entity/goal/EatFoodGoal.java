package org.bensam.touristry.entity.goal;

import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumable;
import org.bensam.touristry.config.Verbosity;
import org.bensam.touristry.entity.TouristEntity;

public class EatFoodGoal extends Goal {
    private final TouristEntity tourist;
    private ItemStack food;

    public EatFoodGoal(TouristEntity tourist) {
        this.tourist = tourist;
    }

    @Override
    public boolean canUse() {
        return this.tourist.getMind().isHungry() && !this.tourist.isUsingCamera() && !this.tourist.isWaving();
    }

    @Override
    public boolean canContinueToUse() {
        return this.tourist.isUsingItem();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        this.food = new ItemStack(Items.APPLE);
        this.tourist.giveItemToHold(this.food);
        this.tourist.setEating(true);

        Consumable consumable = this.food.get(DataComponents.CONSUMABLE);
        if (consumable != null) {
            consumable.startConsuming(this.tourist, this.food, this.tourist.getUsedItemHand());
        }

        TouristEntity.logActivity(Verbosity.LEVEL_2_DIAGNOSTICS, "[{}] Start eating {}",
                this.getClass().getSimpleName(),
                this.food.getDisplayName().getString()
        );
    }

    @Override
    public void stop() {
        this.tourist.playSound(SoundEvents.PLAYER_BURP);
        this.tourist.setEating(false);
        this.tourist.clearHeldItem();
        this.tourist.getMind().updateHunger();
    }
}
