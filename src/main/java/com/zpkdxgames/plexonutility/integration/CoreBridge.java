package com.zpkdxgames.plexonutility.integration;

import com.zpkdxgames.plexoncore.api.PlexonCoreAPI;
import com.zpkdxgames.plexoncore.module.ModuleRegistry.ModuleDescriptor;
import com.zpkdxgames.plexoncore.module.ModuleRegistry.ModuleState;
import com.zpkdxgames.plexoncore.module.ModuleRegistry.ModuleVersionRange;
import com.zpkdxgames.plexonutility.feature.Feature;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

public final class CoreBridge {
    public static final String MODULE_ID = "utility";

    private final JavaPlugin plugin;
    private PlexonCoreAPI core;

    public CoreBridge(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public PlexonCoreAPI connect(Set<Feature> enabledFeatures) {
        ServicesManager services = plugin.getServer().getServicesManager();
        core = services.load(PlexonCoreAPI.class);
        if (core == null) throw new IllegalStateException("PlexonCore API service is unavailable");
        if (!core.supportsApi(2, 0)) {
            throw new IllegalStateException("PlexonCore API 2.0 is required; running " + core.version().apiVersion());
        }

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

        ModuleDescriptor descriptor = new ModuleDescriptor(
                MODULE_ID,
                "PlexonUtility",
                plugin.getName(),
                plugin.getPluginMeta().getVersion(),
                plugin,
                ModuleVersionRange.parse(">=2.0 <3.0"),
                capabilities,
                ModuleState.STARTING,
                "Initializing Core-native utility services",
                Instant.now());

        var result = core.modules().register(descriptor);
        if (!result.success()) throw new IllegalStateException(result.message());
        return core;
    }

    public void ready(String detail) {
        if (core != null) core.modules().updateState(MODULE_ID, plugin, ModuleState.READY, detail);
    }

    public void degraded(String detail) {
        if (core != null) core.modules().updateState(MODULE_ID, plugin, ModuleState.DEGRADED, detail);
    }

    public void disconnect() {
        if (core != null) {
            core.modules().unregisterOwnedBy(plugin);
            core = null;
        }
    }

    public PlexonCoreAPI core() {
        return core;
    }
}
