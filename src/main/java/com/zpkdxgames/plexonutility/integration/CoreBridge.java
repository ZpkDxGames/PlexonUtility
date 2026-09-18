package com.zpkdxgames.plexonutility.integration;

import com.zpkdxgames.plexoncore.api.PlexonCoreAPI;
import com.zpkdxgames.plexoncore.module.ModuleRegistry.ModuleDescriptor;
import com.zpkdxgames.plexoncore.module.ModuleRegistry.ModuleState;
import com.zpkdxgames.plexoncore.module.ModuleRegistry.ModuleVersionRange;
import com.zpkdxgames.plexonutility.config.UtilityConfig;
import com.zpkdxgames.plexonutility.feature.Feature;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

public final class CoreBridge {
    public static final String MODULE_ID = "utility";
    public static final String REQUIRED_CORE_VERSION = "2.1.0";

    private final JavaPlugin plugin;
    private PlexonCoreAPI core;

    public CoreBridge(JavaPlugin plugin) { this.plugin = plugin; }

    public PlexonCoreAPI connect(Set<Feature> enabledFeatures) {
        return connect(enabledFeatures, UtilityConfig.AdminConfig.defaults());
    }

    public PlexonCoreAPI connect(Set<Feature> enabledFeatures, UtilityConfig.AdminConfig admin) {
        ServicesManager services = plugin.getServer().getServicesManager();
        core = services.load(PlexonCoreAPI.class);
        if (core == null) throw new IllegalStateException("PlexonCore API service is unavailable");
        if (!core.supportsApi(2, 1)) {
            throw new IllegalStateException("PlexonCore API 2.1 is required; running " + core.version().apiVersion());
        }
        if (!REQUIRED_CORE_VERSION.equals(core.version().pluginVersion())) {
            throw new IllegalStateException("PlexonCore " + REQUIRED_CORE_VERSION
                    + " is required; running " + core.version().pluginVersion());
        }

        ModuleDescriptor descriptor = descriptor(enabledFeatures, admin, ModuleState.STARTING,
                "Initializing Core 2.1-native utility services");
        var result = core.modules().register(descriptor);
        if (!result.success()) throw new IllegalStateException(result.message());
        return core;
    }

    /** Refreshes the complete descriptor so capabilities cannot remain stale after a config reload. */
    public synchronized void refreshCapabilities(UtilityConfig config, String detail) {
        if (core == null) return;
        ModuleDescriptor previous = core.modules().find(MODULE_ID).orElse(null);
        if (previous == null || previous.plugin() != plugin) {
            throw new IllegalStateException("PlexonUtility no longer owns its Core module descriptor");
        }

        ModuleDescriptor replacement = descriptor(
                config.enabledFeatures(), config.admin(), ModuleState.READY, detail);
        core.modules().unregisterOwnedBy(plugin);
        var result = core.modules().register(replacement);
        if (!result.success()) {
            // Best-effort rollback to the exact previous descriptor.
            core.modules().register(previous);
            throw new IllegalStateException("Core capability refresh failed: " + result.message());
        }
    }

    public void ready(String detail) {
        if (core != null && !core.modules().updateState(MODULE_ID, plugin, ModuleState.READY, detail)) {
            throw new IllegalStateException("PlexonUtility no longer owns its Core module descriptor");
        }
    }

    public void degraded(String detail) {
        if (core != null) core.modules().updateState(MODULE_ID, plugin, ModuleState.DEGRADED, detail);
    }

    public void disconnect() {
        if (core != null) {
            core.scheduler().purgeOwner(plugin);
            core.modules().unregisterOwnedBy(plugin);
            core = null;
        }
    }

    public PlexonCoreAPI core() { return core; }

    private ModuleDescriptor descriptor(Set<Feature> enabledFeatures, UtilityConfig.AdminConfig admin,
                                        ModuleState state, String detail) {
        return new ModuleDescriptor(
                MODULE_ID, "PlexonUtility", plugin.getName(), plugin.getPluginMeta().getVersion(), plugin,
                ModuleVersionRange.parse(">=2.1 <3.0"), capabilities(enabledFeatures, admin), state,
                detail, Instant.now());
    }

    static Set<String> capabilities(Set<Feature> enabledFeatures, UtilityConfig.AdminConfig admin) {
        Set<String> capabilities = new LinkedHashSet<>();
        enabledFeatures.stream().map(Feature::id).forEach(capabilities::add);
        capabilities.add("utility-api");
        capabilities.add("afk-state");
        capabilities.add("afk-event");
        capabilities.add("placeholderapi");
        capabilities.add("quiet-feedback");
        capabilities.add("core-text");
        capabilities.add("core-gui");
        capabilities.add("core-scheduler");
        capabilities.add("core-integrations");
        capabilities.add("family-compatibility");
        capabilities.add("complement-diagnostics");

        if (admin.enabled()) {
            capabilities.add("admin-toolkit");
            if (admin.inventoryInspectionEnabled()) capabilities.add("editable-invsee");
            if (admin.prisonEnabled()) capabilities.add("prison-waypoint");
            if (admin.moderation().kickEnabled() || admin.moderation().banEnabled()) {
                capabilities.add("native-profile-moderation");
            }
            if (admin.vanish().enabled()) {
                capabilities.add("native-vanish");
                capabilities.add("vanish-event");
                if (admin.vanish().syntheticPresence().enabled()) capabilities.add("synthetic-presence");
            }
            if (admin.entityManagement().enabled()) {
                capabilities.add("entity-management");
                if (admin.entityManagement().killall().enabled()) capabilities.add("entity-cleanup");
                if (admin.entityManagement().spawnmob().enabled()) capabilities.add("entity-spawn");
            }
            UtilityConfig.PlayerManagementConfig player = admin.playerManagement();
            if (player.gamemodeEnabled() || player.flyEnabled() || player.godEnabled()
                    || player.speedEnabled() || player.clearInventoryEnabled()) capabilities.add("player-admin");
            if (player.gamemodeEnabled()) capabilities.add("gamemode-control");
            if (player.flyEnabled()) capabilities.add("flight-control");
            if (player.godEnabled()) capabilities.add("god-mode");
            if (player.speedEnabled()) capabilities.add("speed-control");
            if (player.clearInventoryEnabled()) capabilities.add("inventory-clear");
        }
        return Set.copyOf(capabilities);
    }
}
