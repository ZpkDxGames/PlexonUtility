# PlexonUtility 3.5 Architecture

## Product role and ownership

PlexonUtility owns small generic player conveniences plus bounded, auditable server/player administration that does not justify a dedicated Plexon product. Specialist Plexon Family plugins keep their domains:

- PlexonTravel owns `/spawn`, `/hub`, `/back`, warps, RTP and TPA/destination travel.
- PlexonHomes owns homes, limits and home teleport/data.
- PlexonChats owns public/private chat routing, formatting and connection-message presentation when its public API exposes the needed surface.
- PlexonBlacksmith owns repair/gameplay item servicing.
- PlexonRanks/economy/gameplay modules keep rank, money, jobs, skills, quests, keys, crates and related data.

PlexonUtility 3.5 entity spawning is `/spawnmob`. It never registers root `/spawn`.

## Shared PlexonCore runtime

The module registers as `utility` against PlexonCore API `>=2.0 <3.0` and compiles against PlexonCore `2.0.5`.

Core remains authoritative for text, GUI lifecycle, scheduling, module lifecycle and integration state. Active 3.5 capability registration additionally exposes the enabled administration surface, including:

- `admin-toolkit`
- `editable-invsee`
- `native-vanish`
- `synthetic-presence`
- `vanish-event`
- `entity-management`
- `entity-cleanup`
- `entity-spawn`
- `player-admin`
- `gamemode-control`
- `flight-control`
- `god-mode`
- `speed-control`
- `inventory-clear`

Only capabilities whose configuration is active are advertised.

## Command architecture

The legacy moderation executor remains focused on `/invsee`, `/vanish`, `/kick`, `/ban`, `/unban` and `/prison`. New 3.5 functionality is separated:

```text
command/
  EntityAdminCommand
  PlayerAdminCommand

admin/entity/
  EntitySelector
  EntityCleanupService
  EntitySpawnService

admin/player/
  PlayerManagementService

admin/vanish/
  VanishService
  VanishListener
  SyntheticPresenceBridge

api/event/
  VanishStateChangeEvent
```

Command classes own parsing, permission checks, confirmation state and tab completion. Services own business rules, mutation and aggregate audit output.

## Entity cleanup model

`/killall` executes one bounded scan of the selected world/radius. Radius selection is spherical and never fans out explicit chunk loads.

Unconditional protection includes players, interaction entities and markers. Default configurable protection additionally covers named entities, tamed entities, villagers, armor stands, displays and recognizable plugin/NPC metadata/tags. Bosses are protected unless the caller explicitly selects `bosses` or a boss entity type.

Large cleanup confirmation is:

- actor-specific;
- selector/scope-specific because the stored immutable query is the one executed;
- memory-only;
- 15 seconds;
- overwritten/invalidated by a changed cleanup request;
- free of scheduled cleanup tasks.

Cleanup uses direct entity removal rather than simulated combat, avoiding intentional loot/XP/kill-credit semantics.

## Entity spawn model

`/spawnmob` accepts only living, Paper-spawnable entity types and never `PLAYER`. Config can reduce the maximum or block specific types, but the runtime hard maximum is always 100.

Placement searches only a small bounded set around the targeted block/player and requires the target chunk to already be loaded. It does not synchronously fan out chunk generation/loading. Entity creation stays on the normal server thread.

## Player administration state

`PlayerManagementService` owns only cheap in-memory runtime state:

- Utility-managed Survival/Adventure flight UUIDs;
- god-mode UUIDs.

God mode is event-driven through damage cancellation and deliberately does not cancel void damage. It is restart-ephemeral in 3.5; `god.persist` is reserved and startup/reload rejects `true`.

Utility flight never strips legitimate Creative/Spectator flight. Disabling Utility-managed flight resets fall distance before relinquishing `allowFlight`.

Speed values are range-validated before reaching Bukkit setters. Inventory clear explicitly covers storage, armor and offhand and writes one aggregate audit record.

## Vanish and synthetic presence

`VanishService` remains the single Utility vanish-state authority. Visibility/list state is still event-driven through plugin-aware Paper/Bukkit APIs; there is no visibility poll.

On a real state transition:

1. authoritative vanished state changes;
2. viewer visibility/list state is reconciled;
3. configured synthetic presence is broadcast to the intended audience;
4. one aggregate audit record is emitted;
5. `VanishStateChangeEvent` is fired.

Repeated `on`/`off` requests do not emit synthetic presence, events or transition audit records.

`SyntheticPresenceBridge` is presentation-only. It never creates or dispatches `PlayerJoinEvent`/`PlayerQuitEvent`. Default audience excludes the actor and viewers with `plexonutility.admin.vanish.see`.

At the 3.5 implementation point, the public PlexonChats API has no synthetic connection-message renderer/broadcaster. Therefore Utility uses the configured fallback rendered through `PlexonCore.text()`. Plugin presence may be reported diagnostically, but Utility does not compile against or reflect into PlexonChats internals.

Persisted vanished staff are loaded before viewer reconciliation. Real join/quit messages remain suppressible by `VanishListener`; synthetic leave/join is never emitted merely because the player genuinely reconnected/disconnected.

## Configuration/reload

Configuration is additive over 3.4. Existing files load with defaults for all 3.5 sections. Validation rejects out-of-range cleanup/spawn values, unsupported audiences, and the reserved 3.5 god persistence flag.

Synthetic fallback MiniMessage is validated through the shared PlexonCore text service before the runtime configuration is accepted. Reload applies candidate messages/config only after validation, then reconciles AFK, vanish, player-management and integration state.

## Local state and scheduling

PlexonUtility locally owns only cheap product-specific state: cooldowns, AFK state/timestamps, configured vanish state, the existing admin-data snapshot, Utility-managed flight/god sets, and short-lived cleanup confirmations.

3.5 adds **no permanent repeating task**. The existing single bounded AFK timeout scan remains the only regular Utility scheduler when enabled. No entity command creates one task per entity or leaves an orphan recurring task.

## Publication boundary

Source CI uses Java 25, Paper 26.2 and PlexonCore 2.0.5 and verifies tests plus final JAR contents/ownership (`/spawn` must be absent).

Stable 3.5 publication is stricter than source CI: the release workflow also requires a committed `releases/3.5.0-runtime-smoke.txt` containing real Paper 26.2/Java 25 PASS evidence. Without it, source may merge but stable publication is intentionally blocked.
