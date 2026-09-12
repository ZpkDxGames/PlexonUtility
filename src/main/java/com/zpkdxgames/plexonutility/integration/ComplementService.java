package com.zpkdxgames.plexonutility.integration;

import com.zpkdxgames.plexoncore.integration.IntegrationRegistry;
import com.zpkdxgames.plexoncore.integration.IntegrationRegistry.IntegrationState;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public final class ComplementService {
    public enum Category {
        LAND_SECURITY("land-security", "Land & regions", List.of("GriefPrevention", "Lands", "Towny", "WorldGuard")),
        BLOCK_AUDIT("block-audit", "Audit & rollback", List.of("CoreProtect")),
        PERMISSIONS("permissions", "Permissions", List.of("LuckPerms")),
        CUSTOM_MENUS("custom-menus", "Custom menus", List.of("DeluxeMenus", "ChestCommands")),
        ANTI_CHEAT("anti-cheat", "Anti-cheat", List.of("GrimAC", "Vulcan")),
        DISPLAY_HUD("display-hud", "Display & HUD", List.of("TAB", "FancyHolograms", "DecentHolograms")),
        PERFORMANCE("performance", "Profiling & pre-generation", List.of("spark", "Chunky")),
        VOICE("voice", "Proximity voice", List.of("voicechat", "SimpleVoiceChat"));

        private final String id;
        private final String label;
        private final List<String> providers;

        Category(String id, String label, List<String> providers) {
            this.id = id;
            this.label = label;
            this.providers = providers;
        }

        public String id() { return id; }
        public String label() { return label; }
        public List<String> providers() { return providers; }
    }

    public record Provider(String name, String version) {}

    public record Status(Category category, List<Provider> detected) {
        public Status {
            detected = List.copyOf(detected);
        }

        public boolean detectedAny() {
            return !detected.isEmpty();
        }

        public String providerSummary() {
            if (detected.isEmpty()) return "none detected";
            return detected.stream().map(provider -> provider.name() + " " + provider.version()).reduce((a, b) -> a + ", " + b).orElse("none detected");
        }
    }

    private final PluginManager plugins;
    private final IntegrationRegistry integrations;

    public ComplementService(PluginManager plugins, IntegrationRegistry integrations) {
        this.plugins = plugins;
        this.integrations = integrations;
    }

    public List<Status> refresh() {
        List<Status> statuses = snapshot(this::enabledVersion);
        for (Status status : statuses) publish(status);
        return statuses;
    }

    public List<Status> snapshot() {
        return snapshot(this::enabledVersion);
    }

    static List<Status> snapshot(Function<String, String> versionLookup) {
        List<Status> statuses = new ArrayList<>();
        for (Category category : Category.values()) {
            List<Provider> detected = new ArrayList<>();
            for (String provider : category.providers()) {
                String version = versionLookup.apply(provider);
                if (version != null) detected.add(new Provider(provider, version));
            }
            statuses.add(new Status(category, detected));
        }
        return List.copyOf(statuses);
    }

    private String enabledVersion(String name) {
        Plugin plugin = plugins.getPlugin(name);
        return plugin != null && plugin.isEnabled() ? plugin.getPluginMeta().getVersion() : null;
    }

    private void publish(Status status) {
        String id = "UTILITY_" + status.category().name();
        String provider = status.detectedAny()
                ? status.detected().stream().map(Provider::name).reduce((a, b) -> a + ", " + b).orElse("unknown")
                : "No provider detected";
        String version = status.detected().size() == 1 ? status.detected().getFirst().version() : (status.detectedAny() ? "multiple" : "-");
        integrations.publish(
                id,
                provider,
                version,
                status.detectedAny() ? IntegrationState.READY : IntegrationState.MISSING,
                Set.of("utility-complement", status.category().id()),
                status.detectedAny() ? status.category().label() + " available" : status.category().label() + " has no external provider detected");
    }
}
