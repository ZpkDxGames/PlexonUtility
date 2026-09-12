# PlexonUtility 3.1 Architecture

## Product role

PlexonUtility owns small, generic player convenience actions that do not justify dedicated Plexon products. It may surface entry points and health for other PlexonFamily modules, but it does not duplicate their data, transactions, or gameplay authority.

Examples:

- PlexonHomes owns home persistence, limits, `/home`, `/homes`, and safe teleportation.
- PlexonRanks owns rank progression and may grant `plexonhomes.limit.<N>` permissions.
- PlexonChats owns chat formatting/routing.
- PlexonTools, PlexonQuests, PlexonJobs, PlexonSkills and other gameplay modules retain their own reward/progression authority.

## Shared PlexonCore runtime

The 3.x module registers as `utility` against PlexonCore API `>=2.0 <3.0` and compiles against stable PlexonCore `2.0.5`.

Capabilities include:

- `utility-api`
- `afk-state`
- `afk-event`
- `placeholderapi`
- `core-text`
- `core-gui`
- `core-scheduler`
- `core-integrations`
- `family-compatibility`
- `complement-diagnostics`

Shared behavior is delegated to Core:

| Concern | Owner |
| --- | --- |
| MiniMessage/text templates | `PlexonCore.text()` |
| Utility navigation GUI routing | `PlexonCore.gui()` |
| Async → primary handoff | `PlexonCore.scheduler()` |
| Module lifecycle | `PlexonCore.modules()` |
| PlexonFamily/external ecosystem states | `PlexonCore.integrations()` |

Core module state updates and cleanup use the owner-aware API so an old plugin instance cannot mutate or remove a newer registration.

## Text and MiniMessage policy

PlexonUtility does not own a MiniMessage parser.

- trusted static templates are rendered through `PlexonCore.text()`;
- runtime values use `TextService.renderTemplate(...)` and are inserted as plain components;
- configured MiniMessage is strict-validated at startup/reload;
- prefixed and prefixless static configured messages are component-cached until reload;
- a reload clears the caches only after the candidate catalog is validated;
- old message catalogs are migrated by copying only missing required keys from bundled defaults and persisting the result.

This prevents unsafe runtime text parsing, repeated hot-path parser construction, and post-upgrade `Missing message: ...` output.

## Quiet feedback policy

`FeedbackService` centralizes presentation rather than letting individual commands invent their own HUD/chat behavior.

Default policy:

| Event type | Default surface |
| --- | --- |
| Persistent personal state | Bossbar |
| Short successful self-action | Actionbar |
| Social state change affecting other players | Compact prefixless chat |
| Error / permission / invalid state | Normal prefixed chat |
| Admin diagnostics / reload | Normal prefixed chat |
| Destructive interaction | GUI plus short actionbar warning |

Current 3.1 applications:

- AFK entry: persistent `AFK • You are currently away` bossbar for the affected player.
- AFK exit: remove bossbar and show `You are active again` actionbar.
- AFK social event: only other online players receive the compact chat line.
- Fast AFK toggles: the public return line is suppressed when the AFK cycle is shorter than the configured threshold.
- `/feed`, `/heal`, `/trash`: successful self-feedback uses actionbar by default.

`FeedbackService` creates no scheduler. Bossbars are keyed by player UUID and are removed/replaced deterministically on active transition, quit, reload/feature-disable reconciliation, and plugin shutdown.

Social AFK cycles track a monotonic timestamp only when the AFK entry announcement was actually emitted. A return announcement is therefore never emitted for a cycle whose entry announcement was disabled.

## PlexonFamily compatibility

`FamilyCompatibilityService` registers known family products in Core's integration registry and refreshes only at:

- startup;
- explicit Utility reload;
- explicit `/utilityadmin family`/GUI refresh;
- relevant plugin enable/disable events.

There is no repeating compatibility poll.

PlexonHomes receives special navigation integration in `/utility`: when the Core integration state is ready and the viewer has `plexonhomes.gui`, the hub delegates to `/homes` rather than creating a second homes implementation.

## Home-limit permission contract

PlexonHomes remains authoritative for home limits. Its numeric permission contract is intentionally suitable for PlexonRanks or LuckPerms automation:

```text
plexonhomes.limit.<N>
plexonhomes.limit.unlimited
```

The highest active numeric node wins. Unlimited overrides numeric values. If neither exists, the PlexonHomes config default applies.

PlexonUtility only documents/surfaces this contract; it does not cache, reinterpret, or override home limits.

## AFK interoperability

AFK state remains thread-safe, in-memory, and ephemeral.

Consumers have three supported surfaces:

1. `PlexonUtilityAPI#isAfk(UUID)` for point-in-time reads.
2. `%plexonutility_afk%` / `%plexonutility_is_afk%` for PlaceholderAPI/TAB.
3. `AfkStateChangeEvent` for push-based interoperability.

`AfkStateChangeEvent` is informational, non-cancellable, and always fired on the primary thread after the tracker state changes. Async chat activity is handed back through `PlexonCore.scheduler()` before the event/feedback is emitted.

## Local runtime state

PlexonUtility locally owns only state that is specific and cheap:

- monotonic cooldown timestamps;
- AFK activity timestamps/state;
- one AFK timeout scan task when automatic AFK is enabled;
- active AFK bossbar handles keyed by UUID;
- AFK public-announcement timestamps used only for short-cycle suppression;
- the current immutable validated utility configuration;
- the current validated message catalog;
- static rendered message components between reloads.

No plugin-owned database is required. AFK state is intentionally ephemeral and cooldowns are cleared on quit.

## GUI surfaces

`/utility` is a protected 4-row navigation GUI using Core holder identity, click routing, and session tracking. It shows live player status, utility availability, permission states, family readiness, PlexonHomes navigation, refresh/close controls, and admin diagnostics.

`/trash` intentionally does **not** use Core GUI routing because it must permit normal item movement. It creates a short-lived writable Bukkit inventory with a private holder; no persistent reference is retained after the view closes, so remaining contents are discarded. The destructive warning is shown in the actionbar by default rather than consuming chat space.

## External complements

`ComplementService` scans only for enabled specialist providers. It never proxies their commands or claims that PlexonUtility implements their functionality. Each category is published to `PlexonCore.integrations()` under a `UTILITY_*` namespace, with `READY` when at least one configured provider is enabled and `MISSING` otherwise.

## Performance constraints

- no per-player repeating scheduler;
- no feedback scheduler or animation task;
- no file/database/network I/O from gameplay event listeners;
- no plugin-local executor pool;
- no plugin-local navigation GUI listener/router;
- no plugin-local MiniMessage instance;
- no recurring family/integration polling;
- prefixed and prefixless static message components cached between reloads;
- AFK movement ignores rotation/sub-block movement;
- async chat state mutation remains thread-safe and event/UI feedback handoff uses Core;
- external complement scans occur only at startup, explicit reload, or explicit integration diagnostics.
