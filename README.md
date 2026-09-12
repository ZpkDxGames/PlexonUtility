# PlexonUtility

Core-native player utilities, quiet HUD feedback, PlexonFamily interoperability, and a focused native staff control plane for PlexonCraft.

## 3.3.0 stable surface

PlexonUtility 3.3.0 preserves the 3.2 player Utility hub and adds a deliberately small administrative toolkit. It is not an Essentials clone and does not absorb specialist PlexonFamily responsibilities.

Player commands:

- `/utility` — 45-slot player utility hub with live state, family readiness, PlexonHomes navigation, help, refresh/close, and Admin Center entry for authorized staff
- `/feed [player]`
- `/heal [player]`
- `/enderchest [player]` (`/ec`)
- `/workbench`
- `/trash` — disposable writable inventory; remaining contents are destroyed on close
- `/afk` — manual/automatic AFK state with bossbar/actionbar feedback, PlaceholderAPI, and the public AFK event/API

Native admin commands:

- `/utilityadmin` (`/uadmin`) — opens the Admin Center for authorized players; console receives text diagnostics
- `/utilityadmin diagnostics|family|integrations|reload`
- `/invsee <player>` — online, exact-name, read-only cloned inventory snapshot including storage, armor, and offhand
- `/vanish [on|off]` — self-vanish using Paper plugin-aware visibility and player-list APIs
- `/kick <player> [reason...]`
- `/ban <player> [duration|perm] [reason...]` — native profile-ban authority; duration examples: `30m`, `2h`, `1d`, `7d`, `30d`
- `/unban <player>` — removes the same native profile ban
- `/prison status|set|goto|send <player>|clear` — holding-location waypoint only, not a jail/sentence engine

## Admin Center

The Admin Center uses `PlexonCore.gui()` for protected inventory navigation and Paper Dialogs for typed moderation input.

Surfaces:

- 45-slot Admin Center
- 54-slot deterministic alphabetical online-player selector
- 45-slot player actions screen
- compact prison management screen
- paginated native profile-ban management screen
- read-only inventory inspector

Selected-player actions include player information, inventory inspection, Ender Chest, heal, feed, admin teleport-to, bring, send-to-prison, kick, and ban. Kick/ban have explicit final confirmation. Destructive self kick/ban is blocked from the GUI.

Editable inventory inspection and clear-inventory are deliberately not shipped in 3.3.0. Read-only inspection avoids duplication/deletion races; `plexonutility.admin.clearinventory` is reserved for a future deliberately reviewed action.

## Native vanish

Vanish is event-driven and creates no polling task.

- `viewer.hidePlayer(plugin, target)` / `showPlayer(...)` preserve plugin ownership semantics
- player-list visibility is reconciled with Paper list/unlist APIs
- staff with `plexonutility.admin.vanish.see` retain visibility
- joining viewers receive the correct visibility state
- optional persistence is stored in `admin-data.yml`
- normal join/quit announcements can be suppressed for vanished staff
- plugin-owned visibility is restored on shutdown

Do not intentionally operate PlexonUtility native vanish alongside another authoritative vanish engine. If production still uses SuperVanish or another provider, choose one authority during migration. TAB configurations that depended on `%supervanish_isvanished%` should be migrated to `%plexonutility_vanished%` or an equivalent native condition. PlexonChats join/quit handling should keep its hidden-message/respect-hidden behavior enabled during cutover.

## Native moderation

PlexonUtility uses Paper/Bukkit profile bans rather than a private punishment database.

- online exact-name lookup first
- cached/known offline profile lookup only; no synchronous web profile lookup on the primary thread
- permanent and bounded-duration profile bans
- actor/source and sanitized reason recorded by the native ban authority
- online ban can kick the target as part of the supported API operation
- kick uses Adventure `Component` feedback
- destructive actions are logged with actor/target identity and relevant reason/duration
- no IP bans, mute, freeze, punishment history, appeals, web moderation panel, or cross-server network bans

## Prison waypoint

`/prison` owns one configurable holding location in `admin-data.yml`: world, coordinates, yaw, and pitch. It can be set, inspected, visited, used to send an online target, and cleared.

It does **not** implement movement locking, sentence timers, jailed-player persistence, automatic release/return, inventory confiscation, or punishment history.

## Persistence

Human-authored policy stays in `config.yml`. Runtime admin state stays in `admin-data.yml` with schema validation.

- prison waypoint and optional persistent vanish UUIDs only
- writes occur only on state changes
- writes use PlexonCore's IO scheduler lane
- temporary-file + atomic-move where supported, recoverable replacement fallback otherwise
- no disk I/O in movement/chat/inventory-click hot paths
- corrupt/invalid admin data fails safely instead of silently resetting valid state

## Menu/text safety

All protected navigation stays on `PlexonCore.gui()`. All MiniMessage/template rendering stays on `PlexonCore.text()`.

- runtime/player/reason/world values use safe template insertion rather than being reparsed as MiniMessage markup
- menu names/lore force vanilla italics off
- irrelevant attribute tooltip clutter is hidden where appropriate
- green = ready/success, aqua/cyan = information/navigation, yellow/gold = action/caution, red = destructive, gray = metadata/unavailable
- no animation scheduler or plugin-local generic GUI router

## Quiet feedback and AFK

3.3 preserves the established quiet-feedback architecture:

- persistent personal AFK state → bossbar
- short successful self-actions → actionbar
- compact social AFK state changes → prefixless chat by default
- errors, permission failures, cooldowns, and detailed diagnostics → normal prefixed chat

AFK remains the only shared repeating Utility task when automatic timeout is enabled. No admin capability adds a recurring scheduler.

## Placeholder/API interoperability

PlaceholderAPI outputs:

```text
%plexonutility_afk%
%plexonutility_is_afk%
%plexonutility_vanished%
```

Public API:

- `PlexonUtilityAPI#isAfk(UUID)`
- `PlexonUtilityAPI#isVanished(UUID)` — added as a binary-compatible default method
- `AfkStateChangeEvent`

## Specialist ownership boundary

PlexonUtility does not absorb:

- homes persistence/limits/teleports → PlexonHomes
- spawn/hub/back/warps/TPA/player travel → PlexonTravel
- repair/enchant gameplay → PlexonBlacksmith
- ranks/permission progression → PlexonRanks / LuckPerms
- economy/shops → the configured economy/shop providers
- jobs → PlexonJobs
- chat moderation/muting/social communication → PlexonChats
- protection/claims → existing claims/flags providers

The 3.3 native admin scope intentionally supersedes older documentation that said all vanish/moderation must remain external, but only for the narrow capabilities documented above.

## PlexonHomes integration

PlexonHomes remains authoritative for home persistence, limits, `/home`, `/homes`, and teleport safety. PlexonUtility only navigates to the existing Homes GUI when that family integration is ready.

Rank-friendly limits remain:

```text
plexonhomes.limit.<N>
plexonhomes.limit.unlimited
```

## Runtime

- Paper `26.2`
- Java `25`
- PlexonCore `2.0.5` compile/runtime boundary (`depend: PlexonCore`)
- PlaceholderAPI optional
- PlexonHomes optional integration
- in-memory monotonic cooldowns
- one shared AFK timeout scan only when automatic AFK is enabled
- event-driven vanish; no vanish/prison/inventory/integration polling
- no synchronous network profile lookup

## Permissions

Player-facing permissions remain unchanged. Admin permissions are granular and inherit from the OP-default `plexonutility.admin` parent:

```text
plexonutility.admin
plexonutility.admin.menu
plexonutility.admin.invsee
plexonutility.admin.vanish
plexonutility.admin.vanish.see
plexonutility.admin.kick
plexonutility.admin.ban
plexonutility.admin.unban
plexonutility.admin.prison
plexonutility.admin.prison.set
plexonutility.admin.prison.goto
plexonutility.admin.prison.send
plexonutility.admin.prison.clear
plexonutility.admin.teleport
plexonutility.admin.clearinventory
plexonutility.reload
```

## Build and stable release

CI provisions the immutable `PlexonCore-2.0.5.jar`, verifies its pinned SHA-256, installs it into the CI-local Maven repository, then runs:

```bash
mvn -B -ntp clean verify
```

The stable artifact is `PlexonUtility-3.3.0.jar`. Build/release verification checks Java class major `69`, Paper `26.2` metadata, required admin/runtime classes and command descriptors, all discovered tests with zero failures/errors/skips, provided-API isolation, SHA-256 generation, and `git diff --check`.

The stable release workflow publishes only the exact merged `main` source to tag `v3.3.0` and attaches:

- `PlexonUtility-3.3.0.jar`
- `SHA256SUMS.txt`
- `TEST_SUMMARY.txt`
- `PROVENANCE.txt`
