# Essentials Decommission Checklist

Status vocabulary: `REPLACED`, `NOT USED`, `KEEP EXTERNAL`, `DEFERRED — BLOCKS REMOVAL`, `UNKNOWN — BLOCKS REMOVAL`.

| Responsibility | Status after PlexonUtility 3.5.0 source | Evidence / next gate |
| --- | --- | --- |
| Feed | REPLACED | PlexonUtility native implementation + CI |
| Heal | REPLACED | PlexonUtility native implementation + CI |
| Ender Chest | REPLACED | PlexonUtility native implementation + CI; online targets |
| Workbench | REPLACED | PlexonUtility native implementation + CI |
| Anvil | REPLACED | PlexonUtility 3.5 native Paper anvil surface |
| AFK | REPLACED | PlexonUtility event-driven/shared scheduler implementation |
| Gamemode | REPLACED | PlexonUtility 3.5 self/other permission split |
| Fly | REPLACED | PlexonUtility 3.5 Survival/Adventure authority; Creative/Spectator preserved |
| God | REPLACED | PlexonUtility 3.5 event-driven, restart-ephemeral state |
| Speed | REPLACED | PlexonUtility 3.5 validated 1–10/reset control |
| Clear inventory | REPLACED | PlexonUtility 3.5 full-inventory clear + audit |
| Entity cleanup | REPLACED | PlexonUtility 3.5 `/killall`; protected defaults and player exclusion |
| Entity spawn | REPLACED | PlexonUtility 3.5 `/spawnmob`; absolute maximum 100 |
| Self vanish | REPLACED | PlexonUtility native vanish + 3.5 transition-only synthetic presence |
| Kick / profile ban / unban | REPLACED | PlexonUtility bounded native moderation |
| Inventory inspect/edit | REPLACED | PlexonUtility 3.4+ live editor with separate edit permission |
| Prison holding waypoint | REPLACED | PlexonUtility single holding waypoint |
| Homes | UNKNOWN — BLOCKS REMOVAL | Confirm PlexonHomes PRIMARY on production |
| Spawn / Hub / Back / Warps / RTP / TPA | UNKNOWN — BLOCKS REMOVAL | Confirm PlexonTravel PRIMARY on production. `/spawn` remains PlexonTravel; Utility uses `/spawnmob` for entities |
| Nicknames | UNKNOWN — BLOCKS REMOVAL | Audit PlexonChats/Ranks/LuckPerms/Discord/tab integrations |
| Kits | UNKNOWN — BLOCKS REMOVAL | Audit active Essentials kit data/use |
| Mail | UNKNOWN — BLOCKS REMOVAL | Audit player mail data/use |
| Messaging/social spy | KEEP EXTERNAL | PlexonChats responsibility, not Utility |
| Repair/enchant gameplay | KEEP EXTERNAL | PlexonBlacksmith responsibility |
| Broad moderation history/mute/freeze/IP bans/appeals | KEEP EXTERNAL | Requires dedicated moderation owner if used |
| Vault economy provider | UNKNOWN — BLOCKS REMOVAL | **Critical:** identify provider and preserve balances |
| Other plugin hard-dependencies | UNKNOWN — BLOCKS REMOVAL | Inspect installed plugin metadata/source |
| PlexonUtility 3.5 real Paper smoke | DEFERRED — BLOCKS STABLE 3.5 PUBLICATION | Record `releases/3.5.0-runtime-smoke.txt` with PASS evidence before stable release workflow can publish |

## Production decision

`Essentials fully decommissioned? NO — BLOCKED`

`PlexonUtility 3.5.0 stable published? ONLY AFTER REAL PAPER 26.2 RUNTIME PASS`

The release workflow enforces the second statement independently of source CI. Full Essentials removal additionally requires the live dependency/data audit above and a staging boot with Essentials physically disabled.
