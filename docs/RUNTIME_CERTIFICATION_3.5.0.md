# PlexonUtility 3.5.0 Runtime Certification Protocol

This document is the live/staging gate for the exact PlexonUtility 3.5.0 candidate. Source CI is necessary but does not certify runtime deployment.

## Candidate identity

Before deployment, record and independently verify:

- implementation commit from `releases/3.5.0-runtime-smoke.txt`;
- `PlexonUtility-3.5.0.jar` SHA-256 equals `candidate_jar_sha256`;
- PlexonCore is exactly 2.1.0;
- PlexonCore JAR SHA-256 equals `7ee823ded87d5be9c62426b04571c0d0d6b11c138575ca2c91838586c9f7576c`;
- Java 25;
- Paper `26.2.build.121-stable`.

Do not rebuild or substitute the Utility JAR for runtime testing. Deploy the exact CI candidate.

## Evidence rules

Only change a matrix field from `PENDING` to `PASS` after that complete section passes against the exact candidate. Record concrete observations in `notes=` or an attached operator log.

If implementation/resources change after candidate pinning, invalidate the runtime evidence and generate a new candidate. Documentation/workflow-only changes are allowed only when CI proves the rebuilt JAR still exactly matches the pinned candidate hash.

## 1. Startup and plugin-list matrix

Evidence field: `startup_and_plugin_list=PASS`

Verify:

- server boots cleanly with exact Core 2.1.0 and Utility 3.5.0;
- Utility reports enabled rather than degraded/failed;
- `/utilityadmin diagnostics` shows expected runtime generation and Core 2.1 state;
- exactly one PlexonUtility instance is loaded;
- PlaceholderAPI status matches installation state;
- no stale PlexonCore 2.0.5 assumption appears in startup errors/warnings;
- no unexpected exception/error from Utility, Core, WildStacker integration, or migration.

Record plugin list and relevant startup log excerpt/reference.

## 2. Migration matrix

Evidence field: `migration_matrix=PASS`

Run both paths:

1. fresh 3.5 install with no existing Utility files;
2. copied 3.4 Utility data upgraded by the exact 3.5 candidate.

Verify:

- `config.yml` remains valid and custom values survive;
- missing 3.5 config defaults are added only after successful runtime commit;
- `messages.yml` custom strings survive and required new keys are added;
- backups are created before operator-file rewrites;
- `admin-data.yml` schema 1 is backed up before schema 2 migration;
- prison UUID/name data and persistent vanish data remain correct;
- reload after migration succeeds without a mixed generation;
- restart reads the migrated files cleanly.

Keep the pre-3.5 Utility data backup for rollback.

## 3. Duplicate command / ownership matrix

Evidence field: `duplicate_command_warnings=PASS`

With the real plugin set:

- inspect startup for duplicate/override command warnings;
- confirm PlexonUtility does not own root `/spawn`;
- confirm PlexonTravel remains root `/spawn` authority;
- confirm Utility owns `/spawnmob`;
- confirm `/ec`, `/feed`, `/fly`, and admin commands resolve to the intended owner.

If Essentials is still present, explicitly record any alias/command-map collision.

## 4. AFK matrix

Evidence field: `afk_matrix=PASS`

Verify:

- OP/staff without explicit `plexonutility.afk.auto.bypass` can auto-timeout;
- ordinary player auto-times out after configured period;
- explicit bypass prevents automatic AFK;
- `/afk` manual on/off transitions once per action;
- movement across a block boundary resets AFK; yaw/pitch-only movement does not cause churn;
- chat, non-AFK command, interaction, inventory interaction, block break/place reset as configured;
- outgoing player-caused damage resets AFK;
- incoming damage does **not** reset AFK;
- repeated reloads preserve exactly one expected AFK coordinator;
- no duplicate AFK transition broadcasts/events;
- `%plexonutility_afk%` and `%plexonutility_is_afk%` are correct when PlaceholderAPI is present.

## 5. Rank permission simulation

Evidence field: `rank_permission_matrix=PASS`

Until the matching PlexonRanks release assigns these nodes, use temporary LuckPerms test assignments.

Verify from a no-node baseline:

- `/ec`, `/fly`, `/feed`, and `/heal` self-service are denied;
- granting `plexonutility.enderchest` enables `/ec`;
- granting `plexonutility.fly` enables self `/fly`;
- granting `plexonutility.feed` enables `/feed`;
- staff other-target permissions remain separate;
- `plexonutility.admin.killall` does not implicitly grant `plexonutility.admin.killall.bypass-confirm`.

Remove temporary test assignments after certification.

## 6. Flight lifecycle matrix

Evidence field: `flight_lifecycle_matrix=PASS`

Verify Utility-owned Survival/Adventure flight:

- enable/disable normally;
- revoke `plexonutility.fly` while flying and confirm Utility-owned flight is removed;
- feature/reload disable removes Utility-owned flight;
- quit/reconnect reconciles stale ownership correctly;
- plugin disable/shutdown relinquishes Utility-owned flight;
- Creative/Spectator flight is not stripped;
- externally owned flight is not claimed/stripped;
- diagnostics/task count remains bounded.

## 7. Reload generation matrix

Evidence field: `reload_generation_matrix=PASS`

Verify:

- valid `/utilityadmin reload` advances generation exactly once;
- invalid config/message candidate is rejected without replacing the active generation;
- a simulated persistence failure leaves the previous runtime generation active and reports degradation;
- later successful reload clears the runtime-generation failure state;
- capabilities in Core reflect the accepted generation only;
- repeated reloads do not accumulate AFK/flight/entity tasks.

## 8. Entity cleanup matrix

Evidence field: `killall_matrix=PASS`

Verify:

- normal removable entity;
- named/tamed/villager/armor-stand/display/plugin-marked protections;
- boss protected by default and removable only through explicit boss selection;
- bounded radius selection;
- explicit world selection and permission;
- confirmation expiry;
- exact candidate plan: entities spawned after preview are not removed;
- execution revalidation: changed/protected/out-of-scope candidate is not removed;
- preview ceiling fails closed when exceeded;
- large population is batched rather than removed in one tick;
- reload/plugin disable does not leave runaway coordinator work;
- player entities are never removable;
- no loot/XP/kill-credit side effect is intentionally generated.

## 9. WildStacker matrix

Evidence field: `wildstacker_matrix=PASS`

Run with the production WildStacker version:

- remove a normal size-1 stackable living entity;
- remove a larger logical stack;
- verify the WildStacker stack object is removed rather than only the Bukkit representative;
- verify user/audit logical counts match the logical stack amount;
- verify protected entities remain protected;
- temporarily reproduce API-unavailable/incompatible handling in staging if practical and confirm fail-closed behavior;
- verify no duplicate same-type stack remains because Utility removed only the representative.

## 10. Spawnmob matrix

Evidence field: `spawnmob_matrix=PASS`

Verify:

- valid living type;
- invalid/non-living/player type rejected;
- amount 1 and amount 100;
- configured lower maximum;
- large request visibly completes through bounded batches;
- no forced unsafe chunk load/generation;
- no per-entity task fan-out;
- one aggregate success/partial-failure result.

## 11. Clearinventory matrix

Evidence field: `clearinventory_matrix=PASS`

Verify:

- self clear works directly;
- other-player clear requires the separate others permission;
- first other-target command only previews/arms confirmation;
- confirmation expiry prevents clear;
- changing target inventory between preview and confirmation forces reconfirmation;
- storage, armor, and offhand clear together;
- exact custom ItemStacks do not duplicate/drop into unintended locations.

## 12. AdminDataStore persistence matrix

Evidence field: `admin_data_persistence_matrix=PASS`

Verify in staging:

- prison/vanish state persists through restart;
- induced write failure becomes visible/degraded;
- retry accounting is visible;
- later full-snapshot success recovers persistence health;
- current/persisted revisions converge;
- pending writes return to zero;
- delayed/stalled I/O does not create an unbounded shutdown wait;
- no silent data loss or success message is emitted before required persistence succeeds.

## 13. Prison matrix

Evidence field: `prison_matrix=PASS`

Verify:

- set/status/goto/send/clear;
- durable set/clear reporting;
- world UUID is authoritative with name fallback;
- renamed world resolves correctly when UUID is unchanged;
- unloaded target area uses async teleport semantics without synchronous chunk fan-out;
- actor/target disconnect during async teleport produces safe failure handling;
- restart retains valid prison identity.

## 14. Vanish presentation matrix

Evidence field: `vanish_presentation_matrix=PASS`

Verify:

- visible → vanished emits one transition;
- vanished → visible emits one transition;
- repeated same-state requests emit none;
- ordinary audience gets configured synthetic presence;
- actor and `plexonutility.admin.vanish.see` viewers are excluded as intended;
- persisted vanished reconnect is reconciled;
- real join/quit suppression does not create duplicate synthetic messages;
- current Utility fallback renders correctly;
- a test listener handling `SyntheticPresencePresentationEvent` suppresses the fallback;
- DiscordSRV behavior is observed and recorded;
- no fake Bukkit join/quit lifecycle event is dispatched.

## 15. Invsee regression matrix

Evidence field: `invsee_regression_matrix=PASS`

Verify:

- view-only permission;
- edit permission;
- storage, armor, and offhand mapping;
- exact custom ItemStack metadata/components survive edits;
- two concurrent viewers remain synchronized;
- target disconnect closes/stops stale mutation safely;
- unsupported transfer gestures remain blocked;
- no duplication/loss regression.

## 16. Essentials ownership status

Evidence field must end as either:

- `essentials_decommission=READY`, or
- `essentials_decommission=BLOCKED_DOCUMENTED`.

For `READY`, prove every active Essentials responsibility has a verified owner and perform the disabled-staging boot where practical.

For `BLOCKED_DOCUMENTED`, record the exact remaining owners/data/dependencies that prevent removal. Stable Utility publication may proceed, but Essentials removal must not be claimed.

In either case confirm PlexonTravel `/spawn` remains correct.

## 17. Spark performance soak

Evidence fields:

- `spark_result=PASS`
- `spark_evidence=<report/reference>`

Run at least 30 minutes on the exact candidate with representative player/entity/admin activity. Record:

- TPS;
- MSPT;
- scheduler/task counts;
- thread count;
- heap trend;
- entity cleanup/spawn bursts;
- AdminDataStore pending/dirty state;
- warnings/errors.

No HIGH/CRITICAL defect may remain.

## Final evidence commit

After every required section passes:

1. set `result=PASS`;
2. fill `certified_at` and `environment`;
3. set every required matrix field to `PASS`;
4. record `spark_evidence`;
5. set truthful `essentials_decommission`;
6. keep the exact `candidate_jar_sha256`, Core version, and Core hash unchanged;
7. commit **only** `releases/3.5.0-runtime-smoke.txt` after the implementation commit.

The stable workflow verifies that the implementation commit is an ancestor, the runtime-evidence file is the only post-candidate source change, and the deterministically rebuilt JAR SHA-256 equals the certified candidate.
