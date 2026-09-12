# PlexonUtility

Core-native player utilities, quiet HUD feedback, PlexonFamily interoperability, and ecosystem diagnostics for PlexonCraft.

## 3.1 stable surface

PlexonUtility 3.1 keeps generic convenience actions together while delegating shared infrastructure to PlexonCore:

- `/utility` — 4-row Plexon utility hub with player status, AFK state, utility availability, family readiness, PlexonHomes navigation, refresh/close controls, and admin diagnostics
- `/feed [player]`
- `/heal [player]`
- `/enderchest [player]` (`/ec`)
- `/workbench`
- `/trash` — disposable writable inventory; remaining contents are destroyed on close
- `/afk` — manual and automatic AFK state with bossbar/actionbar feedback, PlaceholderAPI, and a public AFK transition event
- `/utilityadmin diagnostics|family|integrations|reload`

## Quiet feedback policy

3.1 moves routine personal feedback away from chat:

- persistent personal state → bossbar
- short successful self-actions → actionbar
- social state changes → compact prefixless chat by default
- errors, permission failures, cooldowns, and diagnostics → normal prefixed chat

AFK behavior by default:

- entering AFK shows a persistent `AFK • You are currently away` bossbar to the affected player;
- other online players receive a compact `<player> is now AFK` line without the PlexonUtility prefix;
- returning active removes the bossbar and shows `You are active again` in the actionbar;
- rapid AFK cycles suppress the public return line to reduce chat spam;
- the player whose state changed never receives their own public AFK announcement.

Successful self `/feed`, `/heal`, and `/trash` feedback also uses the actionbar by default. These policies are configurable under `feedback:` in `config.yml`.

The feedback layer creates no scheduler or animation task. AFK bossbars are lifecycle-managed on active transition, quit, reload/feature disable, and plugin shutdown.

## MiniMessage and PlexonCore

PlexonUtility does not create its own MiniMessage parser. Text rendering uses `PlexonCore.text()`:

- configured MiniMessage is validated at startup/reload;
- prefixed and prefixless static components are cached until reload;
- runtime values use safe `TextService.renderTemplate(...)` insertion rather than parsing user/runtime values as MiniMessage;
- missing keys introduced by upgrades are copied from bundled defaults into existing `messages.yml` files without overwriting customized values.

The module publishes capabilities through PlexonCore including `utility-api`, `afk-state`, `afk-event`, `placeholderapi`, `quiet-feedback`, `core-text`, `core-gui`, `core-scheduler`, `core-integrations`, `family-compatibility`, and `complement-diagnostics`.

## PlexonFamily interoperability

PlexonUtility uses `PlexonCore.integrations()` as the shared compatibility registry. Discovery refreshes on startup, explicit reload/diagnostics, and plugin enable/disable events; there is no recurring compatibility poll.

Known family products include PlexonBackpacks, PlexonBlacksmith, PlexonChats, PlexonCrates, PlexonGPFlags, PlexonHomes, PlexonJobs, PlexonKeys, PlexonPanel, PlexonQuests, PlexonRanks, PlexonShops, PlexonSkills, PlexonSpawners, PlexonTools, and PlexonTravel.

`/utilityadmin family` reports the live registry view.

### PlexonHomes integration and rank limits

PlexonHomes remains authoritative for home persistence, limits, `/home`, `/homes`, and teleport safety. PlexonUtility only exposes the existing PlexonHomes `/homes` GUI from the Utility hub when the integration is ready.

PlexonHomes supports rank-friendly numeric limits:

```text
plexonhomes.limit.<N>
plexonhomes.limit.unlimited
```

The highest active numeric permission wins; `plexonhomes.limit.unlimited` overrides numeric values. PlexonRanks or LuckPerms can therefore grant progressively larger limits without hard-coding rank logic into PlexonUtility.

## AFK interoperability

PlaceholderAPI outputs:

```text
%plexonutility_afk%
%plexonutility_is_afk%
```

`%plexonutility_afk%` returns the configured display value. `%plexonutility_is_afk%` returns `true` or `false`, making it suitable for TAB `placeholder-output-replacements`.

Other plugins can also consume `PlexonUtilityAPI#isAfk(UUID)` or listen for `AfkStateChangeEvent`. The event is informational, non-cancellable, and emitted on the primary thread after the AFK state changes.

## Specialist ownership boundary

PlexonUtility does not reimplement mature specialist systems. `/utilityadmin integrations` detects and publishes availability for areas such as claims/regions, CoreProtect, LuckPerms, configurable menus, anti-cheat, display/HUD plugins, spark/Chunky, and proximity voice.

Dedicated Plexon products retain their own data, transaction, and gameplay authority.

## Runtime

- Paper `26.2`
- Java `25`
- PlexonCore `2.0.5` compile/runtime boundary (`depend: PlexonCore`)
- PlaceholderAPI optional
- PlexonHomes optional integration
- no plugin-owned database
- in-memory monotonic cooldowns
- one shared AFK timeout scan only when automatic AFK is enabled
- no feedback scheduler or per-player repeating task

## Build and release

CI downloads the immutable `PlexonCore-2.0.5.jar`, verifies its pinned SHA-256, installs it into the CI-local Maven repository, then runs:

```bash
mvn -B -ntp clean verify
```

The stable artifact is `target/PlexonUtility-3.1.0.jar`. CI verifies Java class major `69`, Paper `26.2` metadata, required runtime classes/resources, command descriptors, all tests, SHA-256 output, and that Core/Paper/Bukkit/PlaceholderAPI/Adventure runtime classes are not shaded into the JAR.

The stable release workflow publishes only an exact `main` commit with a stable Maven version, matching release notes, and a previously unused tag.

## Permissions

```text
plexonutility.menu
plexonutility.feed
plexonutility.feed.others
plexonutility.feed.cooldown.bypass
plexonutility.heal
plexonutility.heal.others
plexonutility.heal.cooldown.bypass
plexonutility.enderchest
plexonutility.enderchest.others
plexonutility.workbench
plexonutility.trash
plexonutility.afk
plexonutility.afk.auto.bypass
plexonutility.admin
plexonutility.reload
```
