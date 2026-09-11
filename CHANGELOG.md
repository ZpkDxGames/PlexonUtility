# Changelog

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
- Hardened CI distribution verification and RC release evidence generation.
- Stable `v1.0.1` remained unpublished at this checkpoint.

## 1.0.0 — 2026-09-09

- Initial PlexonCore 2.0-native release.
- Added `/feed`, `/heal`, `/enderchest`/`/ec`, and `/workbench`.
- Added explicit self/others/cooldown permissions.
- Added immutable validated configuration and configurable MiniMessage feedback.
- Added public `PlexonUtilityAPI` feature service.
- Added Core module registration and diagnostics.
- Added monotonic in-memory cooldowns with quit cleanup.
- Added reproducible Java 25 / Paper 26.2 CI and immutable release packaging.
- Intentionally omitted AFK, persistence, optional utility expansion, and non-utility Essentials responsibilities pending production evidence.
