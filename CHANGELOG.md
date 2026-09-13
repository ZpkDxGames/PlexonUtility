# Changelog

## 3.5.0 — 2026-09-12

- Added bounded `/killall <category|entity> [radius|world [world]]` with player exclusion, conservative named/tamed/villager/armor-stand/display/plugin protections, explicit boss targeting, aggregate audit, and short-lived actor-specific confirmation for large removals.
- Added `/spawnmob <entity> [amount]` with Paper spawnability validation, safe already-loaded nearby placement, configurable blocked types, and an absolute maximum of 100 entities.
- Preserved PlexonTravel ownership of root `/spawn`; PlexonUtility registers `/spawnmob` only, and CI now rejects an accidental Utility `/spawn` registration.
- Added permission-separated `/gamemode`, `/fly`, `/god`, `/speed`, and `/clearinventory`; Creative/Spectator flight is preserved, god mode is event-driven/restart-ephemeral, and clear inventory covers storage, armor, and offhand.
- Added native `/anvil` while retaining vanilla anvil rules and costs.
- Added transition-only synthetic leave/join presentation for native vanish with ordinary-player audience semantics, no fake Bukkit lifecycle events, no repeated broadcasts for unchanged state, and public `VanishStateChangeEvent` integration.
- Kept PlexonChats as the preferred connection-message presentation authority; because its current public API has no synthetic connection renderer/broadcaster, 3.5.0 uses a configurable PlexonCore/MiniMessage fallback without depending on PlexonChats internals.
- Added conditional Core capabilities for entity cleanup/spawn, player administration, flight/god/speed/inventory control, synthetic presence, and vanish events.
- Preserved the existing `admin-data.yml` schema; optional god persistence was deliberately not introduced in 3.5.0.
- Added focused regression coverage for entity selectors/protections/spawn limits, player control state, additive config compatibility, synthetic vanish audiences, and real transition-only behavior.
- Updated Java 25 / Paper 26.2 CI and distribution verification for the 3.5.0 command/class boundary.
- Added a hard stable-release runtime gate: publication requires committed real Paper 26.2/Java 25 smoke evidence before `v3.5.0` can be created.

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
