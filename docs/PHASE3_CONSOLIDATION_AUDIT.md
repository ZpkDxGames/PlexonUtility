# Phase 3 — Utility Consolidation Audit

Status: AUDIT CHECKPOINT — NO STABLE PROMOTION

Baseline: `phase2/1.0.1-utility-product-hardening` @ `23fe9f8eaa782d9213260a1309e9c2712610ca7f`

## Scope

PlexonUtility remains the authoritative Plexon domain for small player-state/stateless utility commands only.

Current first-party ownership:

| Feature | PlexonUtility command | Status |
|---|---|---|
| Feed | `/feed` | implemented |
| Heal | `/heal` | implemented |
| Ender chest | `/enderchest`, `/ec` | implemented |
| Workbench | `/workbench`, `/wb` | implemented |
| AFK | `/afk` | not implemented; decision gate remains closed |

No moderation, teleportation, economy, home, messaging or RTP functionality is to be added here.

## AFK decision gate

Phase 3 explicitly requires AFK only when real PlexonCraft usage or a first-party consumer is confirmed. The available production boot/config evidence does not currently establish an active `/afk` workflow, `%essentials_*afk*%` consumer, or first-party module that requires an AFK state signal.

Therefore AFK is **DEFERRED**, not missing parity.

Do not implement AFK merely for Essentials parity.

Re-open the AFK gate only if the production audit identifies at least one of:

- an active `/afk` player workflow;
- a TAB/scoreboard/chat/menu/Skript/MyCommand placeholder consumer;
- a Plexon module that requires authoritative AFK state;
- an existing LuckPerms policy depending on AFK state.

If the gate opens, the implementation contract is:

- PlexonUtility is authoritative;
- UUID-keyed online-player state only;
- monotonic last-activity timestamps;
- cheap activity updates;
- one bounded shared periodic scan, never one repeating task per player;
- no expensive per-movement processing;
- configurable automatic timeout and un-AFK triggers;
- manual `/afk`;
- bypass permission;
- optional public messages;
- cache-only PlaceholderAPI state;
- public API/event suitable for PlexonChats display consumption;
- no durable AFK state requirement across restart unless separately approved.

## Command ownership

Essentials is absent from the current runtime evidence. PlexonUtility already declares the standard utility commands directly. Before any historical Essentials aliases/permissions are removed, verify the production Paper command map, `commands.yml`, MyCommand, GUIPlus and LuckPerms export.

Canonical target ownership:

- `/feed` -> PlexonUtility
- `/heal` -> PlexonUtility
- `/enderchest`, `/ec` -> PlexonUtility
- `/workbench`, `/wb` -> PlexonUtility
- `/afk` -> unowned by PlexonUtility until the AFK decision gate opens

## Permission migration map

Only apply a source mapping when that source node actually exists in the production LuckPerms export.

| Historical/source node | Target node |
|---|---|
| `essentials.feed` | `plexonutility.feed` |
| `essentials.feed.others` | `plexonutility.feed.others` |
| `essentials.heal` | `plexonutility.heal` |
| `essentials.heal.others` | `plexonutility.heal.others` |
| `essentials.enderchest` | `plexonutility.enderchest` |
| `essentials.enderchest.others` | `plexonutility.enderchest.others` |
| `essentials.workbench` | `plexonutility.workbench` |
| `essentials.afk` | **do not migrate yet** — no Plexon AFK owner exists |

Migration procedure:

1. export LuckPerms before edits;
2. grant target nodes alongside old nodes;
3. test player and staff behavior;
4. remove old nodes only after command ownership and placeholder audits are clean;
5. retain the LuckPerms export for rollback.

## Placeholder migration

PlexonUtility intentionally has no AFK PlaceholderAPI expansion at this checkpoint because AFK itself is deferred.

Before removing the stale Essentials PlaceholderAPI expansion from production, recursively scan all current consumers for `%essentials_` including TAB/scoreboards, GUIPlus, MyCommand, Skript and other menu/config directories. A positive AFK placeholder hit re-opens the AFK implementation gate.

## Production actions requiring operator approval

- any LuckPerms mutation;
- removal of stale Essentials PlaceholderAPI expansion files;
- command alias changes;
- enabling any future AFK feature;
- server restart/reload.

No production changes are performed by this Phase 3 audit commit.
