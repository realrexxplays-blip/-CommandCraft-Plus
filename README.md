# CommandCraft+

**Your world. Your server. Your commands.**

CommandCraft+ is a dual-loader Minecraft Java utility and server-management mod targeting Minecraft 26.3 and Java 25.

## Implemented commands in this build

- `/cc help` — show CommandCraft+ help
- `/cc sethome [name]`, `/cc home [name]`, `/cc homes`, `/cc delhome [name]` — player-specific homes
- `/cc setwarp <name>`, `/cc warp <name>`, `/cc warps`, `/cc delwarp <name>` — shared server warps
- `/cc tpa <player>`, `/cc tpahere <player>`, `/cc tpaccept`, `/cc tpdeny`, `/cc tpacancel` — actual request/accept/deny teleport flow
- `/cc back` — return to the last location before a CommandCraft+ teleport
- `/cc spawn`, `/cc setspawn` — world spawn travel and setup
- `/cc coords`, `/cc whereami`, `/cc near`, `/cc worldinfo`, `/cc list` — player and world information
- `/cc heal`, `/cc feed`, `/cc fly`, `/cc god`, `/cc vanish` — player utilities
- `/cc day`, `/cc night`, `/cc weather clear|rain|thunder` — world time and weather
- `/cc broadcast <message>`, `/cc rules`, `/cc motd [message]`, `/cc maintenance` — server information and administration

Homes, warps, MOTD, and the maintenance toggle are stored in the world save folder. Teleport requests are server-session data and are cleared when the server restarts. The back history currently tracks CommandCraft+ teleports only.

## Planned next features

The full planned catalog also includes random teleport, hub locations, death recovery, player profiles and playtime, configurable rules and welcome messages, personal time/weather, inventory viewers, GUI menus, permission groups, configurable cooldowns, and expanded administration tools. These are not represented as working commands until their implementations are complete.

## Permissions

Player commands are available to players. World-changing and server-administration commands require permission level 2. Setting or deleting shared warps requires permission level 2.

## Build

Use JDK 25. The GitHub Actions workflow builds Fabric and NeoForge separately and uploads runtime JARs as an artifact when both builds succeed.
