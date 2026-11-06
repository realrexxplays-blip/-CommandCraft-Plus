package com.realrexxplayz.commandcoreplus.neoforge;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;
@Mod("commandcoreplus")
public final class CommandCorePlusNeoForge {
 public CommandCorePlusNeoForge(IEventBus modBus) { NeoForge.EVENT_BUS.addListener(this::registerCommands); }
 private void registerCommands(RegisterCommandsEvent event) { CommandCorePlusCommands.register(event.getDispatcher()); }
}
