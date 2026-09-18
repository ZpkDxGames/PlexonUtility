# PlexonUtility

Core-native player utilities, quiet HUD feedback, PlexonFamily interoperability, and a bounded native staff control plane for PlexonCraft.

## 3.5.0 surface

PlexonUtility 3.5.0 remains a focused utility/admin module rather than an Essentials clone. Specialist systems keep their established ownership boundaries.

### Player commands

- `/utility`
- `/feed [player]`
- `/heal [player]`
- `/enderchest [player]` (`/ec`)
- `/workbench`
- `/trash`
- `/afk`
- `/fly [on|off] [player]`
- `/anvil`

Self-service progression permissions for `/ec`, `/fly`, `/feed`, and `/heal` default to false. Staff other-target permissions remain separate. The intended PlexonRanks contract is `plexonutility.enderchest` for Newbie X, `plexonutility.fly` for PRO I, and `plexonutility.feed` for PRO V; heal is not assigned to a rank by this release.

### Staff/admin commands

- `/utilityadmin` (`/uadmin`)
- `/invsee <player>`
- `/vanish [on|off]`
- `/kick <player> [reason...]`
- `/ban <player> [duration|perm] [reason...]`
- `/unban <player>`
- `/prison status|set|goto|send <player>|clear`
- `/killall <category|entity> [radius|world [world]]`
- `/spawnmob <entity> [amount]`
- `/gamemode <mode> [player]`
- `/god [on|off] [player]`
- `/speed <1-10|reset> [walk|fly] [player]`
- `/clearinventory [player]`

## Ownership boundaries

PlexonUtility does **not** register root `/spawn`. PlexonTravel remains authoritative for `/spawn`, `/hub`, `/back`, warps, RTP/TPA, and destination travel. PlexonHomes owns homes; PlexonChats owns chat/social presentation; PlexonBlacksmith owns repair gameplay; PlexonRanks/LuckPerms own progression/permissions.

Essentials removal is **not** implied by this release. See `docs/ESSENTIALS_DECOMMISSION_CHECKLIST.md`.

## AFK authority

AFK uses one shared Core-owned coordinator when automatic timeout is enabled. There are no per-player timers.

- `plexonutility.afk.auto.bypass` defaults false; only an explicit grant bypasses timeout.
- incoming damage does not count as player activity by default;
- outgoing player-caused damage does;
- async chat activity is serialized onto the primary/Core-owned path before visible state transitions;
- public `PlexonUtilityAPI#afkState(UUID)` exposes stable `AfkState` state/reason/activity-age information while `isAfk(UUID)` remains compatible.

## Entity cleanup safety

`/killall` supports category aliases and explicit living `EntityType` names.

Safety rules include:

- players are never selectable/removable;
- named, tamed, villager, armor-stand, display, plugin/NPC-marked entities, and non-explicit bosses are protected;
- radius is bounded and world sweeps require a separate permission;
- preview creates an immutable UUID candidate plan;
- newly spawned entities cannot enter a confirmed plan;
- each candidate is revalidated immediately before removal;
- a configurable `max-candidates` ceiling (default 10,000) fails closed before an oversized plan is accepted;
- large operations use one Core-owned coordinator with at most 40 planned candidates processed per tick;
- `plexonutility.admin.killall.bypass-confirm` is an exceptional standalone permission, defaults false, and is **not** inherited by `plexonutility.admin`.

WildStacker is an optional soft dependency. When active, Utility resolves managed living stacks through the supported WildStacker API and removes the stack object rather than blindly removing its Bukkit representative. Audit data records representative and logical amounts separately; player-facing removal counts use the logical amount. If a live WildStacker API cannot be resolved safely, the representative is protected/fails closed.

Cleanup remains direct administrative removal, not simulated combat, so Utility does not intentionally create loot, XP, or kill credit.

## Entity spawning safety

`/spawnmob` accepts living spawnable entity types only and never `PLAYER`.

- absolute request maximum: 100;
- configuration may lower but never raise the maximum;
- location search is bounded and only accepts already-loaded chunks;
- no chunk fan-out is requested;
- large operations use one Core-owned coordinator with at most 20 entity creations per tick;
- one aggregate success/failure audit is emitted.

## Player administration

Utility-managed Survival/Adventure flight has explicit ownership. A player PDC marker survives reconnect/crash so Utility can distinguish flight it granted from flight owned elsewhere. Utility-owned flight is reconciled/revoked on explicit disable, permission/rank loss, feature/reload disable, quit, plugin disable, or stale unauthorized reconnect. Creative/Spectator flight and external flight are not claimed or stripped.

God mode remains event-driven and restart-ephemeral. Speed inputs are range-validated. Other-player `/clearinventory` uses actor-specific timed confirmation plus an inventory fingerprint and revalidation; storage, armor, and offhand are included. `/invsee` retains separate view/edit permissions and authoritative live inventory mutation.

## Vanish and synthetic presence

Utility owns vanish state and never dispatches fake Bukkit `PlayerJoinEvent`/`PlayerQuitEvent`.

A real visible ↔ vanished transition flows through:

1. Utility state/visibility commit;
2. `VanishStateChangeEvent`;
3. `SyntheticPresencePresentationEvent`;
4. if an external presenter marks the request handled, Utility sends no fallback;
5. otherwise Utility renders the configured Core TextService fallback.

The presentation request exposes transition and audience context. The default ordinary audience excludes the actor and `plexonutility.admin.vanish.see` staff. Repeated same-state requests do not rebroadcast. This lets a future PlexonChats version become presentation authority without Utility depending on Chats internals.

## Configuration and persistence

Human-authored `config.yml` uses formal schema version 1. Legacy/no-schema files migrate additively; future schemas are refused. Bundled `messages.yml` is the recursive message-schema authority. Candidate preparation is side-effect-free; config/message migrations are persisted only after successful runtime-generation commit and existing files are backed up before rewrite.

`admin-data.yml` schema 2 stores the prison world UUID plus last-known name and persistent vanish UUIDs. Schema-1 data is backed up before migration. Admin-data writes are ordered, revisioned, retried with bounded backoff, expose health/revision/pending/failure state, and have bounded shutdown waiting.

Prison teleports use world UUID first, then name fallback, and `teleportAsync`; actor/target availability is revalidated on completion.

God state is intentionally ephemeral. Flight *authorization state* is not persisted as a toggle; only the Utility ownership marker is stored in player PDC so lifecycle cleanup is safe.

## Runtime generation and Core 2.1

PlexonUtility requires exact PlexonCore **2.1.0** and API 2.1. Reload follows prepare → validate → commit with rollback to the previous runtime generation if a later step fails. Core module capabilities are refreshed only for an accepted generation.

Core/module diagnostics expose the Utility runtime generation, AFK coordinator state, and AdminDataStore persistence health. Admin-data degradation or a failed runtime generation marks the Utility Core module degraded without treating optional integrations as critical failures.

## Performance invariants

- Java 25 / Paper 26.2 / exact PlexonCore 2.1.0;
- one shared AFK coordinator, not one task per player;
- one lightweight Utility-owned-flight reconciliation coordinator when flight management is enabled;
- transient cleanup/spawn work uses one coordinator per operation, never one task per entity;
- no permanent entity polling;
- no invsee polling;
- admin-data I/O uses Core-owned async I/O;
- no synchronous prison chunk fan-out;
- no unbounded shutdown wait;
- PlaceholderAPI reads remain memory-only.

## Public API/events

Public surfaces include:

- `PlexonUtilityAPI#isAfk(UUID)`
- `PlexonUtilityAPI#afkState(UUID)`
- `PlexonUtilityAPI#isVanished(UUID)`
- `PlexonUtilityAPI#isGodMode(UUID)`
- `AfkState`
- `AfkStateChangeEvent`
- `VanishStateChangeEvent`
- `SyntheticPresencePresentationEvent`

## Build and release

Canonical verification provisions the exact PlexonCore 2.1.0 JAR and validates its SHA-256:

`7ee823ded87d5be9c62426b04571c0d0d6b11c138575ca2c91838586c9f7576c`

Then CI runs:

```bash
mvn -B -ntp clean verify
```

Distribution verification checks Java class major 69, Paper 26.2 metadata, required public/admin classes, WildStacker optional integration, provided-API isolation, all discovered tests, and confirms PlexonUtility does not register root `/spawn`.

Stable publication remains intentionally blocked until the full live matrix in `docs/RUNTIME_CERTIFICATION_3.5.0.md` passes and real staging evidence exists in `releases/3.5.0-runtime-smoke.txt` with:

```text
result=PASS
paper=26.2.build.121-stable
java=25
plexoncore=2.1.0
plexoncore_sha256=7ee823ded87d5be9c62426b04571c0d0d6b11c138575ca2c91838586c9f7576c
candidate_jar_sha256=<exact tested PlexonUtility JAR>
```

The release workflow rebuilds deterministically and refuses publication if its JAR hash differs from the runtime-certified candidate.

Stable assets, once certified:

- `PlexonUtility-3.5.0.jar`
- `SHA256SUMS.txt`
- `TEST_SUMMARY.txt`
- `PROVENANCE.txt`
