# PlexonUtility 3.5 Architecture

## Product boundary

PlexonUtility owns generic player conveniences and a bounded administrative control plane. It does not absorb specialist Plexon Family domains. PlexonTravel remains authoritative for root `/spawn` and travel; PlexonHomes owns homes; PlexonChats owns chat/social presentation; PlexonBlacksmith owns repair gameplay; PlexonRanks/LuckPerms own progression/permission assignment.

Entity creation is `/spawnmob`. PlexonUtility never registers root `/spawn`.

## PlexonCore 2.1 authority

PlexonUtility requires exact PlexonCore 2.1.0 and Core API 2.1. The module descriptor advertises `>=2.1 <3.0`, but startup additionally verifies the running Core plugin version is exactly `2.1.0`.

Core remains authoritative for:

- MiniMessage/text rendering;
- protected GUI/session lifecycle;
- owner-scoped scheduling and async I/O;
- integration state;
- module lifecycle/health.

Utility capabilities are rebuilt after a successful runtime generation instead of remaining startup-only.

## Runtime generation

Configuration reload is generation-oriented:

```text
prepare config candidate
prepare message candidate
validate all candidate state
apply candidate runtime
reconcile services
refresh Core capabilities
persist required config/message migrations transactionally
publish generation
```

If a later step fails, the prior config/message generation and service state are restored. A reload failure remains visible through Utility's Core module state until a later generation commits successfully.

`config.yml` schema 1 and bundled `messages.yml` defaults are the respective schema authorities. Candidate preparation does not mutate disk. Existing files are backed up before committed migration rewrites; future config schemas are refused.

## AFK

`AfkTracker` owns in-memory AFK/activity state. `AfkManager` adapts Bukkit/Paper events.

Automatic timeout uses one `SharedScheduler` coordinator. It self-reschedules through `CoreScheduler.schedulePrimary(plugin,...)`, so every pending scan is owner-scoped and purgeable by Core. No per-player timer exists.

Incoming ordinary damage is not activity. Outgoing player-caused damage is activity. Async chat is marshalled to the primary/Core path before applying an observable transition.

The public API exposes `AfkState` with active/reason/activity-age semantics; raw monotonic timestamps are not part of the public contract.

## Flight and god state

`PlayerManagementService` owns Utility flight authorization and event-driven god mode.

For Survival/Adventure flight, Utility stores a PDC ownership marker when it grants flight. This marker lets reconnect/restart reconciliation distinguish Utility-owned flight from flight another plugin or game mode owns. Utility-owned flight is revoked on explicit disable, permission loss, feature disable/reload, quit, plugin disable, or unauthorized stale-marker reconnect. Creative/Spectator flight is never stripped.

A single lightweight owner-scoped reconciliation coordinator inspects only Utility-owned flyers; there is no per-player recurring task.

God mode is deliberately restart-ephemeral and event-driven. Void damage remains authoritative.

## Entity cleanup

`EntityCleanupService` separates preview planning from destructive execution.

A cleanup plan contains:

- immutable query/scope;
- exact candidate UUID set;
- matched/protected counts.

Planning fails closed once configured `admin.entity-management.killall.max-candidates` is exceeded (default 10,000). Confirmation is actor-specific and expires. Only planned UUIDs can be removed; every surviving candidate is revalidated for selector, scope, and protection immediately before removal.

Large plans use one Core-owned coordinator and process at most 40 planned candidates per tick. No task is created per entity. One aggregate audit is emitted after completion.

Players are unconditionally protected. Named/tamed/villager/armor-stand/display/plugin-NPC-marked entities and non-explicit bosses use conservative protection policy.

### WildStacker

WildStacker is a soft dependency. `WildStackerAdapter` uses the supported API reflectively so Utility has no hard binary dependency:

- `WildStackerAPI.getStackedEntity(LivingEntity)`;
- `StackedEntity#getStackAmount()`;
- `StackedEntity#remove()`.

Managed representatives are removed through WildStacker, not raw Bukkit removal. The result tracks Bukkit representatives removed and logical mob count separately. If WildStacker is active but its API cannot be resolved safely, the living representative fails closed/protected.

Cleanup never simulates combat, loot, XP, or kill credit.

## Entity spawning

`EntitySpawnService` accepts only living spawnable types and enforces an absolute request maximum of 100. Safe-location discovery checks a bounded nearby set and requires chunks to already be loaded.

Production spawning uses one Core-owned coordinator and creates at most 20 entities per tick. There is no task-per-entity fan-out and no requested chunk generation.

## AdminDataStore and prison

`admin-data.yml` schema 2 stores:

- prison world UUID;
- last-known prison world name;
- prison coordinates/rotation;
- persistent vanished UUIDs.

Schema 1 is backed up before migration. Persistence uses ordered Core I/O, monotonically increasing revisions, full-snapshot writes, bounded retries/backoff, observable durable results, dirty/pending/failure health, and a bounded shutdown wait. A later successful full snapshot can recover a previously degraded writer.

Prison resolves world UUID first and name second. Teleports use `teleportAsync`, then revalidate actor/target availability before success/audit reporting.

## Vanish and presentation ownership

`VanishService` is the Utility vanish-state authority. Visibility/list changes are plugin-aware and event-driven.

A real transition is:

```text
state + visibility commit
→ VanishStateChangeEvent
→ SyntheticPresencePresentationEvent
→ external presenter handled?
     yes: no Utility fallback
     no:  Core TextService fallback
→ aggregate audit
```

No fake Bukkit `PlayerJoinEvent` or `PlayerQuitEvent` is dispatched. Repeating the same state produces no presentation transition. The default ordinary audience excludes the actor and vanish-aware staff.

The presentation hook is intentionally additive so PlexonChats can later render/broadcast the synthetic connection message without Utility depending on Chats internals.

## Inventory administration

`/invsee` preserves the 3.4 live PlayerInventory semantics: separate view/edit permissions, storage/armor/offhand mapping, exact ItemStack metadata/components, disconnect safety, multi-view refresh, and blocked unsupported transfers.

Other-player `/clearinventory` uses timed actor-specific confirmation and an inventory fingerprint. If the target inventory changes after preview, confirmation is invalidated and a new preview is required.

## Core health

The Utility module's Core state aggregates product-level critical conditions without treating optional integrations as critical:

- sticky failed runtime-generation/rollback state;
- AdminDataStore degraded persistence;
- expected AFK coordinator missing/duplicated.

Core's own scheduler health remains independently visible as a Core 2.1 contributor. Utility diagnostics expose runtime generation, AFK coordinator count, and admin-data revision/dirty/pending/retry/failure state.

## Scheduling/performance invariants

Allowed recurring/coordinated work is deliberately small and owner-scoped:

- one AFK timeout coordinator when automatic AFK is enabled;
- one flight reconciliation coordinator when relevant;
- transient one-coordinator cleanup batches;
- transient one-coordinator spawn batches.

There is no per-player AFK task, per-entity cleanup/spawn task, permanent entity polling, invsee polling, plugin-local executor pool, synchronous prison chunk fan-out, or unbounded shutdown join.

## Publication boundary

Source verification uses Java 25, Paper API `26.2.build.121-stable`, and exact PlexonCore 2.1.0 with Core JAR SHA-256:

`7ee823ded87d5be9c62426b04571c0d0d6b11c138575ca2c91838586c9f7576c`

Stable publication additionally requires real runtime evidence in `releases/3.5.0-runtime-smoke.txt` bound to the exact candidate Utility JAR. The release workflow rebuilds and compares SHA-256; a hash mismatch or missing PASS evidence blocks publication.
