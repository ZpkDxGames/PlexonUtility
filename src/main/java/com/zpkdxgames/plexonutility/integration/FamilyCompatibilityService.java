package com.zpkdxgames.plexonutility.integration;

import com.zpkdxgames.plexoncore.integration.IntegrationRegistry;
import com.zpkdxgames.plexoncore.integration.IntegrationRegistry.IntegrationState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Lightweight PlexonFamily discovery backed by PlexonCore's shared integration registry.
 * Refreshes only during lifecycle/reload events; there is no polling task.
 */
public final class FamilyCompatibilityService implements Listener {
    private static final Map<String, String> FAMILY = familyMap();

    private final JavaPlugin plugin;
    private final IntegrationRegistry integrations;

    public FamilyCompatibilityService(JavaPlugin plugin, IntegrationRegistry integrations) {
        this.plugin = plugin;
        this.integrations = integrations;
        FAMILY.forEach(integrations::registerKnown);
    }

    public List<FamilyStatus> refresh() {
        integrations.refresh();
        integrations.publish(
                "PLEXON_UTILITY",
                plugin.getName(),
                plugin.getPluginMeta().getVersion(),
                IntegrationState.READY,
                Set.of("utility-api", "afk-state", "afk-event", "placeholderapi", "minimessage", "utility-menu"),
                "PlexonUtility shared utility surface");
        return snapshot();
    }

    public List<FamilyStatus> snapshot() {
        return FAMILY.entrySet().stream()
                .map(entry -> integrations.get(entry.getKey())
                        .map(view -> new FamilyStatus(entry.getKey(), entry.getValue(), view.version(), view.state()))
                        .orElseGet(() -> new FamilyStatus(entry.getKey(), entry.getValue(), "-", IntegrationState.MISSING)))
                .toList();
    }

    public boolean ready(String integrationId) {
        return integrations.ready(integrationId);
    }

    public int readyCount() {
        return (int) snapshot().stream().filter(FamilyStatus::ready).count();
    }

    public int totalCount() {
        return FAMILY.size();
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        if (isFamilyPlugin(event.getPlugin().getName())) refresh();
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        Plugin disabled = event.getPlugin();
        integrationIdFor(disabled.getName()).ifPresent(id -> integrations.publish(
                id,
                disabled.getName(),
                disabled.getPluginMeta().getVersion(),
                IntegrationState.DEGRADED,
                Set.of("plexon-family"),
                "Installed but disabled"));
    }

    static boolean isFamilyPlugin(String pluginName) {
        return FAMILY.containsValue(pluginName);
    }

    static Optional<String> integrationIdFor(String pluginName) {
        return FAMILY.entrySet().stream()
                .filter(entry -> entry.getValue().equalsIgnoreCase(pluginName))
                .map(Map.Entry::getKey)
                .findFirst();
    }

    static Map<String, String> knownFamilyPlugins() {
        return FAMILY;
    }

    private static Map<String, String> familyMap() {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        values.put("PLEXON_BACKPACKS", "PlexonBackpacks");
        values.put("PLEXON_BLACKSMITH", "PlexonBlacksmith");
        values.put("PLEXON_CHATS", "PlexonChats");
        values.put("PLEXON_CRATES", "PlexonCrates");
        values.put("PLEXON_GP_FLAGS", "PlexonGPFlags");
        values.put("PLEXON_HOMES", "PlexonHomes");
        values.put("PLEXON_JOBS", "PlexonJobs");
        values.put("PLEXON_KEYS", "PlexonKeys");
        values.put("PLEXON_PANEL", "PlexonPanel");
        values.put("PLEXON_QUESTS", "PlexonQuests");
        values.put("PLEXON_RANKS", "PlexonRanks");
        values.put("PLEXON_SHOPS", "PlexonShops");
        values.put("PLEXON_SKILLS", "PlexonSkills");
        values.put("PLEXON_SPAWNERS", "PlexonSpawners");
        values.put("PLEXON_TOOLS", "PlexonTools");
        values.put("PLEXON_TRAVEL", "PlexonTravel");
        return Collections.unmodifiableMap(values);
    }

    public record FamilyStatus(String id, String pluginName, String version, IntegrationState state) {
        public boolean ready() {
            return state == IntegrationState.READY;
        }
    }
}
