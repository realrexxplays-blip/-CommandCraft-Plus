package com.realrexxplayz.commandcoreplus.fabric;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
public final class CommandCorePlusFabric implements ModInitializer {
 public void onInitialize() { CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> CommandCorePlusCommands.register(dispatcher)); }
}
