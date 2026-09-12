# Essentials Feature Audit — PlexonUtility 1.0.0

> **Superseding note — 2026-09-12 / PlexonUtility 3.3.0:** the historical 1.0 decision below that all moderation remain external is superseded only for the focused native 3.3 admin toolkit: self-vanish, kick, native profile ban/unban, read-only inventory inspection, one prison/holding waypoint, and selected lightweight player-management actions. Broad moderation history, mute, social spy, freeze, IP bans, appeals, cross-server moderation, rank/economy editing, nicknames, kits/mail, and specialist ownership remain external. See `docs/ADMIN_TOOLKIT_3.3_MIGRATION.md` and `releases/3.3.0.md`.

This document otherwise preserves the original 1.0 repository audit as historical evidence. It does **not** claim access to the live PlexonCraft plugin directory, command map, Vault provider, player data, or production configuration.

| Feature | 1.0 owner/status | Production evidence | Historical action |
| --- | --- | --- | --- |
| `/feed` | PlexonUtility — IMPLEMENTED | Specification identifies as required default utility candidate | Stage command ownership before Essentials removal |
| `/heal` | PlexonUtility — IMPLEMENTED | Specification identifies as required default utility candidate | Stage command ownership before Essentials removal |
| `/enderchest`, `/ec` | PlexonUtility — IMPLEMENTED | Specification identifies as required default utility candidate | Online targets only; stage ownership |
| `/workbench` | PlexonUtility — IMPLEMENTED | Specification identifies as required default utility candidate | Stage ownership |
| `/anvil` | DEFERRED | No production-use evidence | Keep disabled/external |
| `/hat` | DEFERRED | No production-use evidence | Keep disabled/external |
| `/trash` | DEFERRED | No production-use evidence | Keep disabled/external |
| `/afk` | DEFERRED | No production consumer/dependency evidence | Do not run a second authoritative AFK engine |
| `/repair` | PlexonBlacksmith / external | Explicitly excluded by architecture | Do not implement in Utility |
| `/fly`, `/god`, `/speed` | DEFERRED / external admin tooling | No production-use evidence | Do not implement for parity alone |
| `/nick` | Identity/chat owner unresolved | Production identity integrations not audited | Blocks Essentials removal if actively used |
| `/msg`, `/r`, `/socialspy` | PlexonChats/future messaging owner | Communication responsibility | Not Utility |
| Kits | Dedicated future owner/external | Production use not audited | Blocks Essentials removal if active |
| Mail | Dedicated future owner/external | Production use not audited | Blocks Essentials removal if active |
| `/spawn`, `/hub`, `/back`, `/warp`, `/tpa` | PlexonTravel | Separate module responsibility | Validate PlexonTravel PRIMARY before removal |
| `/home`, `/sethome` | PlexonHomes | Separate module responsibility | Validate PlexonHomes PRIMARY before removal |
| Economy/Vault provider | NOT UTILITY | Production provider unknown | **Critical blocker until audited** |
| Moderation | Dedicated moderation provider | Production usage unknown | **Historical 1.0 decision; superseded only by the bounded 3.3 native set described above** |

## Required production follow-up

Before uninstalling or disabling Essentials on the live server:

1. Inspect Bukkit/Paper command ownership and aliases.
2. Inspect installed plugin metadata for `depend`/`softdepend` on Essentials and any Essentials API usage.
3. Identify the active Vault economy provider and balance storage.
4. Audit active Essentials kits, mail, nicknames, moderation usage, and any custom MyCommand aliases.
5. Validate PlexonHomes and PlexonTravel in PRIMARY mode.
6. For native 3.3 vanish, decide one authoritative vanish engine and migrate TAB/PlexonChats presentation rules before removing the previous provider.
7. Run a staging boot with Essentials physically disabled/removed.
8. Compare balances and critical plugin behavior, then run a soak/performance profile.

Until those steps have evidence, full Essentials decommission remains **BLOCKED**.
