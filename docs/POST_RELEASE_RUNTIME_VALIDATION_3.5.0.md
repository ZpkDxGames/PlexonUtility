# PlexonUtility 3.5.0 Post-Release Runtime Validation

PlexonUtility 3.5.0 is a full stable release. This document is an operational validation checklist for the published stable JAR; it is not a prerelease gate, candidate stage, RC, preview, or snapshot process.

## Stable artifact identity

Validate the downloaded GitHub Release asset itself:

- `PlexonUtility-3.5.0.jar`;
- Java 25;
- Paper `26.2.build.121-stable`;
- exact PlexonCore 2.1.0;
- PlexonCore SHA-256 `7ee823ded87d5be9c62426b04571c0d0d6b11c138575ca2c91838586c9f7576c`;
- Utility SHA-256 must match the published `SHA256SUMS.txt`.

Record observations in `releases/3.5.0-runtime-validation.txt`.

## Startup and migration

Validate a clean install and a 3.4 → 3.5 upgrade. Confirm config/messages custom values survive, backups are created before migrations, `admin-data.yml` schema 1 safely becomes schema 2, persistent vanish/prison state remains valid, and restart/reload reads the migrated files without warnings or mixed runtime generations.

## Command ownership

Confirm PlexonUtility does not own root `/spawn`, PlexonTravel remains authoritative for `/spawn`, Utility owns `/spawnmob`, and no unexpected Essentials/plugin command collisions appear at startup.

## AFK

Validate ordinary timeout, OP/staff without explicit bypass, explicit `plexonutility.afk.auto.bypass`, manual AFK, configured activity reset sources, outgoing-damage activity, incoming-damage non-activity, exactly one AFK coordinator after reloads, and no duplicate transitions.

## Progression permissions

From a no-node baseline, confirm self `/ec`, `/fly`, `/feed`, and `/heal` are denied. Temporarily grant `plexonutility.enderchest`, `plexonutility.fly`, and `plexonutility.feed` to verify the intended rank contract. Staff other-target permissions must remain separate.

## Flight lifecycle

Validate Utility-owned Survival/Adventure flight through enable/disable, permission revoke, reload/feature disable, quit/reconnect, and plugin shutdown. Creative/Spectator and externally owned flight must not be stripped.

## Reload generation

Validate successful reload generation advancement, invalid config/message rejection, rollback to the last accepted generation, Core capability consistency, and no accumulation of AFK/flight/entity tasks.

## Entity cleanup

Validate removable entities, protected named/tamed/villager/display/armor-stand/plugin-owned entities, bosses, radius/world scope, confirmation expiry, immutable preview membership, execution revalidation, the configured preview ceiling, large-population batching, player protection, and no combat/loot/XP simulation.

## WildStacker

With the production WildStacker version, validate size-1 and multi-entity stacks, logical removal counts, full stack-object removal, protected entities, and fail-closed behavior if the API cannot be resolved safely.

## Spawnmob

Validate valid/invalid types, amount 1 and amount 100, configured lower caps, bounded multi-tick spawning, already-loaded chunk behavior, and aggregate completion reporting.

## Clearinventory and invsee

Validate self and other-target clear behavior, separate other-target permission, timed confirmation, inventory fingerprint reconfirmation, storage/armor/offhand clearing, live invsee view/edit separation, exact custom ItemStack metadata/components, concurrent viewers, target disconnect safety, and no duplication/loss.

## AdminDataStore and prison

Validate prison/vanish restart persistence, induced write failure visibility, retry/recovery, revision convergence, zero pending writes after recovery, bounded shutdown under delayed I/O, prison set/status/goto/send/clear, UUID-first world identity, name fallback, renamed-world behavior, async teleporting, and disconnect races.

## Vanish presentation

Validate visible ↔ vanished transition-only presentation, repeated same-state suppression, staff exclusion, persisted reconnect, real join/quit suppression, Utility fallback, an external listener handling `SyntheticPresencePresentationEvent`, DiscordSRV behavior, and absence of fake Bukkit join/quit lifecycle events.

## Essentials ownership

Essentials removal remains a separate server-architecture decision. Confirm live owners for Homes, Travel, nicknames, kits, mail, Vault economy, and hard plugin dependencies before removing Essentials. PlexonUtility stable status does not imply Essentials is removable.

## Performance soak

Run at least 30 minutes with Spark on the published stable JAR under representative player/entity/admin activity. Record TPS, MSPT, scheduler/task counts, threads, heap trend, cleanup/spawn bursts, AdminDataStore pending/dirty state, and warnings/errors.

No HIGH/CRITICAL runtime defect should remain unaddressed. If a defect is found, fix it in the next full semantic stable release rather than creating an RC/snapshot channel for the same release.
