# PlexonUtility 2.0 Architecture

## Product role

PlexonUtility owns small, generic player convenience actions that do not justify dedicated Plexon products. It is not a replacement for permissions, claims, rollbacks, anti-cheat, displays, profiling, world pre-generation, voice chat, economy, travel, homes, or other specialist systems.

## Shared PlexonCore runtime

The 2.0 module registers as `utility` against PlexonCore API `>=2.0 <3.0` and declares its enabled feature IDs plus these capabilities:

- `utility-api`
- `core-text`
- `core-gui`
- `core-scheduler`
- `complement-diagnostics`

Shared behavior is delegated to Core:

| Concern | Owner |
| --- | --- |
| MiniMessage/text templates | `PlexonCore.text()` |
| Utility navigation GUI routing | `PlexonCore.gui()` |
| Async → primary handoff | `PlexonCore.scheduler()` |
| Module lifecycle | `PlexonCore.modules()` |
| External ecosystem states | `PlexonCore.integrations()` |

This keeps listeners, GUI session tracking, text parsing policy, worker pools and ecosystem status centralized.

## Local runtime state

PlexonUtility locally owns only state that is specific and cheap:

- monotonic cooldown timestamps
- AFK activity timestamps/state
- one AFK timeout scan task when automatic AFK is enabled
- the current immutable validated utility configuration
- the current validated message catalog

No plugin-owned database is required. AFK state is intentionally ephemeral and cooldowns are cleared on quit.

## GUI surfaces

`/utility` is a navigation/selection GUI and therefore uses the Core GUI service, including Core holder identity, click routing and session tracking.

`/trash` intentionally does **not** use Core GUI routing because it must permit normal item movement. It creates a short-lived writable Bukkit inventory with a private holder; no persistent reference is retained after the view closes, so remaining contents are discarded.

## External complements

`ComplementService` scans only for enabled specialist providers. It never proxies their commands or claims that PlexonUtility implements their functionality. Each category is published to `PlexonCore.integrations()` under a `UTILITY_*` namespace, with `READY` when at least one configured provider is enabled and `MISSING` otherwise.

## Performance constraints

- no per-player repeating scheduler
- no file/database/network I/O from gameplay event listeners
- no plugin-local executor pool
- no plugin-local navigation GUI listener/router
- no plugin-local MiniMessage instance
- AFK movement ignores rotation/sub-block movement
- async chat state mutation remains thread-safe and UI notification handoff uses Core
- external complement scans occur only at startup, explicit reload, or explicit integration diagnostics
