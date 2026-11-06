package com.realrexxplayz.commandcraft.common;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public final class CommandCraftDirectCommands {
    private CommandCraftDirectCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        String[] simple = {"sethome","home","homes","delhome","tpa","tpahere","tpaccept","tpdeny",
            "tpacancel","back","spawn","setspawn","near","day","night","weather","broadcast",
            "rules","list","fly","heal","feed","god","vanish","motd","maintenance","coords",
            "whereami","setwarp","warp","warps","delwarp","worldinfo"};
        for (String name : simple) alias(d, name, name, true);

        d.register(Commands.literal("hub").executes(c -> d.execute("cc warp hub", c.getSource())));
        d.register(Commands.literal("sethub").requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
            .executes(c -> d.execute("cc setwarp hub", c.getSource())));
        d.register(Commands.literal("rtp").executes(c -> randomTeleport(c.getSource())));
        d.register(Commands.literal("wild").executes(c -> randomTeleport(c.getSource())));

        d.register(Commands.literal("sendcoords").then(Commands.argument("player", EntityArgument.player())
            .executes(c -> {
                ServerPlayer from = c.getSource().getPlayerOrException();
                ServerPlayer to = EntityArgument.getPlayer(c, "player");
                var p = from.blockPosition();
                to.sendSystemMessage(Component.literal(from.getGameProfile().name() + " shared coordinates: " +
                    p.getX() + ", " + p.getY() + ", " + p.getZ() + " (" +
                    from.level().dimension().identifier() + ")"));
                c.getSource().sendSuccess(() -> Component.literal("Coordinates sent."), false);
                return 1;
            })));

        d.register(Commands.literal("ping").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            int ms = p.connection.getLatency();
            c.getSource().sendSuccess(() -> Component.literal("Your ping: " + ms + " ms"), false);
            return 1;
        }));
        d.register(Commands.literal("playtime").executes(c -> {
            c.getSource().sendSuccess(() -> Component.literal(
                "Playtime tracking will be available in a later update; this command is registered."), false);
            return 1;
        }));
        d.register(Commands.literal("whois").then(Commands.argument("player", EntityArgument.player())
            .executes(c -> {
                ServerPlayer p = EntityArgument.getPlayer(c, "player");
                c.getSource().sendSuccess(() -> Component.literal("Player: " + p.getGameProfile().name() +
                    " | Dimension: " + p.level().dimension().identifier() + " | Position: " +
                    p.blockPosition().getX() + ", " + p.blockPosition().getY() + ", " +
                    p.blockPosition().getZ() + " | Ping: " + p.connection.getLatency() + " ms"), false);
                return 1;
            })));
        d.register(Commands.literal("seen").then(Commands.argument("player", StringArgumentType.word())
            .executes(c -> {
                String name = StringArgumentType.getString(c, "player");
                boolean online = c.getSource().getServer().getPlayerList().getPlayers().stream()
                    .anyMatch(p -> p.getGameProfile().name().equalsIgnoreCase(name));
                c.getSource().sendSuccess(() -> Component.literal(online ?
                    name + " is online now." : name + " is offline. Last-seen history is not stored yet."), false);
                return 1;
            })));

        d.register(Commands.literal("unstuck").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            p.teleportTo(p.level(), p.getX(), p.getY() + 1.0, p.getZ(), java.util.Set.of(),
                p.getYRot(), p.getXRot(), true);
            c.getSource().sendSuccess(() -> Component.literal("Moved you up one block."), false);
            return 1;
        }));
        d.register(Commands.literal("deathpoint").executes(c -> unavailable(c.getSource(), "Death-point history")));
        d.register(Commands.literal("lastdeath").executes(c -> unavailable(c.getSource(), "Last-death history")));
        d.register(Commands.literal("recover").executes(c -> unavailable(c.getSource(), "Death recovery")));

        d.register(Commands.literal("ptime").executes(c -> unavailable(c.getSource(), "Personal time")));
        d.register(Commands.literal("pweather").executes(c -> unavailable(c.getSource(), "Personal weather")));

        d.register(Commands.literal("clearchat").requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
            .executes(c -> {
                for (ServerPlayer p : c.getSource().getServer().getPlayerList().getPlayers()) {
                    for (int i = 0; i < 80; i++) p.sendSystemMessage(Component.literal(" "));
                    p.sendSystemMessage(Component.literal("Chat cleared by an administrator."));
                }
                return 1;
            }));
        d.register(Commands.literal("invsee").requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
            .then(Commands.argument("player", EntityArgument.player()).executes(c ->
                inventorySummary(c.getSource(), EntityArgument.getPlayer(c, "player"), false))));
        d.register(Commands.literal("endersee").requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
            .then(Commands.argument("player", EntityArgument.player()).executes(c ->
                inventorySummary(c.getSource(), EntityArgument.getPlayer(c, "player"), true))));

        d.register(Commands.literal("nick").then(Commands.argument("nickname", StringArgumentType.greedyString())
            .executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                String nick = StringArgumentType.getString(c, "nickname").trim();
                if (nick.isEmpty() || nick.length() > 32) {
                    c.getSource().sendFailure(Component.literal("Nickname must be 1-32 characters."));
                    return 0;
                }
                p.setCustomName(Component.literal(nick));
                p.setCustomNameVisible(true);
                c.getSource().sendSuccess(() -> Component.literal(
                    "Visible name tag set. Chat and tab-list names are unchanged."), false);
                return 1;
            })));
        d.register(Commands.literal("hat").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            ItemStack held = p.getItemInHand(InteractionHand.MAIN_HAND);
            if (held.isEmpty()) {
                c.getSource().sendFailure(Component.literal("Hold an item first."));
                return 0;
            }
            ItemStack previous = p.getItemBySlot(EquipmentSlot.HEAD);
            p.setItemSlot(EquipmentSlot.HEAD, held.copy());
            p.setItemInHand(InteractionHand.MAIN_HAND, previous);
            c.getSource().sendSuccess(() -> Component.literal("Held item equipped as a hat."), false);
            return 1;
        }));
        d.register(Commands.literal("trash").executes(c -> {
            c.getSource().sendSuccess(() -> Component.literal(
                "To discard the item in your main hand, run /trash confirm."), false);
            return 1;
        }).then(Commands.literal("confirm").executes(c -> discardHeld(c.getSource()))));
        d.register(Commands.literal("craft").executes(c -> unavailable(c.getSource(), "Portable crafting menu")));
        d.register(Commands.literal("workbench").executes(c -> unavailable(c.getSource(), "Portable crafting menu")));
        d.register(Commands.literal("enderchest").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            int count = 0;
            for (int i = 0; i < p.getEnderChestInventory().getContainerSize(); i++) {
                if (!p.getEnderChestInventory().getItem(i).isEmpty()) count++;
            }
            final int occupied = count;
            c.getSource().sendSuccess(() -> Component.literal("Your Ender Chest has " + occupied +
                " occupied slots. Open a placed Ender Chest to manage items."), false);
            return 1;
        }));
        d.register(Commands.literal("homeinfo").then(Commands.argument("name", StringArgumentType.word())
            .executes(c -> unavailable(c.getSource(), "Home information display"))));
        d.register(Commands.literal("renamehome").then(Commands.argument("old", StringArgumentType.word())
            .then(Commands.argument("new", StringArgumentType.word()).executes(c ->
                unavailable(c.getSource(), "Home renaming")))));
    }

    private static void alias(CommandDispatcher<CommandSourceStack> d, String name, String ccName, boolean args) {
        var root = Commands.literal(name).executes(c -> d.execute("cc " + ccName, c.getSource()));
        if (args) root.then(Commands.argument("arguments", StringArgumentType.greedyString())
            .executes(c -> d.execute("cc " + ccName + " " + StringArgumentType.getString(c, "arguments"), c.getSource())));
        d.register(root);
    }

    private static int randomTeleport(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        source.getServer().getCommands().performPrefixedCommand(source,
            "spreadplayers ~ ~ 100 10000 false " + p.getGameProfile().name());
        return 1;
    }

    private static int inventorySummary(CommandSourceStack source, ServerPlayer target, boolean ender) {
        var inv = ender ? target.getEnderChestInventory() : target.getInventory();
        int occupied = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) if (!inv.getItem(i).isEmpty()) occupied++;
        final int slots = occupied;
        source.sendSuccess(() -> Component.literal((ender ? "Ender Chest" : "Inventory") + " for " +
            target.getGameProfile().name() + ": " + slots + " occupied slots (read-only summary)."), false);
        return 1;
    }

    private static int discardHeld(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        source.sendSuccess(() -> Component.literal("Held item discarded."), false);
        return 1;
    }

    private static int unavailable(CommandSourceStack source, String feature) {
        source.sendFailure(Component.literal(feature + " is not implemented yet."));
        return 0;
    }

    private static int fail(CommandSourceStack source, String message) {
        source.sendFailure(Component.literal(message));
        return 0;
    }
}
