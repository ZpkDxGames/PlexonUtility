# PlexonUtility

Core-native player utilities and ecosystem complement diagnostics for PlexonCraft.

## 2.0 stable surface

PlexonUtility 2.0 keeps small convenience actions together while moving shared runtime work into PlexonCore:

- `/utility` — compact Plexon-style utility hub powered by `PlexonCore.gui()`
- `/feed [player]`
- `/heal [player]`
- `/enderchest [player]` (`/ec`)
- `/workbench`
- `/trash` — disposable 27-slot inventory; contents left behind are destroyed on close
- `/afk` — manual and automatic AFK state with PlaceholderAPI support
- `/utilityadmin diagnostics|integrations|reload`

All configurable chat output uses MiniMessage through `PlexonCore.text()`. The default PlexonUtility identity uses the Plexon green→cyan gradient `#57E389 → #22D3EE`.

## PlexonCore 2.0 architecture

PlexonUtility does not create duplicate shared infrastructure:

- text rendering and safe template values → `PlexonCore.text()`
- navigation GUI routing/session tracking → `PlexonCore.gui()`
- async-to-primary handoff → `PlexonCore.scheduler()`
- module lifecycle/capabilities → `PlexonCore.modules()`
- external complement visibility → `PlexonCore.integrations()`

The AFK system retains one bounded shared repeating scan for online players. There is never one scheduler per player, no filesystem/database I/O on activity events, and AFK state remains intentionally ephemeral.

## Specialist ownership boundary

PlexonUtility deliberately does **not** reimplement mature specialist systems. `/utilityadmin integrations` detects and publishes availability for:

- land/regions: GriefPrevention, Lands, Towny, WorldGuard
- block audit/rollback: CoreProtect
- permissions: LuckPerms
- configurable menu builders: DeluxeMenus, ChestCommands
- anti-cheat: GrimAC, Vulcan
- display/HUD: TAB, FancyHolograms, DecentHolograms
- profiling/pre-generation: spark, Chunky
- proximity voice: Simple Voice Chat / voicechat

Dedicated Plexon products also keep their existing ownership boundaries: travel, homes, shops, jobs, skills, tools, ranks, chats, quests, crates, backpacks, spawners, keys, blacksmith, claim flags, and Panel are not duplicated here.

## Runtime

- Paper `26.2`
- Java `25`
- PlexonCore `2.0.0` Runtime API (`depend: PlexonCore`)
- PlaceholderAPI optional
- no plugin-owned database
- in-memory monotonic cooldowns
- one shared AFK scan only when AFK auto-timeout is enabled

## Configuration and reload safety

- feature toggles are independent
- invalid types/ranges are rejected instead of coerced
- `config.yml` and `messages.yml` are completely parsed before live state changes
- MiniMessage formatting is validated at startup/reload
- failed reloads keep the previous known-good runtime state
- runtime placeholder values are inserted as plain components rather than parsed as MiniMessage

## Build and release

CI downloads the immutable `PlexonCore-2.0.0.jar`, verifies its pinned SHA-256, installs it only into the CI-local Maven repository, then runs:

```bash
mvn -B -ntp clean verify
```

The stable artifact is `target/PlexonUtility-2.0.0.jar`. CI verifies Java class major `69`, Paper `26.2` metadata, required 2.0 classes/resources, command descriptors, all tests, SHA-256 output, and that Core/Paper/Bukkit/PlaceholderAPI/Adventure runtime classes are not shaded into the JAR.

The `release/stable` workflow only publishes when its commit is exactly equal to `main`, the Maven version is stable, `releases/2.0.0.md` exists, and tag `v2.0.0` does not already exist.

## Permissions

```text
plexonutility.menu
plexonutility.feed
plexonutility.feed.others
plexonutility.feed.cooldown.bypass
plexonutility.heal
plexonutility.heal.others
plexonutility.heal.cooldown.bypass
plexonutility.enderchest
plexonutility.enderchest.others
plexonutility.workbench
plexonutility.trash
plexonutility.afk
plexonutility.afk.auto.bypass
plexonutility.admin
plexonutility.reload
```
