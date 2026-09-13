# PlexonUtility

Core-native player utilities, quiet HUD feedback, PlexonFamily interoperability, and a bounded native staff control plane for PlexonCraft.

## 3.5.0 surface

PlexonUtility 3.5.0 is deliberately broader than the original Utility plugin but is still **not an Essentials clone**. It owns general-purpose conveniences and bounded administration only when no specialist Plexon Family plugin already owns the domain.

### Player commands

- `/utility`
- `/feed [player]`
- `/heal [player]`
- `/enderchest [player]` (`/ec`)
- `/workbench`
- `/trash`
- `/afk`
- `/anvil` — native Paper anvil surface; vanilla anvil costs/rules remain authoritative

### Staff/admin commands

- `/utilityadmin` (`/uadmin`)
- `/invsee <player>` — live inventory inspection; edit authority remains separately permissioned
- `/vanish [on|off]`
- `/kick <player> [reason...]`
- `/ban <player> [duration|perm] [reason...]`
- `/unban <player>`
- `/prison status|set|goto|send <player>|clear`
- `/killall <category|entity> [radius|world [world]]`
- `/spawnmob <entity> [amount]`
- `/gamemode <survival|creative|adventure|spectator> [player]` (`/gm`)
- `/fly [on|off] [player]`
- `/god [on|off] [player]`
- `/speed <1-10|reset> [walk|fly] [player]`
- `/clearinventory [player]` (`/ci`, `/clearinv`)

## `/spawn` ownership

PlexonUtility does **not** register root `/spawn`.

- `/spawn` remains PlexonTravel travel-spawn authority.
- `/spawnmob` is PlexonUtility entity creation.

This is enforced by CI against the packaged `plugin.yml`.

## Entity cleanup safety

`/killall` supports category aliases such as `hostile`, `monsters`, `passive`, `animals`, `ambient`, `aquatic`, `water`, `bosses`, `mobs`, and `all`, plus explicit living `EntityType` names.

Default safety rules:

- players are never valid targets;
- named entities are protected;
- tamed entities are protected;
- villagers are protected;
- armor stands/displays are protected;
- plugin/NPC-marked entities are protected where detectable;
- bosses are protected unless `bosses` or an explicit boss type is selected;
- configured radius is bounded (default maximum 512);
- large cleanup requires actor-specific 15-second confirmation;
- removal is direct cleanup, not simulated player combat, and therefore does not intentionally create normal loot/XP/kill credit.

## Entity spawning safety

`/spawnmob` accepts only living Paper-spawnable entity types, never `PLAYER`.

- default amount: 1
- absolute maximum: 100
- config may lower but never raise the maximum
- placement uses a bounded safe search near the player/target block
- candidate chunk must already be loaded
- no arbitrary async Bukkit entity operation
- no chunk fan-out and no permanent task

## Player controls

`/gamemode`, `/fly`, `/god`, `/speed`, and `/clearinventory` use separate self/other permissions.

- Utility-managed `/fly` applies to Survival/Adventure and never forcibly strips legitimate Creative/Spectator flight.
- `/god` cancels applicable damage events rather than polling/restoring health. Void damage remains authoritative. State is intentionally restart-ephemeral in 3.5.0.
- `/speed` validates the friendly 1–10/reset input before calling Bukkit setters.
- `/clearinventory` explicitly clears storage, armor and offhand and audits one aggregate stack count.

## Vanish synthetic presence

Native vanish remains event-driven with plugin-aware visibility/player-list operations and no visibility polling.

3.5.0 additionally provides presentation-only synthetic presence:

- visible → vanished: ordinary viewers receive one synthetic leave;
- vanished → visible: ordinary viewers receive one synthetic join;
- repeated `/vanish on` or `/vanish off`: no duplicate synthetic message;
- actor excluded;
- `plexonutility.admin.vanish.see` viewers excluded from the ordinary audience by default;
- real persisted vanished join/quit remains suppressible without duplicate synthetic lifecycle messages;
- no fake `PlayerJoinEvent` or `PlayerQuitEvent` is dispatched.

`VanishStateChangeEvent` is available to integrations and fires only on a real state transition.

PlexonChats remains the preferred connection-message formatting authority. Its current public API does not expose synthetic join/quit rendering, so 3.5.0 uses a configurable fallback rendered through `PlexonCore.text()` without depending on PlexonChats internals.

## API/interoperability

PlaceholderAPI outputs remain:

```text
%plexonutility_afk%
%plexonutility_is_afk%
%plexonutility_vanished%
```

Public API/event surfaces include:

- `PlexonUtilityAPI#isAfk(UUID)`
- `PlexonUtilityAPI#isVanished(UUID)`
- `PlexonUtilityAPI#isGodMode(UUID)`
- `AfkStateChangeEvent`
- `VanishStateChangeEvent`

Core module registration conditionally advertises the active entity-management, player-admin, synthetic-presence and vanish-event capabilities.

## Specialist ownership boundary

PlexonUtility does not absorb:

- homes and home limits → PlexonHomes
- spawn/hub/back/warps/RTP/TPA and destination travel → PlexonTravel
- repair/gameplay item servicing → PlexonBlacksmith
- chat routing/private messaging/social spy/connection formatting → PlexonChats
- rank progression → PlexonRanks/LuckPerms
- economy/shops, jobs, skills, quests, keys, crates, claims → their specialist providers
- kits, mail, nicknames, IP bans, freeze, mute, punishment history, appeals, or cross-server moderation

## Persistence

Human-authored policy stays in `config.yml`. Existing `admin-data.yml` continues to store the prison waypoint and optional persistent vanish UUIDs; 3.5.0 does not change its schema.

God mode and Utility-managed flight are intentionally runtime-ephemeral. `admin.player-management.god.persist` is reserved and must remain `false` in 3.5.0.

## Performance boundary

- Java 25
- Paper 26.2
- PlexonCore 2.0.5
- no new permanent 3.5 scheduler/poller
- one existing shared AFK timeout scan only when automatic AFK is enabled
- no per-entity task fan-out
- no entity command chunk fan-out
- no plugin-local executor pool
- no NMS/CraftBukkit reflection/packet fake-disconnect implementation

## Permissions

The OP-default `plexonutility.admin` parent includes the staff/admin capability set. Important 3.5 additions:

```text
plexonutility.admin.killall
plexonutility.admin.killall.world
plexonutility.admin.killall.bypass-confirm
plexonutility.admin.spawnmob
plexonutility.admin.gamemode
plexonutility.admin.gamemode.others
plexonutility.admin.fly
plexonutility.admin.fly.others
plexonutility.admin.god
plexonutility.admin.god.others
plexonutility.admin.speed
plexonutility.admin.speed.others
plexonutility.admin.clearinventory
plexonutility.admin.clearinventory.others
plexonutility.anvil
```

## Build and release

Canonical source verification provisions the pinned PlexonCore 2.0.5 artifact and runs:

```bash
mvn -B -ntp clean verify
```

CI verifies Java class major 69, Paper 26.2 metadata, required resources/classes/commands, provided-API isolation, all discovered tests with zero failures/errors/skips, SHA-256 generation, and the absence of a PlexonUtility root `/spawn` command.

Stable 3.5.0 publication has an additional runtime gate. The release workflow requires committed real Paper staging evidence in:

```text
releases/3.5.0-runtime-smoke.txt
```

with at least:

```text
result=PASS
paper=26.2
java=25
```

Without that evidence, source may be reviewable/mergeable but `v3.5.0` publication remains intentionally blocked.

When certified, the stable release publishes:

- `PlexonUtility-3.5.0.jar`
- `SHA256SUMS.txt`
- `TEST_SUMMARY.txt`
- `PROVENANCE.txt`
