package org.bensam.touristry.config;

import java.util.function.BooleanSupplier;

public final class ConfigBridgeForClient {
    //private static Function<SomeItem, GameplayBalanceConfig> gameplayBalanceResolver; // not in use at this time
    private static BooleanSupplier verboseTooltipsSupplier = () -> true;

    private ConfigBridgeForClient() {}

    public static void initialize(
            //Function<SomeItem, GameplayBalanceConfig> resolver, // not in use at this time
            BooleanSupplier verboseTooltipsSupplier
    ) {
        //ConfigBridgeForClient.gameplayBalanceResolver = resolver;
        ConfigBridgeForClient.verboseTooltipsSupplier = verboseTooltipsSupplier;
    }

    // not in use at this time
    /*
    public static @NonNull GameplayBalanceConfig getGameplayBalanceConfig(SomeItem someItem) {
        return gameplayBalanceResolver.apply(someItem);
    }
     */

    public static boolean showVerboseTooltips() {
        return verboseTooltipsSupplier.getAsBoolean();
    }
}
