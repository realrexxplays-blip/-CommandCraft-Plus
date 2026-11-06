package com.realrexxplayz.commandcoreplus.fabric;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import com.realrexxplayz.commandcoreplus.common.CommandCorePlusCore;
public final class CommandCorePlusCommands {
    private CommandCorePlusCommands() {}
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        CommandCorePlusCore.register(dispatcher);
    }
}
