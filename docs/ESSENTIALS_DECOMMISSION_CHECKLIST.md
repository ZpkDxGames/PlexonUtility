# Essentials Decommission Checklist

Status vocabulary: `REPLACED`, `NOT USED`, `KEEP EXTERNAL`, `DEFERRED — BLOCKS REMOVAL`, `UNKNOWN — BLOCKS REMOVAL`.

| Responsibility | Status | Evidence / next gate |
| --- | --- | --- |
| Feed | REPLACED | PlexonUtility 1.0.0 implementation + CI |
| Heal | REPLACED | PlexonUtility 1.0.0 implementation + CI |
| Ender Chest | REPLACED | PlexonUtility 1.0.0 implementation + CI; online targets only |
| Workbench | REPLACED | PlexonUtility 1.0.0 implementation + CI |
| Homes | UNKNOWN — BLOCKS REMOVAL | Confirm PlexonHomes PRIMARY on production |
| Spawn/Hub/Back/Warps/TPA | UNKNOWN — BLOCKS REMOVAL | Confirm PlexonTravel PRIMARY on production |
| AFK | DEFERRED — BLOCKS REMOVAL if used | No production consumer audit available |
| Nicknames | UNKNOWN — BLOCKS REMOVAL | Audit PlexonChats/Ranks/LuckPerms/Discord/tab integrations |
| Kits | UNKNOWN — BLOCKS REMOVAL | Audit active Essentials kit data/use |
| Mail | UNKNOWN — BLOCKS REMOVAL | Audit player mail data/use |
| Messaging/social spy | KEEP EXTERNAL | Communication owner, not Utility |
| Moderation | KEEP EXTERNAL | Dedicated provider required if used |
| Repair/enchant gameplay | KEEP EXTERNAL | PlexonBlacksmith responsibility |
| Vault economy provider | UNKNOWN — BLOCKS REMOVAL | **Critical:** identify provider and preserve balances |
| Other plugin hard-dependencies | UNKNOWN — BLOCKS REMOVAL | Inspect installed plugin metadata/source |

## Production decision

`Essentials fully decommissioned? NO — BLOCKED`

This checklist must be updated from live/staging evidence before the old provider is removed.
