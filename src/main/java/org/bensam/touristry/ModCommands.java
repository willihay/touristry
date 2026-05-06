package org.bensam.touristry;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.bensam.touristry.command.ConfigCommand;

public class ModCommands {
    private ModCommands() {}

    public static void initialize() {
        CommandRegistrationCallback.EVENT.register(ConfigCommand::new);
    }
}
