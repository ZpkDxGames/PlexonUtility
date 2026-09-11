# Phase 3 — PlexonUtility AFK Implementation

Status: IMPLEMENTED ON PHASE 3 BRANCH — NO PRODUCTION CUTOVER / NO STABLE PROMOTION

## Why AFK exists now

The live PlexonCraft audit found real TAB consumers of `%essentials_afk%` in tab suffix, nametag suffix and TAB placeholder conditions. The Master Coordinator approved the AFK gate. PlexonUtility is therefore the authoritative AFK state owner.

## Runtime architecture

AFK state is UUID-keyed and ephemeral. It resets to ACTIVE after plugin/server restart.

Activity path:

```text
meaningful event
  -> constant-time config check
  -> AfkTracker atomic timestamp update
  -> only on an AFK -> ACTIVE transition: player feedback / optional announcement
```

Automatic timeout path:

```text
one shared repeating task
  -> every afk.auto-timeout.scan-interval-seconds
  -> iterate online players once
  -> skip plexonutility.afk.auto.bypass
  -> compare monotonic last-activity timestamp
  -> only transition eligible players to AFK
```

Complexity is O(online players) per configured scan. There is no repeating task per player, no timer object per player, no AFK database and no filesystem I/O on the activity path.

## Core player-watch decision

PlexonCore's current shared player-watch service is designed for short-lived opt-in watches such as teleport warmups. It dispatches exact positional movement to registered watches. AFK requires long-lived tracking for essentially every online player and only needs block-level movement. Registering all online players with the Core watch would increase movement dispatch work without removing the AFK-wide scan.

Therefore Phase 3 uses a Utility-local constant-time block-coordinate filter and does not modify PlexonCore. No Core change is required for correctness or performance.

A future optional Core facility could expose aggregated/coarsened player activity timestamps for long-lived consumers, but AFK does not depend on such an API.

## Listener inventory and expected hot-path cost

| Event | Purpose | Cost before state update |
|---|---|---|
| `PlayerMoveEvent` | meaningful movement | config boolean + world/block-coordinate comparison; yaw/pitch-only and same-block movement return immediately |
| `AsyncChatEvent` | chat activity | config boolean + atomic state update; main-thread task is created only when an AFK player transitions ACTIVE |
| `PlayerCommandPreprocessEvent` | command activity | config boolean + small `/afk` exclusion check |
| `PlayerInteractEvent` | block/air interaction | config boolean + atomic state update |
| `PlayerInteractEntityEvent` | entity interaction | config boolean + atomic state update |
| `InventoryClickEvent` | inventory activity | config boolean + player type check + atomic state update |
| `BlockBreakEvent` / `BlockPlaceEvent` | committed block changes | config boolean + atomic state update |
| `EntityDamageEvent` | received/dealt combat damage | config boolean + player checks + atomic state update |
| join / quit | initialize/remove ephemeral state | one map mutation |

No listener performs database or filesystem I/O.

## Command

`/afk`

- player-only;
- permission `plexonutility.afk`;
- no arguments;
- ACTIVE -> AFK;
- AFK -> ACTIVE;
- no invented moderation/admin target mode.

`/afk` is excluded from command-activity reset so an AFK player invoking it correctly toggles directly to ACTIVE rather than being cleared and immediately toggled back to AFK.

## Permissions

- `plexonutility.afk` — manual toggle, default true.
- `plexonutility.afk.auto.bypass` — bypass automatic timeout, default op.

Historical `essentials.afk` may be migrated to `plexonutility.afk` only after the operator verifies the old node exists in the LuckPerms export.

## Configuration

Default policy:

```yaml
afk:
  auto-timeout:
    enabled: true
    seconds: 300
    scan-interval-seconds: 10
  announcements:
    enabled: true
  reset-on:
    movement: true
    chat: true
    command: true
    interaction: true
    block-change: true
    damage: true
  placeholder:
    active: ''
    afk: ' <gray>[AFK]</gray>'
```

Existing PlexonUtility configs inherit these parser defaults. On deployment, Bukkit's default-copy migration adds missing bundled keys while preserving configured values.

## PlaceholderAPI

Native expansion identifier: `plexonutility`.

- `%plexonutility_afk%` — configurable display-safe suffix; default empty while ACTIVE and ` <gray>[AFK]</gray>` while AFK.
- `%plexonutility_is_afk%` — raw `true` / `false` state.

The expansion is optional and registers only when PlaceholderAPI is installed. It is not shaded into the JAR.

## Exact TAB AFK cutover

Replace every live:

```text
%essentials_afk%
```

with:

```text
%plexonutility_afk%
```

This applies to the confirmed `tabsuffix`, `tagsuffix` and AFK formatting/condition consumer.

For conditions that require a raw boolean instead of the display suffix, use:

```text
%plexonutility_is_afk%
```

No production TAB file is changed by this branch.

## Remaining Essentials placeholder cutover

### Vanish

TAB already provides the internal `%vanished%` placeholder (`true` / `false`) and SuperVanish also exposes `%supervanish_isvanished%` when its PlaceholderAPI hook is enabled.

For TAB configuration itself, prefer `%vanished%` because TAB is the direct consumer and already owns its vanish-aware presentation layer. `%supervanish_isvanished%` is the direct provider alternative when a PlaceholderAPI-form placeholder is specifically required.

### Nickname

No active nickname authority was proven after Essentials was removed. Do not create a nickname system in PlexonUtility.

For the current TAB nickname comparison, the safe no-nickname cutover is to use the canonical `%player%` name and remove/simplify the Essentials nickname-difference condition. TAB's `%displayname%` is available if the product intentionally wants the server display-name value, but it is not guaranteed to be semantically identical to an Essentials nickname and should not be substituted blindly.

`Expansion-essentials.jar` remains operator-blocked until AFK, vanish and nickname references have all been migrated.

## Public API

`PlexonUtilityAPI#isAfk(UUID)` provides a minimal read-only AFK contract for future PlexonChats presentation. PlexonChats remains non-authoritative and requires no application change in this implementation.

## Runtime validation plan

Before the Essentials expansion is removed:

1. install the future approved PlexonUtility build in staging/production maintenance window;
2. confirm startup reports AFK enabled and exactly one AFK shared scheduler;
3. confirm PlaceholderAPI registers `plexonutility`;
4. test `%plexonutility_afk%` and `%plexonutility_is_afk%` ACTIVE state;
5. run `/afk` and verify AFK state and optional public announcement;
6. perform same-block yaw/pitch movement and verify it does not cause expensive AFK churn;
7. cross a block boundary and verify AFK clears;
8. test chat, non-AFK command, interaction, block change and damage resets;
9. verify `/afk` itself toggles AFK -> ACTIVE correctly;
10. grant `plexonutility.afk.auto.bypass` to a test identity and verify no automatic timeout;
11. remove bypass and verify automatic timeout after configured period;
12. reload repeatedly and verify diagnostics never reports more than one AFK scheduler;
13. disconnect/reconnect and verify state starts ACTIVE;
14. restart and verify AFK state is intentionally not persisted;
15. update TAB placeholders only after the Plexon placeholder is confirmed live.

No production configuration or plugin removal is part of this source implementation.
