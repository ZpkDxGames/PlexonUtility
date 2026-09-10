package com.zpkdxgames.plexonutility;

import com.zpkdxgames.plexoncore.api.PlexonCoreAPI;
import com.zpkdxgames.plexonutility.api.PlexonUtilityAPI;
import com.zpkdxgames.plexonutility.command.UtilityAdminCommand;
import com.zpkdxgames.plexonutility.command.UtilityCommand;
import com.zpkdxgames.plexonutility.config.UtilityConfig;
import com.zpkdxgames.plexonutility.cooldown.CooldownService;
import com.zpkdxgames.plexonutility.feature.Feature;
import com.zpkdxgames.plexonutility.integration.CoreBridge;
import com.zpkdxgames.plexonutility.message.MessageService;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;

public final class PlexonUtilityPlugin extends JavaPlugin implements Listener {
    private volatile UtilityConfig utilityConfig;
    private CooldownService cooldowns;
    private MessageService messages;
    private CoreBridge coreBridge;
    private PlexonUtilityAPI api;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (!new File(getDataFolder(), "messages.yml").exists()) saveResource("messages.yml", false);

        try {
            utilityConfig = loadUtilityConfigCandidate();
            cooldowns = new CooldownService();
            messages = new MessageService(this);
            coreBridge = new CoreBridge(this);
            coreBridge.connect(utilityConfig.enabledFeatures());

            UtilityCommand utilityCommand = new UtilityCommand(this::utilityConfig, cooldowns, messages);
            Objects.requireNonNull(getCommand("feed")).setExecutor(utilityCommand);
            Objects.requireNonNull(getCommand("heal")).setExecutor(utilityCommand);
            Objects.requireNonNull(getCommand("enderchest")).setExecutor(utilityCommand);
            Objects.requireNonNull(getCommand("workbench")).setExecutor(utilityCommand);
            Objects.requireNonNull(getCommand("utilityadmin")).setExecutor(new UtilityAdminCommand(this, cooldowns, messages));

            getServer().getPluginManager().registerEvents(this, this);
            api = new DefaultUtilityAPI();
            getServer().getServicesManager().register(PlexonUtilityAPI.class, api, this, ServicePriority.Normal);
            coreBridge.ready("Enabled features: " + utilityConfig.enabledFeatures());
            getLogger().info("PlexonUtility " + getPluginMeta().getVersion() + " enabled with " + utilityConfig.enabledFeatures());
        } catch (RuntimeException exception) {
            getLogger().severe("PlexonUtility failed to initialize: " + detail(exception));
            if (coreBridge != null) coreBridge.degraded("Initialization failed: " + detail(exception));
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (cooldowns != null) cooldowns.clearAll();
        if (coreBridge != null) coreBridge.disconnect();
        if (getServer() != null) getServer().getServicesManager().unregisterAll(this);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (cooldowns != null) cooldowns.clear(event.getPlayer().getUniqueId());
    }

    public void reloadUtilityState() {
        UtilityConfig candidateConfig = loadUtilityConfigCandidate();
        YamlConfiguration candidateMessages = messages.loadCandidate();

        // Both candidates are fully parsed and validated before either live reference changes.
        messages.apply(candidateMessages);
        utilityConfig = candidateConfig;

        if (coreBridge != null && coreBridge.core() != null) {
            coreBridge.ready("Reloaded; enabled features: " + candidateConfig.enabledFeatures());
        }
    }

    public UtilityConfig utilityConfig() {
        return utilityConfig;
    }

    public PlexonCoreAPI core() {
        return coreBridge == null ? null : coreBridge.core();
    }

    private UtilityConfig loadUtilityConfigCandidate() {
        File file = new File(getDataFolder(), "config.yml");
        YamlConfiguration candidate = new YamlConfiguration();
        try {
            candidate.load(file);
        } catch (IOException | InvalidConfigurationException exception) {
            throw new IllegalArgumentException("config.yml could not be loaded: " + exception.getMessage(), exception);
        }
        return UtilityConfig.from(candidate);
    }

    private static String detail(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
    }

    private final class DefaultUtilityAPI implements PlexonUtilityAPI {
        @Override
        public String version() {
            return getPluginMeta().getVersion();
        }

        @Override
        public boolean isEnabled(Feature feature) {
            return utilityConfig.enabled(feature);
        }

        @Override
        public Set<Feature> enabledFeatures() {
            return utilityConfig.enabledFeatures();
        }
    }
}
