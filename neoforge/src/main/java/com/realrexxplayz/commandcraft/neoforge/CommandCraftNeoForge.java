package com.realrexxplayz.commandcraft.neoforge;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;
@Mod("commandcraft")
public final class CommandCraftNeoForge {
 public CommandCraftNeoForge(IEventBus modBus) { NeoForge.EVENT_BUS.addListener(this::registerCommands); }
 private void registerCommands(RegisterCommandsEvent event) { CommandCraftCommands.register(event.getDispatcher()); }
}
