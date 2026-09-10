# PlexonUtility

Core-native, intentionally small player utility module for PlexonCraft.

## Phase 2 candidate

Current source candidate: `1.0.1-rc.1`.

PlexonUtility owns only:

- `/feed [player]`
- `/heal [player]`
- `/enderchest [player]` (`/ec`)
- `/workbench`
- `/utilityadmin diagnostics`
- `/utilityadmin reload`

It does **not** own homes, spawn/hub/back/warps, repair/enchant/combine, chat, ranks, economy, jobs, skills, kits, mail, moderation, vanish, claim flags, server control, or other dedicated Plexon product responsibilities.

AFK, `/anvil`, `/hat`, and `/trash` remain deferred because the current product audit did not establish enough production value to justify expanding the runtime surface.

See `docs/PHASE2_PRODUCT_DECISION.md` for the ownership review and activation rationale.

## Runtime

- Paper `26.2`
- Java `25`
- PlexonCore `2.0.0` Runtime API (`depend: PlexonCore`)
- No database
- No repeating scheduler
- In-memory monotonic cooldowns only

## Correctness policy

- `/heal` uses the player's actual max-health attribute and rejects dead/invalid player state.
- named target forms use exact online-player lookup and separate `.others` permissions.
- configuration types and numeric ranges are validated strictly.
- reload parses and validates both configuration files before applying either candidate.
- failed reloads retain the previous known-good runtime state.

## Build

CI downloads the immutable `PlexonCore-2.0.0.jar`, verifies its pinned SHA-256, installs it into the CI-local Maven repository, then runs:

```bash
mvn -B -ntp clean verify
```

The candidate distribution is `target/PlexonUtility-1.0.1-rc.1.jar`. Core, Paper/Bukkit, PlaceholderAPI, and Adventure runtime classes must not be shaded into the JAR. CI also verifies Java class major `69` and all required plugin resources.

## Permissions

```text
plexonutility.feed
plexonutility.feed.others
plexonutility.feed.cooldown.bypass
plexonutility.heal
plexonutility.heal.others
plexonutility.heal.cooldown.bypass
plexonutility.enderchest
plexonutility.enderchest.others
plexonutility.workbench
plexonutility.admin
plexonutility.reload
```

## Release boundary

`v1.0.0` is the existing rollback baseline. `v1.0.1-rc.1` is a prerelease candidate only; stable `v1.0.1` must remain unpublished until PlexonCraft batch runtime certification succeeds.

The RC release must contain:

- `PlexonUtility-1.0.1-rc.1.jar`
- `SHA256SUMS.txt`
- `TEST_SUMMARY.txt`
- `PROVENANCE.txt`

## Essentials migration safety

PlexonUtility replacing these four utility commands does **not** prove Essentials can be uninstalled. Before full removal, audit the live server command map, active Vault economy provider/balance storage, Essentials-dependent plugins, kits/mail/nickname data, and PlexonHomes/PlexonTravel PRIMARY state. See `docs/ESSENTIALS_FEATURE_AUDIT.md` and `docs/ESSENTIALS_DECOMMISSION_CHECKLIST.md`.
