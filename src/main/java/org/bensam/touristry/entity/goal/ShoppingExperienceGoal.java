package org.bensam.touristry.entity.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.bensam.touristry.block.entity.AbstractExperienceBlockEntity;
import org.bensam.touristry.block.entity.ShoppingExperienceBlockEntity;
import org.bensam.touristry.entity.TouristEntity;
import org.bensam.touristry.tourism.TouristEconomy;
import org.bensam.touristry.tourism.VisitResult;
import org.bensam.touristry.tourism.experience.ExperienceVisit;
import org.bensam.touristry.tourism.experience.ItemPrice;

import java.util.*;

public class ShoppingExperienceGoal extends LookAtTargetPosGoal {

    private final TouristEntity tourist;
    private final BlockPos shoppingExperiencePos;
    private final BlockPos targetPos;
    private int tickCount;
    private final int timeAtTarget;
    private final boolean isPurchaseCounter;

    public ShoppingExperienceGoal(TouristEntity tourist, BlockPos shoppingExperiencePos, BlockPos targetPos, int startingTickCount, int timeAtTarget, boolean isPurchaseCounter) {
        super(tourist, targetPos);
        this.tourist = tourist;
        this.shoppingExperiencePos = shoppingExperiencePos;
        this.targetPos = targetPos;
        this.tickCount = startingTickCount;
        this.timeAtTarget = timeAtTarget;
        this.isPurchaseCounter = isPurchaseCounter;
    }

    @Override
    public void tick() {
        super.tick();
        this.tickCount++;

        if (!(this.tourist.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (this.tickCount >= this.timeAtTarget) {
            if (this.isPurchaseCounter) {
                // Pay for items in shopping bag.
                this.payForItems(serverLevel);
            } else {
                // Make purchase decision.
                this.makePurchaseDecision(serverLevel);
            }

            // Mark finished at this target.
            this.tourist.getMind().finishTargetGoal(serverLevel);
        }
    }

    private void payForItems(ServerLevel serverLevel) {
        if (serverLevel.getBlockEntity(this.targetPos) instanceof ShoppingExperienceBlockEntity shoppingExperienceBlockEntity) {
            boolean paymentFailed = false;

            for (ItemPrice purchase : this.tourist.getShoppingBag()) {
                if (shoppingExperienceBlockEntity.tryDepositPayment(purchase.cost())) {
                    float itemValue = (int) TouristEconomy.getEmeraldEquivalent(purchase.cost());
                    this.tourist.getMind().spendBudget(itemValue);
                } else {
                    paymentFailed = true;
                    // TODO: Decide what to do with shopping bag items that couldn't be paid for.
                }
            }

            if (paymentFailed) {
                this.tourist.getMind().updateExperienceVisitResult(VisitResult.PAYMENT_FAILED);
            } else {
                this.tourist.getMind().updateExperienceVisitResult(VisitResult.GOOD);
            }
        }

        this.tourist.clearShoppingBag();
    }

    private void makePurchaseDecision(ServerLevel serverLevel) {
        ExperienceVisit visit = this.tourist.getMind().getExperienceVisit();
        if (visit == null || !(serverLevel.getBlockEntity(this.shoppingExperiencePos) instanceof ShoppingExperienceBlockEntity shoppingExperienceBlockEntity)) {
            return;
        }

        float allowance = visit.budgetRemaining();

        // Gather all items in target container.
        List<ItemStack> itemsInContainer = new ArrayList<>();
        BlockEntity blockEntity = serverLevel.getBlockEntity(this.targetPos);
        if (blockEntity instanceof Container container) {
            itemsInContainer = AbstractExperienceBlockEntity.getTargetContainerContents(container);

            if (itemsInContainer.isEmpty()) {
                return;
            }
        }

        // Fetch item prices and their quantity available in the container.
        HashMap<ItemPrice, Integer> itemPrices = new HashMap<>();
        for (ItemStack itemInContainer : itemsInContainer) {
            ItemPrice itemPrice = shoppingExperienceBlockEntity.getItemPrice(itemInContainer);
            int qtyMultiple = itemPrice.itemForSale().getCount();
            int qtyAvailable = itemInContainer.getCount() / qtyMultiple;
            if (qtyAvailable > 0) {
                itemPrices.put(itemPrice, qtyAvailable);
            }
        }

        if (itemPrices.isEmpty()) {
            return;
        }

        // Build a shopping cart of items that the tourist wants to buy.
        List<ItemPrice> shoppingCart = new ArrayList<>();
        // TODO: Make purchase decision based on items already in shopping bag, fairness of price, and interests of tourist, not random item in list.
        int selectedIndex = this.tourist.getRandom().nextInt(itemPrices.size());
        Map.Entry<ItemPrice, Integer> selectedEntry = null;
        Iterator<Map.Entry<ItemPrice, Integer>> iterator = itemPrices.entrySet().iterator();
        for (int i = 0; i <= selectedIndex; i++) {
            selectedEntry = iterator.next();
        }
        ItemPrice itemToBuy = selectedEntry.getKey();
        int qtyAvailable = selectedEntry.getValue();
        // TODO: Use qtyAvailable to consider buying more than 1 quantity.
        float itemValue = TouristEconomy.getEmeraldEquivalent(itemToBuy.cost());
        if (itemValue <= allowance) {
            shoppingCart.add(itemToBuy); // only buying 1 quantity for now
            allowance -= itemValue;
        }

        // Move items to buy from container to tourist's shopping bag.
        for (ItemPrice purchase : shoppingCart) {
            if (((Container) blockEntity).iterator() instanceof Container.ContainerIterator it) {
                ItemStack itemBuying = purchase.itemForSale();
                int countBuying = itemBuying.getCount();

                // Find item to buy in container and reduce its quantity by purchase count.
                while (it.hasNext()) {
                    ItemStack itemInContainer = it.next();
                    if (!itemInContainer.isEmpty() && ItemStack.isSameItemSameComponents(itemInContainer, itemBuying)) {
                        int shrinkBy = Math.min(countBuying, itemInContainer.getCount());
                        itemInContainer.shrink(shrinkBy);
                        countBuying -= shrinkBy;
                        if (countBuying <= 0) {
                            break;
                        }
                    }
                }

                // Add item to buy to tourist's shopping bag.
                this.tourist.addToShoppingBag(purchase);

                // Update tourist's allowance at the experience.
                this.tourist.getMind().updateExperienceVisitAllowance(allowance);
            }
        }
    }
}
