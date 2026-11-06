package com.realrexxplayz.commandcoreplus.common;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelData;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loader-neutral CommandCore+ command implementation.
 * Data is stored per-world in commandcraft-plus.properties.
 */
public final class CommandCorePlusCore {
    private static final Map<UUID, TeleportRequest> REQUESTS = new ConcurrentHashMap<>();
    private static final Map<UUID, LastPosition> LAST_POSITIONS = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> LAST_REQUESTS = new ConcurrentHashMap<>();
    private static final Set<UUID> GOD_MODE = ConcurrentHashMap.newKeySet();

    private CommandCorePlusCore() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var cc = Commands.literal("cc");

        cc.then(Commands.literal("help").executes(c -> {
            c.getSource().sendSuccess(() -> Component.literal(
                "CommandCore+: homes, warps, tpa, tpahere, tpaccept, tpdeny, tpacancel, back, spawn, rtp, " +
                "coords, near, whereami, heal, feed, fly, god, vanish, day, night, weather, broadcast, rules, motd, worldinfo."
            ), false);
            c.getSource().sendSuccess(() -> Component.literal(
                "Use /cc help <category> in a future update for detailed help. Admin commands require permission level 2."
            ), false);
            return 1;
        }));

        cc.then(Commands.literal("coords").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            var pos = p.position();
            c.getSource().sendSuccess(() -> Component.literal(String.format(
                "Coordinates: %.1f, %.1f, %.1f (%s)", pos.x, pos.y, pos.z,
                p.level().dimension().identifier()
            )), false);
            return 1;
        }));

        cc.then(Commands.literal("whereami").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            var pos = p.blockPosition();
            c.getSource().sendSuccess(() -> Component.literal(
                "You are in " + p.level().dimension().identifier() + " at " +
                pos.getX() + ", " + pos.getY() + ", " + pos.getZ()
            ), false);
            return 1;
        }));

        cc.then(Commands.literal("heal").requires(Commands.hasPermission(Commands.LEVEL_ADMINS)).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            p.setHealth(p.getMaxHealth());
            c.getSource().sendSuccess(() -> Component.literal("Your health has been restored."), false);
            return 1;
        }));

        cc.then(Commands.literal("feed").requires(Commands.hasPermission(Commands.LEVEL_ADMINS)).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            p.getFoodData().eat(20, 20.0F);
            c.getSource().sendSuccess(() -> Component.literal("Your hunger has been restored."), false);
            return 1;
        }));

        cc.then(Commands.literal("spawn").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            var level = p.level();
            var pos = level.getRespawnData().pos();
            remember(p);
            p.teleportTo(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                java.util.Set.of(), p.getYRot(), p.getXRot(), true);
            c.getSource().sendSuccess(() -> Component.literal("Teleported to this world's spawn."), false);
            return 1;
        }));

        cc.then(Commands.literal("setspawn").requires(Commands.hasPermission(Commands.LEVEL_ADMINS)).executes(c -> {
            var p = c.getSource().getPlayerOrException();
            p.level().setRespawnData(LevelData.RespawnData.of(p.level().dimension(), p.blockPosition(), p.getYRot(), p.getXRot()));
            c.getSource().sendSuccess(() -> Component.literal("World spawn updated to your position."), true);
            return 1;
        }));

        registerSavedLocation(cc, "sethome", SavedKind.HOME, SavedAction.SET, false);
        registerSavedLocation(cc, "home", SavedKind.HOME, SavedAction.GO, false);
        registerSavedLocation(cc, "delhome", SavedKind.HOME, SavedAction.DELETE, false);
        registerSavedList(cc, "homes", SavedKind.HOME);
        registerSavedLocation(cc, "setwarp", SavedKind.WARP, SavedAction.SET, true);
        registerSavedLocation(cc, "warp", SavedKind.WARP, SavedAction.GO, false);
        registerSavedLocation(cc, "delwarp", SavedKind.WARP, SavedAction.DELETE, true);
        registerSavedList(cc, "warps", SavedKind.WARP);

        cc.then(Commands.literal("tpa").then(Commands.argument("player", EntityArgument.player())
            .executes(c -> requestTeleport(c.getSource(), EntityArgument.getPlayer(c, "player"), false))));
        cc.then(Commands.literal("tpahere").then(Commands.argument("player", EntityArgument.player())
            .executes(c -> requestTeleport(c.getSource(), EntityArgument.getPlayer(c, "player"), true))));
        cc.then(Commands.literal("tpaccept").executes(c -> answerRequest(c.getSource(), true)));
        cc.then(Commands.literal("tpdeny").executes(c -> answerRequest(c.getSource(), false)));
        cc.then(Commands.literal("tpacancel").executes(c -> cancelRequest(c.getSource())));
        cc.then(Commands.literal("back").executes(c -> goBack(c.getSource())));

        cc.then(Commands.literal("near").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            var nearby = p.level().getPlayers(other -> true).stream()
                .filter(other -> other != p && other.distanceTo(p) <= 100.0)
                .map(ServerPlayer::getGameProfile)
                .map(profile -> profile.name())
                .toList();
            c.getSource().sendSuccess(() -> Component.literal(
                nearby.isEmpty() ? "No players within 100 blocks." : "Nearby (100 blocks): " + String.join(", ", nearby)
            ), false);
            return 1;
        }));

        cc.then(Commands.literal("worldinfo").executes(c -> {
            var level = c.getSource().getLevel();
            c.getSource().sendSuccess(() -> Component.literal(
                "Dimension: " + level.dimension().identifier() + " | Day time: " + level.getLevelData().getGameTime() +
                " | Players: " + c.getSource().getServer().getPlayerList().getPlayerCount()
            ), false);
            return 1;
        }));

        cc.then(Commands.literal("day").requires(Commands.hasPermission(Commands.LEVEL_ADMINS)).executes(c -> setTime(c.getSource(), 1000L, "Day")));
        cc.then(Commands.literal("night").requires(Commands.hasPermission(Commands.LEVEL_ADMINS)).executes(c -> setTime(c.getSource(), 13000L, "Night")));
        cc.then(Commands.literal("weather").requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
            .then(Commands.literal("clear").executes(c -> setWeather(c.getSource(), false, false)))
            .then(Commands.literal("rain").executes(c -> setWeather(c.getSource(), true, false)))
            .then(Commands.literal("thunder").executes(c -> setWeather(c.getSource(), true, true))));

        cc.then(Commands.literal("broadcast").requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
            .then(Commands.argument("message", StringArgumentType.greedyString()).executes(c -> {
                Component message = Component.literal("[CommandCore+] " + StringArgumentType.getString(c, "message"));
                c.getSource().getServer().getPlayerList().broadcastSystemMessage(message, false);
                return 1;
            })));

        cc.then(Commands.literal("rules").executes(c -> {
            c.getSource().sendSuccess(() -> Component.literal(
                "Server rules have not been configured. An administrator can add them in the CommandCore+ config in a later release."
            ), false);
            return 1;
        }));

        cc.then(Commands.literal("list").executes(c -> {
            var names = c.getSource().getServer().getPlayerList().getPlayers().stream()
                .map(player -> player.getGameProfile().name()).toList();
            c.getSource().sendSuccess(() -> Component.literal(
                "Online (" + names.size() + "): " + (names.isEmpty() ? "nobody" : String.join(", ", names))
            ), false);
            return 1;
        }));

        cc.then(Commands.literal("fly").requires(Commands.hasPermission(Commands.LEVEL_ADMINS)).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            var abilities = p.getAbilities();
            abilities.mayfly = !abilities.mayfly;
            if (!abilities.mayfly) abilities.flying = false;
            p.onUpdateAbilities();
            c.getSource().sendSuccess(() -> Component.literal("Flight " + (abilities.mayfly ? "enabled" : "disabled") + "."), false);
            return 1;
        }));

        cc.then(Commands.literal("god").requires(Commands.hasPermission(Commands.LEVEL_ADMINS)).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            boolean enabled;
            if (GOD_MODE.add(p.getUUID())) {
                p.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, MobEffectInstance.INFINITE_DURATION, 4, false, false, true));
                enabled = true;
            } else {
                GOD_MODE.remove(p.getUUID());
                p.removeEffect(MobEffects.RESISTANCE);
                enabled = false;
            }
            final boolean godModeEnabled = enabled;
            c.getSource().sendSuccess(() -> Component.literal("God mode " + (godModeEnabled ? "enabled" : "disabled") + "."), false);
            return 1;
        }));

        cc.then(Commands.literal("vanish").requires(Commands.hasPermission(Commands.LEVEL_ADMINS)).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            p.setInvisible(!p.isInvisible());
            c.getSource().sendSuccess(() -> Component.literal("Vanish " + (p.isInvisible() ? "enabled" : "disabled") + "."), false);
            return 1;
        }));

        cc.then(Commands.literal("motd").executes(c -> {
            String motd = load(c.getSource()).getProperty("server.motd", "Welcome to the server!");
            c.getSource().sendSuccess(() -> Component.literal(motd), false);
            return 1;
        }).then(Commands.argument("message", StringArgumentType.greedyString()).requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
            .executes(c -> {
                Properties data = load(c.getSource());
                data.setProperty("server.motd", StringArgumentType.getString(c, "message"));
                save(c.getSource(), data);
                c.getSource().sendSuccess(() -> Component.literal("CommandCore+ MOTD updated."), true);
                return 1;
            })));

        cc.then(Commands.literal("maintenance").requires(Commands.hasPermission(Commands.LEVEL_ADMINS)).executes(c -> {
            Properties data = load(c.getSource());
            boolean enabled = Boolean.parseBoolean(data.getProperty("server.maintenance", "false"));
            data.setProperty("server.maintenance", Boolean.toString(!enabled));
            save(c.getSource(), data);
            c.getSource().sendSuccess(() -> Component.literal("Maintenance mode " + (!enabled ? "enabled" : "disabled") +
                ". (This toggle is informational; it does not yet block logins.)"), true);
            return 1;
        }));

        dispatcher.register(cc);
        CommandCorePlusDirectCommands.register(dispatcher);
    }

    private enum SavedKind { HOME, WARP }
    private enum SavedAction { SET, GO, DELETE }

    private static void registerSavedLocation(com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> root,
                                              String command, SavedKind kind, SavedAction action, boolean adminOnly) {
        var literal = Commands.literal(command).requires(s -> !adminOnly || Commands.hasPermission(Commands.LEVEL_ADMINS).test(s));
        literal.executes(c -> savedLocation(c.getSource(), kind, action, "home"));
        literal.then(Commands.argument("name", StringArgumentType.word()).executes(c ->
            savedLocation(c.getSource(), kind, action, StringArgumentType.getString(c, "name"))));
        root.then(literal);
    }

    private static void registerSavedList(com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> root,
                                          String command, SavedKind kind) {
        root.then(Commands.literal(command).executes(c -> {
            ServerPlayer player = c.getSource().getPlayerOrException();
            Properties data = load(c.getSource());
            String prefix = kind == SavedKind.HOME ? "home." + player.getUUID() + "." : "warp.";
            var names = data.stringPropertyNames().stream()
                .filter(key -> key.startsWith(prefix) && key.endsWith(".location"))
                .map(key -> key.substring(prefix.length(), key.length() - ".location".length()))
                .sorted().toList();
            c.getSource().sendSuccess(() -> Component.literal(
                names.isEmpty() ? "No saved " + (kind == SavedKind.HOME ? "homes" : "warps") + "." :
                    (kind == SavedKind.HOME ? "Homes: " : "Warps: ") + String.join(", ", names)
            ), false);
            return 1;
        }));
    }

    private static int savedLocation(CommandSourceStack source, SavedKind kind, SavedAction action, String name) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!name.matches("[A-Za-z0-9_-]{1,24}")) {
            source.sendFailure(Component.literal("Names must be 1-24 letters, numbers, underscores or hyphens."));
            return 0;
        }
        Properties data = load(source);
        String prefix = kind == SavedKind.HOME ? "home." + player.getUUID() + "." : "warp.";
        String key = prefix + name + ".location";
        if (action == SavedAction.SET) {
            var pos = player.position();
            String value = player.level().dimension().identifier() + "|" + pos.x + "|" + pos.y + "|" + pos.z + "|" + player.getYRot() + "|" + player.getXRot();
            data.setProperty(key, value);
            save(source, data);
            source.sendSuccess(() -> Component.literal((kind == SavedKind.HOME ? "Home" : "Warp") + " '" + name + "' saved."), false);
            return 1;
        }
        if (action == SavedAction.DELETE) {
            if (data.remove(key) == null) {
                source.sendFailure(Component.literal("No saved location named '" + name + "'."));
                return 0;
            }
            save(source, data);
            source.sendSuccess(() -> Component.literal("Deleted '" + name + "'."), false);
            return 1;
        }
        String value = data.getProperty(key);
        if (value == null) {
            source.sendFailure(Component.literal("No saved location named '" + name + "'."));
            return 0;
        }
        String[] parts = value.split("\\|", -1);
        if (parts.length != 6) {
            source.sendFailure(Component.literal("Saved location data is invalid."));
            return 0;
        }
        var dimension = Identifier.tryParse(parts[0]);
        if (dimension == null) {
            source.sendFailure(Component.literal("Saved dimension is invalid."));
            return 0;
        }
        var level = source.getServer().getLevel(net.minecraft.resources.ResourceKey.create(
            net.minecraft.core.registries.Registries.DIMENSION, dimension));
        if (level == null) {
            source.sendFailure(Component.literal("That dimension is not currently available."));
            return 0;
        }
        double x = Double.parseDouble(parts[1]), y = Double.parseDouble(parts[2]), z = Double.parseDouble(parts[3]);
        float yaw = Float.parseFloat(parts[4]), pitch = Float.parseFloat(parts[5]);
        remember(player);
        player.teleportTo(level, x, y, z, java.util.Set.of(), yaw, pitch, true);
        source.sendSuccess(() -> Component.literal("Teleported to " + (kind == SavedKind.HOME ? "home " : "warp ") + name + "."), false);
        return 1;
    }

    private static int requestTeleport(CommandSourceStack source, ServerPlayer target, boolean bringRequester) throws CommandSyntaxException {
        ServerPlayer requester = source.getPlayerOrException();
        if (requester == target) {
            source.sendFailure(Component.literal("You cannot send a teleport request to yourself."));
            return 0;
        }
        REQUESTS.put(target.getUUID(), new TeleportRequest(requester.getUUID(), bringRequester));
        LAST_REQUESTS.put(requester.getUUID(), target.getUUID());
        target.sendSystemMessage(Component.literal(requester.getGameProfile().name() +
            (bringRequester ? " wants you to teleport to them." : " wants to teleport to you.") +
            " Use /cc tpaccept or /cc tpdeny."));
        source.sendSuccess(() -> Component.literal("Teleport request sent to " + target.getGameProfile().name() + "."), false);
        return 1;
    }

    private static int answerRequest(CommandSourceStack source, boolean accept) throws CommandSyntaxException {
        ServerPlayer target = source.getPlayerOrException();
        TeleportRequest request = REQUESTS.remove(target.getUUID());
        if (request == null) {
            source.sendFailure(Component.literal("You do not have a pending teleport request."));
            return 0;
        }
        ServerPlayer requester = source.getServer().getPlayerList().getPlayer(request.requester());
        if (requester == null) {
            source.sendFailure(Component.literal("The requester is no longer online."));
            return 0;
        }
        if (!accept) {
            requester.sendSystemMessage(Component.literal(target.getGameProfile().name() + " declined your teleport request."));
            source.sendSuccess(() -> Component.literal("Teleport request declined."), false);
            return 1;
        }
        ServerPlayer mover = request.bringRequester() ? target : requester;
        ServerPlayer destination = request.bringRequester() ? requester : target;
        remember(mover);
        mover.teleportTo(destination.level(), destination.getX(), destination.getY(), destination.getZ(),
            java.util.Set.of(), destination.getYRot(), destination.getXRot(), true);
        requester.sendSystemMessage(Component.literal("Teleport request accepted."));
        source.sendSuccess(() -> Component.literal("Teleport request accepted."), false);
        return 1;
    }

    private static int cancelRequest(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer requester = source.getPlayerOrException();
        UUID targetId = LAST_REQUESTS.remove(requester.getUUID());
        if (targetId == null) {
            source.sendFailure(Component.literal("You have no recent teleport request to cancel."));
            return 0;
        }
        TeleportRequest request = REQUESTS.get(targetId);
        if (request != null && request.requester().equals(requester.getUUID())) {
            REQUESTS.remove(targetId);
            ServerPlayer target = source.getServer().getPlayerList().getPlayer(targetId);
            if (target != null) target.sendSystemMessage(Component.literal("The teleport request was cancelled."));
            source.sendSuccess(() -> Component.literal("Teleport request cancelled."), false);
            return 1;
        }
        source.sendFailure(Component.literal("That request is no longer pending."));
        return 0;
    }

    private static int goBack(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        LastPosition last = LAST_POSITIONS.remove(player.getUUID());
        if (last == null) {
            source.sendFailure(Component.literal("You do not have a previous CommandCore+ teleport location."));
            return 0;
        }
        var level = source.getServer().getLevel(last.dimension());
        if (level == null) {
            source.sendFailure(Component.literal("Your previous dimension is unavailable."));
            return 0;
        }
        remember(player);
        player.teleportTo(level, last.x(), last.y(), last.z(), java.util.Set.of(), last.yaw(), last.pitch(), true);
        source.sendSuccess(() -> Component.literal("Returned to your previous location."), false);
        return 1;
    }

    private static void remember(ServerPlayer player) {
        LAST_POSITIONS.put(player.getUUID(), new LastPosition(player.level().dimension(),
            player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot()));
    }

    private static int setTime(CommandSourceStack source, long time, String label) {
        source.getServer().getCommands().performPrefixedCommand(source, "time set " + (time == 1000L ? "day" : "night"));
        source.sendSuccess(() -> Component.literal("Time set to " + label + "."), true);
        return 1;
    }

    private static int setWeather(CommandSourceStack source, boolean rain, boolean thunder) {
        source.getServer().setWeatherParameters(0, 12000, rain, thunder);
        source.sendSuccess(() -> Component.literal("Weather updated."), true);
        return 1;
    }

    private static Properties load(CommandSourceStack source) {
        Path file = dataFile(source);
        Properties properties = new Properties();
        if (Files.exists(file)) {
            try (var input = Files.newInputStream(file)) {
                properties.load(input);
            } catch (IOException e) {
                source.sendFailure(Component.literal("Could not read CommandCore+ data: " + e.getMessage()));
            }
        }
        return properties;
    }

    private static void save(CommandSourceStack source, Properties properties) {
        Path file = dataFile(source);
        try {
            Files.createDirectories(file.getParent());
            try (var output = Files.newOutputStream(file)) {
                properties.store(output, "CommandCore+ world data");
            }
        } catch (IOException e) {
            source.sendFailure(Component.literal("Could not save CommandCore+ data: " + e.getMessage()));
        }
    }

    private static Path dataFile(CommandSourceStack source) {
        return source.getServer().getWorldPath(LevelResource.ROOT).resolve("commandcraft-plus.properties");
    }

    private record TeleportRequest(UUID requester, boolean bringRequester) {}
    private record LastPosition(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,
                                double x, double y, double z, float yaw, float pitch) {}
}
