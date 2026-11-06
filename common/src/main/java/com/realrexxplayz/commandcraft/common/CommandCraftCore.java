package com.realrexxplayz.commandcraft.common;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public final class CommandCraftCore {
    private static final String[][] ALIASES = {
        {"setspawn","setworldspawn","true"},{"spawnpoint","spawnpoint","true"},
        {"day","time set day","true"},{"night","time set night","true"},{"noon","time set noon","true"},{"midnight","time set midnight","true"},{"sunrise","time set 23000","true"},{"sunset","time set 12000","true"},
        {"weather","weather","true"},{"clearweather","weather clear","true"},{"rain","weather rain","true"},{"thunder","weather thunder","true"},{"time","time","true"},{"difficulty","difficulty","true"},{"gamemode","gamemode","true"},{"gms","gamemode survival","true"},{"gmc","gamemode creative","true"},{"gma","gamemode adventure","true"},{"gmsp","gamemode spectator","true"},
        {"gamerule","gamerule","true"},{"worldborder","worldborder","true"},{"xp","experience","true"},{"effect","effect","true"},{"cleareffects","effect clear @s","true"},
        {"tp","tp","true"},{"tpa","tp","true"},{"tpahere","tp","true"},{"tphere","tp","true"},
        {"msg","tell","false"},{"say","say","true"},{"rules","help","false"},
        {"kick","kick","true"},{"ban","ban","true"},{"pardon","pardon","true"},{"whitelist","whitelist","true"},{"unwhitelist","whitelist remove","true"},{"op","op","true"},
        {"list","list","false"},{"seed","seed","true"},{"locate","locate","true"},{"locatebiome","locate biome","true"},{"spectate","spectate","true"},{"spreadplayers","spreadplayers","true"},
        {"title","title","true"},{"tellraw","tellraw","true"},{"team","team","true"},{"scoreboard","scoreboard","true"},
    };

    private CommandCraftCore() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = Commands.literal("cc");
        root.then(Commands.literal("help").executes(ctx -> {
            ctx.getSource().sendSuccess(() -> Component.literal(
                "CommandCraft+ commands (64): help, coords, heal, feed, spawn, homes/warps, teleport, time, weather, player and server tools. Use /cc <command>."
            ), false);
            return 1;
        }));
        root.then(Commands.literal("coords").executes(ctx -> {
            var p = ctx.getSource().getPosition();
            ctx.getSource().sendSuccess(() -> Component.literal(String.format("X %.1f  Y %.1f  Z %.1f", p.x, p.y, p.z)), false);
            return 1;
        }));
        root.then(Commands.literal("heal").requires(s -> s.hasPermission(2)).executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            p.setHealth(p.getMaxHealth());
            ctx.getSource().sendSuccess(() -> Component.literal("Health restored."), false);
            return 1;
        }));
        root.then(Commands.literal("feed").requires(s -> s.hasPermission(2)).executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            p.getFoodData().eat(20, 20.0F);
            ctx.getSource().sendSuccess(() -> Component.literal("Food restored."), false);
            return 1;
        }));
        root.then(Commands.literal("spawn").executes(ctx -> {
            var source = ctx.getSource();
            ServerPlayer p = source.getPlayerOrException();
            var pos = source.getLevel().getSharedSpawnPos();
            run(source, "execute in " + source.getLevel().dimension().location() + " run tp @s " +
                (pos.getX() + 0.5) + " " + pos.getY() + " " + (pos.getZ() + 0.5));
            return 1;
        }));

        registerNamedStore(root, "sethome", "home", Store.HOME, Action.SET);
        registerNamedStore(root, "home", "home", Store.HOME, Action.GO);
        registerNamedStore(root, "delhome", "home", Store.HOME, Action.DELETE);
        registerList(root, "homes", Store.HOME);
        registerNamedStore(root, "setwarp", null, Store.WARP, Action.SET);
        registerNamedStore(root, "warp", null, Store.WARP, Action.GO);
        registerNamedStore(root, "delwarp", null, Store.WARP, Action.DELETE);
        registerList(root, "warps", Store.WARP);

        for (String[] alias : ALIASES) {
            String name = alias[0], target = alias[1];
            boolean admin = Boolean.parseBoolean(alias[2]);
            var literal = Commands.literal(name).requires(s -> !admin || s.hasPermission(2))
                .executes(ctx -> run(ctx.getSource(), target));
            literal.then(Commands.argument("arguments", StringArgumentType.greedyString())
                .executes(ctx -> run(ctx.getSource(), target + " " + StringArgumentType.getString(ctx, "arguments"))));
            root.then(literal);
        }
        dispatcher.register(root);
    }

    private enum Store { HOME, WARP }
    private enum Action { SET, GO, DELETE }

    private static void registerNamedStore(com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> root,
                                           String command, String defaultName, Store store, Action action) {
        var literal = Commands.literal(command).requires(s -> store == Store.HOME || (action != Action.SET && action != Action.DELETE) || s.hasPermission(2));
        if (defaultName != null) {
            literal.executes(ctx -> storeAction(ctx.getSource(), store, action, defaultName));
        }
        literal.then(Commands.argument("name", StringArgumentType.word()).executes(ctx ->
            storeAction(ctx.getSource(), store, action, StringArgumentType.getString(ctx, "name"))));
        root.then(literal);
    }

    private static void registerList(com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> root, String command, Store store) {
        root.then(Commands.literal(command).executes(ctx -> {
            var props = load(ctx.getSource());
            String prefix = store == Store.HOME ? "home." + ctx.getSource().getPlayerOrException().getUUID() + "." : "warp.";
            List<String> names = new ArrayList<>();
            for (String key : props.stringPropertyNames()) if (key.startsWith(prefix) && key.endsWith(".data"))
                names.add(key.substring(prefix.length(), key.length() - 5));
            ctx.getSource().sendSuccess(() -> Component.literal(names.isEmpty() ? "No saved " + command + "." : command + ": " + String.join(", ", names)), false);
            return 1;
        }));
    }

    private static int storeAction(CommandSourceStack source, Store store, Action action, String name) {
        if (!name.matches("[A-Za-z0-9_-]{1,24}")) {
            source.sendFailure(Component.literal("Names must be 1-24 letters, numbers, underscores or hyphens."));
            return 0;
        }
        try {
            Properties props = load(source);
            String prefix = store == Store.HOME ? "home." + source.getPlayerOrException().getUUID() + "." : "warp.";
            String key = prefix + name + ".data";
            if (action == Action.SET) {
                var p = source.getPosition();
                props.setProperty(key, source.getLevel().dimension().location() + "|" + p.x + "|" + p.y + "|" + p.z);
                save(source, props);
                source.sendSuccess(() -> Component.literal((store == Store.HOME ? "Home" : "Warp") + " '" + name + "' saved."), false);
            } else if (action == Action.DELETE) {
                if (props.remove(key) == null) {
                    source.sendFailure(Component.literal("No saved " + (store == Store.HOME ? "home" : "warp") + " named '" + name + "'."));
                    return 0;
                }
                save(source, props);
                source.sendSuccess(() -> Component.literal("Deleted '" + name + "'."), false);
            } else {
                String value = props.getProperty(key);
                if (value == null) {
                    source.sendFailure(Component.literal("No saved " + (store == Store.HOME ? "home" : "warp") + " named '" + name + "'."));
                    return 0;
                }
                String[] parts = value.split("\\|", -1);
                if (parts.length != 4 || !parts[0].matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
                    source.sendFailure(Component.literal("Saved location data is invalid."));
                    return 0;
                }
                run(source, "execute in " + parts[0] + " run tp @s " + parts[1] + " " + parts[2] + " " + parts[3]);
            }
            return 1;
        } catch (IOException e) {
            source.sendFailure(Component.literal("Could not access CommandCraft+ save data: " + e.getMessage()));
            return 0;
        }
    }

    private static Properties load(CommandSourceStack source) throws IOException {
        Path file = source.getServer().getWorldPath(LevelResource.ROOT).resolve("commandcraft-plus.properties");
        Properties props = new Properties();
        if (Files.exists(file)) try (var in = Files.newInputStream(file)) { props.load(in); }
        return props;
    }

    private static void save(CommandSourceStack source, Properties props) throws IOException {
        Path file = source.getServer().getWorldPath(LevelResource.ROOT).resolve("commandcraft-plus.properties");
        Files.createDirectories(file.getParent());
        try (var out = Files.newOutputStream(file)) { props.store(out, "CommandCraft+ saved homes and warps"); }
    }

    private static int run(CommandSourceStack source, String command) {
        source.getServer().getCommands().performPrefixedCommand(source, command);
        return 1;
    }
}
