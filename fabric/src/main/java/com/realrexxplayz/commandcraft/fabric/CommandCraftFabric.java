package com.realrexxplayz.commandcraft.fabric;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
public final class CommandCraftFabric implements ModInitializer {
 public void onInitialize() {
  CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> CommandCraftCommands.register(dispatcher));
 }
}
