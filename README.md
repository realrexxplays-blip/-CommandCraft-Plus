# CommandCraft+

**Your world. Your server. Your commands.**

CommandCraft+ is a dual-loader Minecraft Java utility and server-management mod for Minecraft 26.3 and Java 25.

## The 64-command catalog

Core: `/cc help`, `coords`, `heal`, `feed`, `spawn`.

Homes and warps: `sethome`, `home`, `delhome`, `homes`, `setwarp`, `warp`, `delwarp`, `warps`. Homes are player-specific; warps are shared by the world. Saved locations persist in the world save folder.

World and time: `setspawn`, `spawnpoint`, `day`, `night`, `noon`, `midnight`, `sunrise`, `sunset`, `weather`, `clearweather`, `rain`, `thunder`, `time`, `difficulty`, `gamerule`, `worldborder`.

Player and utility: `gamemode`, `gms`, `gmc`, `gma`, `gmsp`, `xp`, `effect`, `cleareffects`, `tp`, `tpa`, `tpahere`, `tphere`, `msg`, `say`, `rules`, `list`, `seed`.

Server administration: `kick`, `ban`, `pardon`, `whitelist`, `unwhitelist`, `op`, `locate`, `locatebiome`, `spectate`, `spreadplayers`, `title`, `tellraw`, `team`, `scoreboard`.

All command names are used under the `/cc` root. Example: `/cc day`, `/cc sethome base`, `/cc home base`, `/cc setwarp shop`, `/cc warp shop`.

Commands that change world/server settings or moderate players require permission level 2. Homes are player-accessible; setting and deleting shared warps requires permission level 2. Some entries are aliases for vanilla commands and use their normal argument syntax and permission checks. Teleport aliases are direct vanilla teleports, not a request/accept workflow. GUI screens, teleport request queues, back-history, player profiles, and configurable permission/cooldown systems are not included in this command pass yet.

## Build
Use JDK 25 and Gradle 9.6. GitHub Actions builds both loaders and uploads the resulting JARs as workflow artifacts.
