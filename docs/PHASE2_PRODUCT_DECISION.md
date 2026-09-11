# Phase 2 Product Decision

Date: 2026-09-10

## Decision

**ACTIVATE** PlexonUtility as a deliberately narrow production candidate.

The repository already had a merged and published `v1.0.0` before this Phase 2 decision was requested. That historical release is preserved as the rollback baseline; its existence is not treated as runtime certification or as evidence that the product is already deployed on PlexonCraft.

## Why activation is justified

The retained commands fill small, generic convenience gaps that are not owned by another first-party Plexon product:

- `/feed [player]`
- `/heal [player]`
- `/enderchest [player]` (`/ec`)
- `/workbench`
- `/utilityadmin diagnostics|reload`

They are low-frequency, local gameplay actions with no database, no network traffic, and no scheduler requirement. The runtime cost is therefore small enough to justify a focused utility module instead of expanding an unrelated product.

## Ownership boundaries

PlexonUtility does **not** own:

- spawn, hub, back, warps — PlexonTravel
- homes — PlexonHomes
- repair/enchant/combine workflows — PlexonBlacksmith
- buying/selling — PlexonShops
- jobs/economic work progression — PlexonJobs
- skill progression — PlexonSkills
- Legendary Tools — PlexonTools
- server administration/control plane — PlexonPanel
- claim flags — PlexonGPFlags
- shared runtime infrastructure — PlexonCore
- ranks/chat/quests/crates/backpacks/spawners/keys or other dedicated product surfaces

`/repair` is intentionally absent. `/anvil`, `/hat`, `/trash`, and AFK remain deferred because no current production requirement justifies adding them to this candidate.

## Version decision

The corrective Phase 2 candidate is **`1.0.1-rc.1`**.

Patch SemVer is appropriate because the command ownership and user-facing product scope remain the same as `1.0.0`; this candidate hardens correctness, configuration/reload behavior, tests, and release evidence rather than adding a new feature family.

Rollback baseline:

- tag: `v1.0.0`
- commit: `0cc055049258b619a4d0928e0018b2e8d9999ce5`

## Phase 2 hardening in 1.0.1-rc.1

- reject invalid configuration types/ranges instead of silently coercing or clamping them;
- parse both `config.yml` and `messages.yml` completely before swapping live settings;
- reject healing dead/invalid players or invalid max-health state;
- resolve named targets through exact-name lookup;
- reject surplus command arguments deterministically;
- separate reload authorization with `plexonutility.reload`;
- remove the old `migration.claim-standard-commands` no-op setting so configuration no longer implies runtime behavior it cannot control;
- extend automated command/config/message/cooldown regression coverage;
- make CI distribution checks version-aware and verify Java 25 class major 69 plus non-shading contracts;
- publish RC evidence as JAR, `SHA256SUMS.txt`, `TEST_SUMMARY.txt`, and `PROVENANCE.txt`.

## Runtime status

Repository/source certification does not equal PlexonCraft runtime certification.

For `v1.0.1-rc.1`:

- runtime certification: **NOT EXECUTED**
- stable `v1.0.1`: **UNPUBLISHED**
- intended classification after successful exact-tag publication: **RC RELEASED / RUNTIME PENDING**

Do not merge/stable-promote solely from source CI. The candidate must enter the ecosystem batch runtime-certification stage with backup/rollback, representative command tests, restart/persistence checks, cross-plugin ownership verification, Spark/MSPT evidence, and zero HIGH/CRITICAL runtime defects.
