package org.bensam.touristry.tourism;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public record TouristReview(
        TouristLocation targetLocation,
        VisitResult result,
        boolean applyRatingToTarget,
        boolean announceToNearbyPlayers,
        Component reviewMessage,
        boolean prependTouristName,
        boolean appendTargetName
) {
    private static final double MIN_REPUTATION = -100.0;
    private static final double MAX_REPUTATION = 100.0;

    public static double calculateNewReputation(double currentReputation, VisitResult result) {
        double positiveNormalized = Math.max(0.0, currentReputation) / MAX_REPUTATION;
        double negativeNormalized = Math.max(0.0, -currentReputation) / MAX_REPUTATION;
        double change = switch (result) {
            case ARRIVED, GOOD, GREAT ->
                    result.baseReputationDelta() * (1.0 - positiveNormalized) * (1.0 + 0.5 * negativeNormalized);
            case UNFAVORABLE, FAILED_SPAWN, LOST, CLOSED_EARLY, HURT_EN_ROUTE, HURT_ON_PREMISES, KILLED_EN_ROUTE, KILLED_ON_PREMISES ->
                    result.baseReputationDelta() * (0.75 + 0.5 * positiveNormalized);
        };

        return Mth.clamp(currentReputation + change, MIN_REPUTATION, MAX_REPUTATION);
    }
}
