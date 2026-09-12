# PlexonUtility 3.3.0 Admin Toolkit Migration

Date: 2026-09-12

## Superseding product decision

Earlier PlexonUtility releases deliberately left vanish and moderation outside the plugin because no production requirement had been established. Version 3.3.0 supersedes that decision only for this bounded native set: self-vanish, kick, native profile ban/unban, read-only inventory inspection, one holding waypoint, and selected lightweight player-management actions.

This does not transfer ownership of broad moderation, travel, homes, ranks, economy, chat, claims, or specialist gameplay systems into PlexonUtility.

## Vanish cutover

Repository history referenced SuperVanish/TAB vanish placeholders. PlexonUtility 3.3.0 can now be the native vanish authority.

Do not intentionally run two authoritative vanish engines. Before enabling native PlexonUtility vanish in production:

1. decide whether PlexonUtility or the external provider owns vanish state;
2. if PlexonUtility becomes authoritative, disable/remove the external authoritative vanish behavior;
3. migrate TAB conditions that depend on `%supervanish_isvanished%` to `%plexonutility_vanished%` or a direct native-compatible condition;
4. validate staff roles that should retain `plexonutility.admin.vanish.see`;
5. validate PlexonChats join/quit hidden-message handling when `admin.vanish.suppress-join-quit` is enabled.

Vanish persistence is stored by UUID in `admin-data.yml` when `admin.vanish.persist: true`.

## Ban authority

PlexonUtility bans use the server's native profile ban list. There is no separate PlexonUtility punishment database.

- known/cached profile resolution only unless the player is currently online;
- no synchronous web profile lookup;
- no IP bans;
- no punishment-history migration is required.

If another moderation plugin is currently authoritative for profile bans, operators should decide which command surface staff will use and avoid conflicting automation.

## Prison waypoint

The prison feature is one persisted teleport waypoint. It is not a jail/sentence system. Existing jail data from another plugin is not imported and should remain with that specialist provider.

## Ownership unchanged

- PlexonHomes: home persistence, limits and teleports
- PlexonTravel: spawn/hub/back/warps/TPA and player travel
- PlexonBlacksmith: repair/enchant gameplay loops
- PlexonRanks/LuckPerms: ranks and permission progression
- PlexonJobs: jobs
- PlexonChats: chat moderation/muting/social communication
- economy/shop providers: economy ownership
- claims/flags providers: protection ownership

## Rollback

The previous stable boundary is PlexonUtility 3.2.0. Rolling back the JAR does not require converting `admin-data.yml`; 3.2.0 simply does not consume it. Keep the file if a later 3.3+ reinstallation should recover the saved prison waypoint/vanish UUID set.
