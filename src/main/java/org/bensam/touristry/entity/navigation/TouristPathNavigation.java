package org.bensam.touristry.entity.navigation;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathFinder;

public class TouristPathNavigation extends GroundPathNavigation {
    public TouristPathNavigation(Mob mob, Level level) {
        super(mob, level);
    }

    @Override
    protected PathFinder createPathFinder(int i) {
        this.nodeEvaluator = new TouristWalkNodeEvaluator();
        return new PathFinder(this.nodeEvaluator, i);
    }
}
