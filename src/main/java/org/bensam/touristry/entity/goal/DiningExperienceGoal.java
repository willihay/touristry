package org.bensam.touristry.entity.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.bensam.touristry.ModSounds;
import org.bensam.touristry.block.entity.AbstractExperienceBlockEntity;
import org.bensam.touristry.block.entity.DiningExperienceBlockEntity;
import org.bensam.touristry.config.Verbosity;
import org.bensam.touristry.entity.TouristEntity;
import org.bensam.touristry.entity.TouristItemInterest;
import org.bensam.touristry.tourism.TourismManager;
import org.bensam.touristry.tourism.TouristEconomy;
import org.bensam.touristry.tourism.VisitResult;
import org.bensam.touristry.tourism.experience.ExperienceVisit;
import org.bensam.touristry.tourism.experience.ItemPrice;

import java.util.*;

public class DiningExperienceGoal extends LookAtTargetPosGoal {
    private static final boolean ALLOW_MULTIPLE_PURCHASE_OF_SAME_MENU_ITEM = true;

    private final TouristEntity tourist;
    private final BlockPos diningExperiencePos;
    private final BlockPos targetPos;
    private final int durationAtTarget;
    private final int adjustedTimeAtTarget;
    private final boolean isOrderingHere;
    private final boolean isPayingHere;
    private int tickCount;

    public DiningExperienceGoal(TouristEntity tourist, BlockPos diningExperiencePos, BlockPos targetPos, int startingTickCount, int timeAtTarget, boolean isOrderingHere, boolean isPayingHere) {
        super(tourist, targetPos, true, false);
        this.tourist = tourist;
        this.diningExperiencePos = diningExperiencePos;
        this.targetPos = targetPos;
        this.tickCount = this.adjustedTickDelay(startingTickCount);
        this.adjustedTimeAtTarget = this.adjustedTickDelay(timeAtTarget);
        this.durationAtTarget = Math.max(0, timeAtTarget - startingTickCount);
        this.isOrderingHere = isOrderingHere;
        this.isPayingHere = isPayingHere;
    }

    @Override
    public void start() {
        super.start();

        ExperienceVisit visit = this.tourist.getMind().getExperienceVisit();
        float allowance = 0;
        if (visit != null) {
            allowance = visit.budgetRemaining();
        }

        if (!this.isOrderingHere && !this.isPayingHere) {
            TouristEntity.logActivity(Verbosity.LEVEL_2_DIAGNOSTICS, "[{}] At target for {} ticks",
                    this.getClass().getSimpleName(),
                    this.durationAtTarget);
        } else if (this.isOrderingHere && !this.isPayingHere) {
            TouristEntity.logActivity(Verbosity.LEVEL_2_DIAGNOSTICS, "[{}] Ordering from menu with a budget of {} for {} ticks",
                    this.getClass().getSimpleName(),
                    allowance,
                    this.durationAtTarget);
        } else if (!this.isOrderingHere) {
            TouristEntity.logActivity(Verbosity.LEVEL_2_DIAGNOSTICS, "[{}] Paying for food with a budget of {} for {} ticks",
                    this.getClass().getSimpleName(),
                    allowance,
                    this.durationAtTarget);
        } else {
            TouristEntity.logActivity(Verbosity.LEVEL_2_DIAGNOSTICS, "[{}] Ordering from menu and paying for food with a budget of {} for {} ticks",
                    this.getClass().getSimpleName(),
                    allowance,
                    this.durationAtTarget);
        }
    }

    @Override
    public void tick() {
        super.tick();
        this.tickCount++;

        if (!(this.tourist.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (this.tickCount >= this.adjustedTimeAtTarget) {
            if (this.isOrderingHere) {
                // Select food from menu and update pantry inventory.
                this.orderFood(serverLevel);
            }

            if (this.isPayingHere && !this.tourist.getShoppingBag().isEmpty()) {
                // Pay for food.
                this.payForItems(serverLevel);
            }

            // Mark finished at this target.
            this.tourist.getMind().finishTargetGoal();
        }
    }

    private void payForItems(ServerLevel serverLevel) {
        boolean paymentCompleted = false;
        if (serverLevel.getBlockEntity(this.targetPos) instanceof AbstractExperienceBlockEntity experienceBlockEntity) {
            for (ItemPrice purchase : this.tourist.getShoppingBag()) {
                if (experienceBlockEntity.tryDepositPayment(purchase.cost())) {
                    TourismManager.recordTouristPurchase(purchase);
                    float itemValue = (int) TouristEconomy.getEmeraldEquivalent(purchase.cost());
                    this.tourist.getMind().spendBudget(itemValue);
                    this.tourist.removeFromShoppingBag(purchase);
                    paymentCompleted = true;
                } else {
                    paymentCompleted = false;
                    break;
                }
            }
        }

        if (paymentCompleted) {
            serverLevel.playSound(null, this.targetPos, ModSounds.CASH_REGISTER, SoundSource.NEUTRAL);
            this.tourist.clearShoppingBag();
            this.tourist.getMind().updateExperienceVisitResult(VisitResult.GOOD);
        } else {
            this.tourist.dropAll();
            this.tourist.getMind().updateExperienceVisitResult(VisitResult.PAYMENT_FAILED);
        }
    }

    private void orderFood(ServerLevel serverLevel) {
        ExperienceVisit visit = this.tourist.getMind().getExperienceVisit();
        if (visit == null || !(serverLevel.getBlockEntity(this.diningExperiencePos) instanceof DiningExperienceBlockEntity diningExperienceBlockEntity)) {
            return;
        }

        float allowance = visit.budgetRemaining();
        boolean hasReacted = false;

        // Gather all food items in all pantry containers.
        List<ItemStack> itemsInPantry = diningExperienceBlockEntity.getContentsOfAllTargetContainers(serverLevel, TouristItemInterest.FOOD);
        if (itemsInPantry.isEmpty()) {
            return;
        }

        // Fetch item prices and their quantity available in the container.
        HashMap<ItemPrice, Integer> itemPrices = new HashMap<>();
        for (ItemStack itemInContainer : itemsInPantry) {
            ItemPrice itemPrice = diningExperienceBlockEntity.getMenuPrice(itemInContainer);
            int qtyMultiple = itemPrice.itemForSale().getCount();
            int qtyAvailable = itemInContainer.getCount() / qtyMultiple;
            if (qtyAvailable > 0) {
                itemPrices.put(itemPrice, qtyAvailable);
            }
        }

        if (itemPrices.isEmpty()) {
            return;
        }

        // Build a list of menu items that the tourist wants to order.
        List<ItemPrice> order = new ArrayList<>();

        // Decide how many items to look for (i.e. how hungry are they?).
        // Most likely they will want 3 items.
        // Commonly, but less frequently, it will be 2 or 4 items.
        // Rarely, but on occasion, it will be 1 or 5 items.
        int desiredItems = Mth.clamp((int) Math.round(3 + (this.tourist.getRandom().nextGaussian() * 0.75)), 1, 5);

        for (int f = 1; f <= desiredItems; f++) {
            if (itemPrices.isEmpty()) {
                break;
            }

            // TODO: Look for items of particular interest to this tourist.

            // If no specific item of interest was found, look for something else to eat.
            int selectedIndex = this.tourist.getRandom().nextInt(itemPrices.size());
            Map.Entry<ItemPrice, Integer> selectedEntry = null;
            Iterator<Map.Entry<ItemPrice, Integer>> iterator = itemPrices.entrySet().iterator();
            for (int i = 0; i <= selectedIndex; i++) {
                selectedEntry = iterator.next();
            }
            ItemPrice itemToBuy = selectedEntry.getKey();

            if (ALLOW_MULTIPLE_PURCHASE_OF_SAME_MENU_ITEM || !order.contains(itemToBuy)) {
                TouristEntity.logActivity(Verbosity.LEVEL_2_DIAGNOSTICS, "[{}] Tourist found menu item of interest: {}",
                        this.getClass().getSimpleName(),
                        itemToBuy.itemForSale().getItem().getName().getString()
                );

                float itemValue = TouristEconomy.getEmeraldEquivalent(itemToBuy.cost());
                if (itemValue <= allowance) {
                    order.add(itemToBuy);
                    allowance -= itemValue;
                    if (selectedEntry.getValue() <= 1) {
                        // Tourist bought last of item.
                        itemPrices.remove(itemToBuy);
                    } else {
                        selectedEntry.setValue(selectedEntry.getValue() - 1);
                    }
                }
            }
        }

        // Move order items from pantry to tourist's shopping bag.
        for (ItemPrice itemPrice : order) {
            ItemStack menuItem = itemPrice.itemForSale();
            int countBuying = menuItem.getCount();
            diningExperienceBlockEntity.removeItemStackFromTargetContainers(serverLevel, menuItem);

            // Add menu item's item price to tourist's shopping bag.
            TouristEntity.logActivity(Verbosity.LEVEL_2_DIAGNOSTICS, "[{}] Adding {} {} to shopping bag",
                    this.getClass().getSimpleName(),
                    countBuying,
                    menuItem.getItem().getName().getString()
            );
            this.tourist.addToShoppingBag(itemPrice);
        }

        // Update tourist's allowance at the experience.
        this.tourist.getMind().updateExperienceVisitAllowance(allowance);
    }
}
