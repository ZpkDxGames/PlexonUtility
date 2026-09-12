# PlexonUtility

Core-native player utilities, PlexonFamily interoperability, and ecosystem diagnostics for PlexonCraft.

## 3.0 stable surface

PlexonUtility 3.0 keeps small convenience actions together while using PlexonCore for shared runtime infrastructure:

- `/utility` — redesigned 4-row Plexon utility hub with player status, live AFK state, integration status, refresh/close controls, and admin diagnostics
- `/feed [player]`
- `/heal [player]`
- `/enderchest [player]` (`/ec`)
- `/workbench`
- `/trash` — disposable 27-slot inventory; contents left behind are destroyed on close
- `/afk` — manual and automatic AFK state with PlaceholderAPI and a public AFK transition event
- `/utilityadmin diagnostics|family|integrations|reload`

All configurable chat output uses MiniMessage through `PlexonCore.text()`. Static configured messages are parsed once and cached until reload; dynamic values use safe MiniMessage tag resolvers rather than string concatenation.

## PlexonFamily interoperability

PlexonUtility uses `PlexonCore.integrations()` as the shared compatibility registry. Family discovery happens on startup, reload, and plugin enable/disable events only; there is no integration polling task.

The 3.0 compatibility catalog includes:

- PlexonBackpacks
- PlexonBlacksmith
- PlexonChats
- PlexonCrates
- PlexonGPFlags
- PlexonHomes
- PlexonJobs
- PlexonKeys
- PlexonPanel
- PlexonQuests
- PlexonRanks
- PlexonShops
- PlexonSkills
- PlexonSpawners
- PlexonTools
- PlexonTravel

`/utilityadmin family` reports the live registry view.

### PlexonHomes integration

PlexonHomes remains the authoritative home/data/teleport service. PlexonUtility does not create a competing `/home`, `/homes`, or home database. When PlexonHomes is available, the Utility hub exposes a **Homes** entry that opens the authoritative `/homes` GUI.

PlexonHomes already exposes rank-friendly numeric home limits:

```text
plexonhomes.limit.<N>
plexonhomes.limit.unlimited
```

The highest effective numeric permission wins. If no numeric permission is present, PlexonHomes uses its configured default. `plexonhomes.limit.unlimited` overrides numeric limits. This means PlexonRanks can grant progressively larger limits without a hard dependency or duplicated rank logic, for example:

```text
plexonhomes.limit.3
plexonhomes.limit.5
plexonhomes.limit.10
plexonhomes.limit.15
```

The homes browser itself uses `plexonhomes.gui`.

## AFK interoperability

PlaceholderAPI outputs:

```text
%plexonutility_afk%
%plexonutility_is_afk%
```

`%plexonutility_afk%` returns the configured display value, while `%plexonutility_is_afk%` returns `true` or `false` and is suitable for TAB output replacements.

Other PlexonFamily plugins can consume `PlexonUtilityAPI#isAfk(UUID)` or listen for `AfkStateChangeEvent`. The event is informational, non-cancellable, and fired on the primary thread for manual, timeout, and activity transitions. Consumers therefore do not need to poll AFK state or implement their own AFK detector.

## Message migration and MiniMessage safety

PlexonUtility 3.0 fixes upgrades from older `messages.yml` files:

- missing required keys are copied from the bundled defaults into the existing file;
- existing customized values are preserved;
- migrated keys are persisted to disk;
- invalid MiniMessage still fails closed at startup/reload;
- failed reloads keep the previous known-good runtime state;
- static messages are component-cached until reload;
- dynamic values are inserted as plain components with `PlexonCore.text().renderTemplate(...)`.

This prevents old installations from displaying `Missing message: ...` after a release introduces new message keys.

## PlexonCore 2 architecture

PlexonUtility does not create duplicate shared infrastructure:

- text rendering and safe template values → `PlexonCore.text()`
- navigation GUI routing/session tracking → `PlexonCore.gui()`
- async-to-primary handoff → `PlexonCore.scheduler()`
- module lifecycle/capabilities → `PlexonCore.modules()`
- family/external integration visibility → `PlexonCore.integrations()`

The 3.0 line compiles against the stable PlexonCore `2.0.5` boundary and uses owner-aware module state updates and cleanup.

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

Dedicated Plexon products keep their own data and gameplay authority. PlexonUtility may surface their entry points/status but does not duplicate their persistence or transaction logic.

## Runtime

- Paper `26.2`
- Java `25`
- PlexonCore `2.0.5` compile/runtime boundary (`depend: PlexonCore`)
- PlaceholderAPI optional
- PlexonHomes optional integration
- no plugin-owned database
- in-memory monotonic cooldowns
- one shared AFK scan only when AFK auto-timeout is enabled

## Build and release

CI downloads the immutable `PlexonCore-2.0.5.jar`, verifies its pinned SHA-256, installs it only into the CI-local Maven repository, then runs:

```bash
mvn -B -ntp clean verify
```

The stable artifact is `target/PlexonUtility-3.0.0.jar`. CI verifies Java class major `69`, Paper `26.2` metadata, required 3.0 classes/resources, command descriptors, all tests, SHA-256 output, and that Core/Paper/Bukkit/PlaceholderAPI/Adventure runtime classes are not shaded into the JAR.

The stable release workflow only publishes an exact `main` commit with a stable Maven version, matching release notes, and a previously unused version tag.

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
