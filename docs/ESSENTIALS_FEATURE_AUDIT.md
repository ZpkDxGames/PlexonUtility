# Essentials Feature Audit — PlexonUtility 3.5.0

> **3.5.0 update — 2026-09-12:** PlexonUtility now intentionally absorbs the useful low-overlap administration/utility surface listed below. This supersedes older `DEFERRED` decisions for `/anvil`, `/fly`, `/god`, `/speed`, `/clearinventory`, `/gamemode`, bounded entity cleanup, and bounded entity spawning. It does **not** change specialist Plexon Family ownership.

This repository audit does not by itself prove that Essentials is removable from the live PlexonCraft host. Live command ownership, dependent plugins, economy provider/data, kits/mail/nicknames, and staging behavior still require evidence.

| Responsibility | 3.5.0 owner/status | Boundary / evidence |
| --- | --- | --- |
| `/feed` | PlexonUtility — REPLACED | Existing native Utility command |
| `/heal` | PlexonUtility — REPLACED | Existing native Utility command |
| `/enderchest`, `/ec` | PlexonUtility — REPLACED | Existing native Utility command; online targets |
| `/workbench` | PlexonUtility — REPLACED | Existing native Utility command |
| `/anvil` | PlexonUtility — REPLACED in 3.5 | Native Paper anvil UI; vanilla rules/costs remain authoritative |
| `/trash` | PlexonUtility — REPLACED | Writable disposable Utility inventory |
| `/afk` | PlexonUtility — REPLACED | One shared event-driven/bounded AFK engine |
| `/gamemode` | PlexonUtility — REPLACED in 3.5 | Self/other permission split and audit |
| `/fly` | PlexonUtility — REPLACED in 3.5 | Utility-owned Survival/Adventure flight; does not disable Creative/Spectator authority |
| `/god` | PlexonUtility — REPLACED in 3.5 | Event cancellation, no health polling; restart-ephemeral in 3.5 |
| `/speed` | PlexonUtility — REPLACED in 3.5 | Validated 1–10/reset mapping to Bukkit speed bounds |
| `/clearinventory`, `/ci`, `/clearinv` | PlexonUtility — REPLACED in 3.5 | Full storage + armor + offhand clear with aggregate audit |
| Entity cleanup (`/killall`) | PlexonUtility — REPLACED in 3.5 | Bounded selectors/radius, conservative protections, confirmation threshold, players never valid targets |
| Entity spawning | PlexonUtility — REPLACED in 3.5 as `/spawnmob` | Hard cap 100; safe loaded-location search; **does not take `/spawn`** |
| Self vanish | PlexonUtility — REPLACED | Native visibility/list state plus 3.5 synthetic presentation on real transitions only |
| Kick/profile ban/unban | PlexonUtility — REPLACED | Focused native moderation, no private punishment-history system |
| Online inventory inspection/edit | PlexonUtility — REPLACED | 3.4 live editor with separate edit permission |
| Prison holding waypoint | PlexonUtility — REPLACED | Single holding location, not a jail/sentence subsystem |
| `/repair` | PlexonBlacksmith — KEEP EXTERNAL | Specialist item-repair owner |
| `/msg`, `/r`, social spy/chat formatting | PlexonChats — KEEP EXTERNAL | Communication owner |
| `/spawn`, `/hub`, `/back`, `/warp`, `/warps`, `/rtp`, TPA family | PlexonTravel — KEEP EXTERNAL | Travel/destination owner. `/spawn` remains PlexonTravel; entity creation is `/spawnmob` |
| `/home`, `/sethome`, `/delhome`, home limits | PlexonHomes — KEEP EXTERNAL | Home/data/teleport owner |
| Rank editing | PlexonRanks / rank system — KEEP EXTERNAL | Not Utility |
| Economy editing/provider | Economy owner — KEEP EXTERNAL / LIVE AUDIT REQUIRED | Preserve balances/provider; Utility does not edit economy |
| Kits | Dedicated/external owner — LIVE AUDIT REQUIRED | Utility does not absorb kit storage/claims |
| Mail | Dedicated/external owner — LIVE AUDIT REQUIRED | Utility does not absorb mail |
| Nicknames | Identity/chat owner — LIVE AUDIT REQUIRED | Utility does not own nicknames |
| Claims, crates, jobs, skills, quests, keys | Specialist Plexon plugins — KEEP EXTERNAL | Explicit family ownership |
| Freeze, mute, IP bans, punishment history, appeals, cross-server moderation | Dedicated moderation system — KEEP EXTERNAL | Intentionally outside 3.5 scope |

## Synthetic vanish / PlexonChats boundary

PlexonUtility remains the vanish-state authority. PlexonChats remains the desired connection-message formatting authority. At the 3.5.0 implementation point, the current public `PlexonChatsAPI` has no synthetic join/quit renderer or audience-aware broadcaster, so Utility uses a configurable PlexonCore/MiniMessage fallback. It does not dispatch fake Bukkit lifecycle events and does not compile against PlexonChats internals.

## Required production follow-up before Essentials removal

1. Inspect the live Paper command map and verify each Essentials-owned command still used by players/staff has an intentional replacement or retirement decision.
2. Inspect installed plugins for hard/soft Essentials API dependencies.
3. Identify and preserve the active economy/Vault provider and balances.
4. Audit any still-active Essentials kits, mail, nicknames, homes/warps data, moderation data, and custom command aliases.
5. Confirm PlexonTravel PRIMARY for travel commands and PlexonHomes PRIMARY for home commands.
6. Confirm PlexonUtility is the single active vanish authority and that TAB/chat presentation does not expose vanished staff.
7. Run the full PlexonUtility 3.5 Paper 26.2 runtime smoke test, including protected cleanup entities, `/spawnmob zombie 100`, and three-audience vanish behavior.
8. Boot staging with Essentials physically disabled/removed and run a soak/performance check.

`Essentials fully decommissioned? NO — BLOCKED pending live dependency/data/runtime evidence.`
