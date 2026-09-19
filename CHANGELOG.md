# Changelog

## 3.5.0 — 2026-09-18

- Upgraded the exact runtime/compile/release boundary to PlexonCore 2.1.0 and moved Utility scheduling/I/O onto owner-scoped Core primitives.
- Corrected AFK authority: automatic bypass now defaults false, incoming damage no longer resets AFK, async activity is serialized safely, and public `AfkState` / `afkState(UUID)` supports external consumers.
- Locked the established rank-facing self permissions: `plexonutility.enderchest`, `plexonutility.fly`, `plexonutility.feed`, and `plexonutility.heal` default false; self `/fly` uses the progression node rather than a staff node.
- Hardened Utility-owned Survival/Adventure flight with a durable PDC ownership marker and reconciliation on permission loss, reload/feature disable, quit, reconnect, and plugin shutdown without stripping Creative/Spectator or external flight.
- Added formal config schema version 1 and recursive bundled-message schema migration. Candidate loading is side-effect-free; committed migrations use backups and transactional multi-file persistence.
- Added runtime generation/rollback semantics and Core capability refresh so rejected reloads restore the previous accepted state instead of leaving mixed config/message/service generations.
- Hardened `/killall` with exact UUID candidate plans, revalidation at execution, actor-specific one-shot confirmation, a default 10,000-candidate preview ceiling, and a dangerous bypass permission that is no longer inherited by the admin umbrella.
- Added optional fail-closed WildStacker stack-aware cleanup; logical stack amounts are reported separately from Bukkit representative removals.
- Bounded large cleanup and `/spawnmob` bursts through one Core-owned coordinator per operation (40 cleanup candidates/tick, 20 entity creations/tick) without per-entity task fan-out or chunk loading.
- Preserved PlexonTravel ownership of root `/spawn`; PlexonUtility registers `/spawnmob` only and CI rejects an accidental Utility `/spawn`.
- Hardened other-player `/clearinventory` confirmation with inventory fingerprint/revalidation while preserving 3.4 live `/invsee` edit semantics.
- Upgraded `admin-data.yml` to schema 2 with pre-migration backup, prison world UUID + name fallback, revisioned/retryable observable persistence, recovery after later full-snapshot success, and bounded shutdown waiting.
- Changed prison set/clear reporting to require durable persistence and moved prison teleports to `teleportAsync` with actor/target completion revalidation.
- Added public `SyntheticPresencePresentationEvent` after `VanishStateChangeEvent`; external presenters such as future PlexonChats versions can handle synthetic presence without fake Bukkit join/quit events, with Utility fallback only when unhandled.
- Added Utility module health publication for sticky runtime-generation failures, AdminDataStore degradation, and AFK coordinator state, plus expanded `/utilityadmin diagnostics`.
- Expanded CI/regression coverage for permission descriptors, immutable cleanup plans, preview ceilings, batching, WildStacker fail-closed behavior, flight lifecycle, migrations, persistence retry/recovery/bounded close, prison async races, and presentation handoff.
- Promoted 3.5.0 to the final-only full stable release model: source CI, distribution verification, and byte-identical double-build reproducibility are publication gates; live Paper validation is tracked separately after release rather than as a prerelease channel.

## 3.4.0 — 2026-09-12

- Upgraded `/invsee` from snapshot-only viewing to a permission-separated live inventory editor.
- Added `plexonutility.admin.invsee.edit`; view-only authority remains separate.
- Added safe cursor-based edits for storage, armor, and offhand with immediate authoritative writes, multi-view refresh, logout cleanup, and per-edit audit records.
- Cancelled unsupported vanilla transfer gestures that could otherwise mutate the mirror without an equivalent target-inventory write.
- Preserved the rest of the 3.3 native admin toolkit and runtime boundary unchanged.

## 3.3.0 — 2026-09-12

- Added a dedicated 45-slot Admin Center reachable from `/utility` and `/utilityadmin`, while preserving text diagnostics/family/integrations/reload subcommands and adding `/uadmin`.
- Added deterministic paginated online-player selection and a focused Player Actions GUI with info, read-only inventory inspection, Ender Chest, heal, feed, administrative teleport/bring, prison send, kick, and ban.
- Added `/invsee <player>` as an online-only, exact-name, protected read-only snapshot including storage, armor, and offhand; displayed stacks are cloned and target logout closes stale inspectors safely.
- Added native event-driven `/vanish [on|off]` using Paper plugin-aware hide/show and player-list APIs, granular `vanish.see`, optional persistence, join/quit suppression, reconnect/viewer reconciliation, and shutdown cleanup with no polling task.
- Added native `/kick`, profile `/ban`, and `/unban` backed by modern Paper/Bukkit authority instead of a private punishment database; cached/known offline lookup avoids blocking profile web requests.
- Added bounded ban durations (`m`/`h`/`d` plus permanent aliases), sanitized 160-character reasons, actor/source recording, Adventure kick components, GUI typed input, and explicit final confirmations.
- Added `/prison status|set|goto|send|clear` as a single configurable holding waypoint, deliberately excluding jail/sentence/punishment-history behavior.
- Added `admin-data.yml` for schema-validated prison and optional vanish state, with Core IO-lane persistence and atomic/recoverable file replacement on state changes only.
- Added granular admin permissions, server-log audit records for destructive/state-changing actions, and destructive self-action protection in the GUI.
- Added `%plexonutility_vanished%` and a binary-compatible `PlexonUtilityAPI#isVanished(UUID)` default method.
- Extracted reusable feed/heal/Ender Chest business logic so the admin GUI invokes services directly instead of issuing new internal admin commands.
- Added a shared Utility menu item factory that forces non-italic names/lore and hides irrelevant attributes while keeping runtime values on safe Core template insertion.
- Documented the native-vanish migration from old SuperVanish/TAB assumptions and preserved PlexonHomes/PlexonTravel/PlexonChats/PlexonRanks/PlexonBlacksmith ownership boundaries.
- Expanded regression coverage for duration parsing, admin config/model behavior, inventory clone isolation, vanish authorization/persistence, prison coordinate fidelity, and moderation reason validation.

## 3.2.0 — 2026-09-12

- Rebuilt `/utility` as a compact 45-slot premium PlexonCraft control hub with restrained green → cyan framing and clearer visual hierarchy.
- Added detailed state-aware cards for Feed, Heal, Ender Chest, AFK, Workbench, Trash, and PlexonHomes navigation.
- Added explicit `AVAILABLE`, `NO PERMISSION`, `FEATURE DISABLED`, `INTEGRATION MISSING`, `TEMPORARILY UNAVAILABLE`, and `ADMIN ONLY` presentation rules.
- Added a live player profile summary with AFK state and available-utility count.
- Added dedicated PlexonFamily, Help, and admin Diagnostics submenus with consistent Back/Refresh/Close navigation.
- Preserved PlexonHomes ownership and the `plexonhomes.limit.<N>` / `plexonhomes.limit.unlimited` contract without duplicating home-limit logic.
- Preserved the 3.1 quiet-feedback AFK bossbar/actionbar/social model and kept Trash as a writable non-Core navigation inventory.
- Kept all GUI routing on `PlexonCore.gui()` and all MiniMessage/template rendering on `PlexonCore.text()` with no new scheduler, poller, parser, or inventory listener.

## 3.1.0 — 2026-09-12

- Added a centralized quiet-feedback presentation service with no scheduler or per-player polling.
- AFK self-state now uses a persistent bossbar instead of chat; returning active uses a short actionbar.
- AFK social broadcasts are compact, prefixless by default, and exclude the player whose state changed.
- Added short-return suppression so rapid AFK toggles do not create repeated public "is back" lines.
- Routed successful self `/feed`, `/heal`, and `/trash` feedback to actionbar by default while keeping errors/admin diagnostics in chat.
- Added configurable bossbar color/overlay, social-prefix behavior, actionbar policy, and AFK short-return suppression window.
- Preserved safe MiniMessage rendering through `PlexonCore.text()` and added prefixless component caching for HUD/social surfaces.
- Added lifecycle cleanup/reconciliation for AFK bossbars on activity, quit, reload, feature disable, and plugin shutdown.
- Published the `quiet-feedback` capability through the PlexonCore module registry for ecosystem diagnostics and compatibility awareness.

## 3.0.0 — 2026-09-11

- Redesigned `/utility` into a 4-row Plexon-style hub with live player/AFK state, permission-aware actions, PlexonFamily status, refresh/close controls, and admin diagnostics.
- Added optional PlexonHomes integration to the Utility hub while preserving PlexonHomes as the sole home/data/teleport authority.
- Added public `AfkStateChangeEvent`, shared Core text/GUI/scheduler/integration services, module diagnostics, and stricter configuration/message migration validation.
- Moved the compile boundary to stable PlexonCore `2.0.5` and hardened CI/release verification.

## 2.0.0 — 2026-09-11

- Promoted PlexonUtility into a fuller Core-native utility surface while preserving specialist-plugin ownership boundaries.
- Added `/utility`, `/trash`, Core-native MiniMessage/GUI/scheduler/integration use, and expanded diagnostics/complement discovery.
- Retained the bounded AFK runtime and provided-API isolation.

## 1.0.1 — 2026-09-11

- Promoted the accepted `1.0.1-rc.2` source line to stable `1.0.1` without broad product changes.
- Retained the deliberately narrow utility ownership boundary, strict validation, shared AFK scheduler, reproducible Java 25/Paper 26.2 CI, and immutable release packaging.

## 1.0.1-rc.1 — 2026-09-10

- Recorded the Phase 2 product decision to keep PlexonUtility active with a deliberately narrow ownership boundary.
- Added strict configuration type/range validation, transactional candidate loading, heal safety checks, exact player targeting, reload authorization, and expanded tests.

## 1.0.0 — 2026-09-09

- Initial PlexonCore 2.0-native release.
- Added `/feed`, `/heal`, `/enderchest`/`/ec`, `/workbench`, AFK, API, placeholders, validated configuration, Core module registration, diagnostics, cooldowns, and Java 25/Paper 26.2 CI.
