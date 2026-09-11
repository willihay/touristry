package org.bensam.touristry.menu;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;

import java.util.function.Predicate;

public class TabbedMenuSlot<T extends Enum<T>> extends Slot {
    private final AbstractExperienceMenu<T> menu;
    private final Predicate<AbstractExperienceMenu<T>> visibleWhen;

    TabbedMenuSlot(AbstractExperienceMenu<T> menu, Container container, int containerSlotId, int x, int y, Predicate<AbstractExperienceMenu<T>> visibleWhen) {
        super(container, containerSlotId, x, y);
        this.menu = menu;
        this.visibleWhen = visibleWhen;
    }

    @Override
    public boolean isActive() {
        return this.visibleWhen.test(this.menu);
    }
}
