package com.realrexxplayz.commandcraft.fabric;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import com.realrexxplayz.commandcraft.common.CommandCraftCore;
public final class CommandCraftCommands {
    private CommandCraftCommands() {}
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        CommandCraftCore.register(dispatcher);
    }
}
