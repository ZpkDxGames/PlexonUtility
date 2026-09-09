# Architecture

PlexonUtility 1.0.0 is intentionally stateless except for optional in-memory cooldown timestamps.

## Runtime components

- `PlexonUtilityPlugin` — lifecycle, API service registration, command wiring, quit cleanup.
- `CoreBridge` — loads `PlexonCoreAPI`, verifies API 2.0 compatibility, registers module id `utility`, and publishes only enabled capabilities.
- `UtilityConfig` — validates and freezes the current runtime configuration.
- `UtilityCommand` — main-thread Bukkit mutations for feed, heal, Ender Chest, and workbench.
- `CooldownService` — UUID + feature monotonic timestamps; no persistence and no task-per-player model.
- `MessageService` — low-frequency Adventure/MiniMessage command feedback.
- `PlexonUtilityAPI` — compact public feature/status API.

## Performance model

There are no repeating tasks and no high-frequency listeners in 1.0.0. The only listener is player quit cleanup. Feed/heal/inventory operations execute only when commands are invoked. Cooldown state is allocated only when a configured cooldown is non-zero and a player successfully uses the command.

## Threading

All player mutation and inventory opening occurs on the command/main thread. No asynchronous `Player` mutation exists.

## Persistence

None. Cooldown state resets on quit/restart by design.
