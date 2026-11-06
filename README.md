# CommandCore+

**More commands. More control. Your world, your rules.**

CommandCore+ is a dual-loader Minecraft Java utility and server-management mod targeting Minecraft 26.3 and Java 25. Separate builds are provided for Fabric and NeoForge.

## Implemented commands in this build

- `/cc help` — show CommandCore+ help
- `/cc sethome [name]`, `/cc home [name]`, `/cc homes`, `/cc delhome [name]` — player-specific homes
- `/cc setwarp <name>`, `/cc warp <name>`, `/cc warps`, `/cc delwarp <name>` — shared server warps
- `/cc tpa <player>`, `/cc tpahere <player>`, `/cc tpaccept`, `/cc tpdeny`, `/cc tpacancel` — teleport request flow
- `/cc back` — return to the last location before a CommandCore+ teleport
- `/cc spawn`, `/cc setspawn` — world spawn travel and setup
- `/cc coords`, `/cc whereami`, `/cc near`, `/cc worldinfo`, `/cc list` — player and world information
- `/cc heal`, `/cc feed`, `/cc fly`, `/cc god`, `/cc vanish` — player utilities
- `/cc day`, `/cc night`, `/cc weather clear|rain|thunder` — world time and weather
- `/cc broadcast <message>`, `/cc rules`, `/cc motd [message]`, `/cc maintenance` — server information and administration

The project also registers additional standalone utility commands. Some are informational placeholders or are awaiting full implementation; see the CurseForge description for the planned command catalog.

## Saved data

Homes, warps, MOTD, and the maintenance toggle are stored in the world save folder. Existing world data remains in the legacy `commandcraft-plus.properties` file so this rebrand does not strand saved locations. Teleport requests are server-session data and are cleared when the server restarts.

## Planned next features

The wider roadmap includes random teleport, hub locations, death recovery, player profiles and playtime, configurable rules and welcome messages, personal time/weather, inventory viewers, GUI menus, permission groups, configurable cooldowns, and expanded administration tools. These are not represented as working commands until their implementations are complete.

## Permissions

Player commands are available to players. World-changing and server-administration commands require permission level 2. Setting or deleting shared warps requires permission level 2.

## Build

Use JDK 25. The GitHub Actions workflow builds Fabric and NeoForge separately and uploads their runtime JARs as an artifact when both builds succeed.
