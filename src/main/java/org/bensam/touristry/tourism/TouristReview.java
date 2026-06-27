package org.bensam.touristry.tourism;

import net.minecraft.network.chat.Component;

public record TouristReview(
        TouristLocation targetLocation,
        VisitResult result,
        boolean applyRatingToTarget,
        boolean announceToNearbyPlayers,
        Component reviewMessage,
        boolean prependTouristName,
        boolean appendTargetName
) {}
