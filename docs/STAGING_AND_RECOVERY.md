# Staging and Recovery

## Staging

1. Back up the current Essentials JAR/config and server command aliases.
2. Install PlexonCore 2.0.x and PlexonUtility 1.0.0 on staging.
3. Verify `/utilityadmin diagnostics` reports Core READY and the four selected features enabled.
4. Inspect actual command ownership for `/feed`, `/heal`, `/ec`, and `/workbench`.
5. Validate self and elevated-target permissions.
6. Audit Vault economy provider and all plugins depending on Essentials before disabling Essentials.
7. Only after every remaining Essentials feature has an owner, boot staging with Essentials physically disabled/removed.
8. Validate Homes, Travel, Shops, Jobs, Quests, Ranks, balances, and other critical plugins.
9. Run a 30-minute soak and profile idle/active MSPT before production decommission.

## Rollback

PlexonUtility itself stores no persistent data.

If a utility regression occurs:

1. Stop the server.
2. Remove/disable PlexonUtility.
3. Restore the backed-up Essentials JAR/config and previous command ownership/aliases.
4. Start the server and verify the former command provider.

If the problem is only a feature, disable that feature in `config.yml` while preserving the rest of PlexonUtility.
