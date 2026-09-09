# PlexonUtility

Core-native, intentionally small player utility module for PlexonCraft.

## 1.0.0 scope

PlexonUtility 1.0.0 owns only:

- `/feed [player]`
- `/heal [player]`
- `/enderchest [player]` (`/ec`)
- `/workbench`
- `/utilityadmin diagnostics`
- `/utilityadmin reload`

It does **not** own homes, travel/teleports, chat, ranks, economy, jobs, skills, kits, mail, moderation, vanish, repair gameplay, or other Essentials-sized responsibilities.

AFK is intentionally deferred in 1.0.0 because repository-only implementation did not establish a production consumer/dependency requiring a first-party AFK signal.

## Runtime

- Paper `26.2`
- Java `25`
- PlexonCore `2.0.0` Runtime API (`depend: PlexonCore`)
- No database
- No repeating tasks in the 1.0.0 runtime

## Build

CI downloads the immutable `PlexonCore-2.0.0.jar`, verifies its pinned SHA-256, installs it into the CI-local Maven repository, then runs:

```bash
mvn -B -ntp clean verify
```

The distribution JAR is `target/PlexonUtility-1.0.0.jar`. Core and Paper runtime classes are provided dependencies and are rejected if bundled into the artifact.

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
```

## Essentials migration safety

PlexonUtility replacing these four utility commands does **not** prove Essentials can be uninstalled. Before full removal, audit the live server command map, active Vault economy provider/balance storage, Essentials-dependent plugins, kits/mail/nickname data, and PlexonHomes/PlexonTravel PRIMARY state. See `docs/ESSENTIALS_FEATURE_AUDIT.md` and `docs/ESSENTIALS_DECOMMISSION_CHECKLIST.md`.
