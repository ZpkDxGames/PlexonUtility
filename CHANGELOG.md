# Changelog

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
- Deliberately deferred editable invsee and clear-inventory behavior; no freeze, mute, IP ban, punishment DB, appeals, web panel, cross-server moderation, rank/economy editor, or nickname system was added.

## 3.2.0 — 2026-09-12

- Rebuilt `/utility` as a compact 45-slot premium PlexonCraft control hub with restrained green → cyan framing and clearer visual hierarchy.
- Added detailed state-aware cards for Feed, Heal, Ender Chest, AFK, Workbench, Trash, and PlexonHomes navigation.
- Added explicit `AVAILABLE`, `NO PERMISSION`, `FEATURE DISABLED`, `INTEGRATION MISSING`, `TEMPORARILY UNAVAILABLE`, and `ADMIN ONLY` presentation rules.
- Added a live player profile summary with AFK state and available-utility count.
- Added dedicated PlexonFamily, Help, and admin Diagnostics submenus with consistent Back/Refresh/Close navigation.
- Preserved PlexonHomes ownership and the `plexonhomes.limit.<N>` / `plexonhomes.limit.unlimited` contract without duplicating home-limit logic.
- Preserved the 3.1 quiet-feedback AFK bossbar/actionbar/social model and kept Trash as a writable non-Core navigation inventory.
- Kept all GUI routing on `PlexonCore.gui()` and all MiniMessage/template rendering on `PlexonCore.text()` with no new scheduler, poller, parser, or inventory listener.
- Added deterministic availability-state regression tests and required the new menu model in canonical build/release distribution verification.

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
- Documented the existing rank-friendly `plexonhomes.limit.<N>` and `plexonhomes.limit.unlimited` contract for PlexonRanks/LuckPerms-driven slot progression.
- Added lifecycle-driven PlexonFamily discovery through `PlexonCore.integrations()` with no polling scheduler.
- Added `/utilityadmin family` and expanded diagnostics with family/Homes readiness.
- Added public `AfkStateChangeEvent` so PlexonFamily modules can react to manual, timeout, and activity AFK transitions without polling.
- Kept `%plexonutility_afk%` and `%plexonutility_is_afk%` PlaceholderAPI outputs for TAB and other display plugins.
- Fixed old `messages.yml` upgrades: newly required keys are now copied from bundled defaults, persisted, and existing custom values remain untouched.
- Added static MiniMessage component caching between reloads and retained safe runtime value insertion through PlexonCore `TextService` tag resolvers.
- Moved the compile boundary to stable PlexonCore `2.0.5` and adopted owner-aware module state updates/cleanup.
- Hardened CI/release verification for the new interoperability/event classes and immutable PlexonCore 2.0.5 artifact.
- Added regression coverage for persisted message migration behavior and the PlexonFamily compatibility catalog.

## 2.0.0 — 2026-09-11

- Promoted PlexonUtility into a fuller Core-native utility surface while preserving specialist-plugin ownership boundaries.
- Added `/utility`, a compact Plexon-style navigation hub backed by `PlexonCore.gui()` rather than a plugin-local inventory router.
- Added `/trash`, a lightweight disposable 27-slot inventory with no persistence or scheduler cost.
- Migrated configurable MiniMessage rendering to `PlexonCore.text()` and adopted the green→cyan PlexonUtility gradient.
- Added MiniMessage validation at startup/reload and safe component insertion for runtime placeholder values.
- Routed AFK async-to-primary notifications through `PlexonCore.scheduler()` while retaining one shared O(online players) timeout scan.
- Expanded the Core module capability declaration for shared text, GUI, scheduler and complement diagnostics.
- Added external complement detection for claims/regions, CoreProtect, LuckPerms, menu builders, anti-cheat, display/HUD, spark/Chunky and proximity voice.
- Published complement states into `PlexonCore.integrations()` so ecosystem diagnostics share one control-plane view.
- Expanded `/utilityadmin diagnostics` with Core health, GUI sessions and scheduler queue visibility and added `/utilityadmin integrations`.
- Updated release verification to require the new 2.0 runtime classes and command descriptors.
- Added regression coverage for complement detection and retained existing command/config/cooldown/AFK/message/placeholder tests.

## 1.0.1 — 2026-09-11

- Promoted the accepted `1.0.1-rc.2` source line to stable `1.0.1` without broad product changes.
- Retained the deliberately narrow utility ownership boundary: feed, heal, ender chest, workbench, AFK, diagnostics/reload, API, and AFK placeholders.
- Retained the bounded AFK runtime with thread-safe in-memory state, block-coordinate activity filtering, and one shared scheduler.
- Kept strict configuration validation, transactional candidate loading, exact target lookup, heal safety checks, and expanded regression coverage.
- Updated Build CI so exact `main` commits are verified instead of being excluded.
- Replaced version-specific publication workflows with one generic exact-`main` stable release workflow.
- Added stable artifact verification for Java 25 bytecode, Paper 26.2 metadata, required classes/resources, provided-API isolation, test execution, and SHA-256 output.

## 1.0.1-rc.1 — 2026-09-10

- Recorded the Phase 2 product decision to keep PlexonUtility active with a deliberately narrow ownership boundary.
- Added strict configuration type/range validation and transactional config/message candidate loading.
- Added dead/invalid-player and invalid max-health protection for `/heal`.
- Switched named player targeting to exact-name lookup and deterministic argument-count handling.
- Added explicit `plexonutility.reload` authorization.
- Removed the misleading no-op `migration.claim-standard-commands` setting.
- Expanded regression coverage for utility commands, configuration, messages, and cooldown behavior.

## 1.0.0 — 2026-09-09

- Initial PlexonCore 2.0-native release.
- Added `/feed`, `/heal`, `/enderchest`/`/ec`, and `/workbench`.
- Added explicit self/others/cooldown permissions.
- Added immutable validated configuration and configurable MiniMessage feedback.
- Added public `PlexonUtilityAPI` feature service.
- Added Core module registration and diagnostics.
- Added monotonic in-memory cooldowns with quit cleanup.
- Added reproducible Java 25 / Paper 26.2 CI and immutable release packaging.
