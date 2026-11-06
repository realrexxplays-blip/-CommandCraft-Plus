package com.realrexxplayz.commandcraft.fabric;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
public final class CommandCraftCommands {
 private CommandCraftCommands() {}
 public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
  dispatcher.register(Commands.literal("cc")
   .then(Commands.literal("help").executes(ctx -> { ctx.getSource().sendSuccess(() -> Component.literal("CommandCraft+ | /cc help, /cc coords, /cc heal, /cc feed"), false); return 1; }))
   .then(Commands.literal("coords").executes(ctx -> { var p=ctx.getSource().getPosition(); ctx.getSource().sendSuccess(() -> Component.literal(String.format("X: %.1f Y: %.1f Z: %.1f",p.x,p.y,p.z)),false); return 1; }))
   .then(Commands.literal("heal").requires(s -> s.hasPermission(2)).executes(ctx -> { var p=ctx.getSource().getPlayerOrException(); p.setHealth(p.getMaxHealth()); ctx.getSource().sendSuccess(() -> Component.literal("Health restored."),false); return 1; }))
   .then(Commands.literal("feed").requires(s -> s.hasPermission(2)).executes(ctx -> { var p=ctx.getSource().getPlayerOrException(); p.getFoodData().eat(20,20.0F); ctx.getSource().sendSuccess(() -> Component.literal("Food restored."),false); return 1; }))
  );
 }
}
