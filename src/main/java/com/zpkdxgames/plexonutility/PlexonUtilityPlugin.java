package com.zpkdxgames.plexonutility;

import com.zpkdxgames.plexoncore.api.PlexonCoreAPI;
import com.zpkdxgames.plexonutility.admin.AdminAuditService;
import com.zpkdxgames.plexonutility.admin.AdminDataStore;
import com.zpkdxgames.plexonutility.admin.gui.AdminMenuService;
import com.zpkdxgames.plexonutility.admin.inventory.InventoryInspectionService;
import com.zpkdxgames.plexonutility.admin.moderation.ModerationService;
import com.zpkdxgames.plexonutility.admin.prison.PrisonService;
import com.zpkdxgames.plexonutility.admin.vanish.VanishListener;
import com.zpkdxgames.plexonutility.admin.vanish.VanishService;
import com.zpkdxgames.plexonutility.afk.AfkManager;
import com.zpkdxgames.plexonutility.afk.AfkTracker;
import com.zpkdxgames.plexonutility.api.PlexonUtilityAPI;
import com.zpkdxgames.plexonutility.command.AdminActionCommand;
import com.zpkdxgames.plexonutility.command.AfkCommand;
import com.zpkdxgames.plexonutility.command.UtilityAdminCommand;
import com.zpkdxgames.plexonutility.command.UtilityCommand;
import com.zpkdxgames.plexonutility.config.UtilityConfig;
import com.zpkdxgames.plexonutility.cooldown.CooldownService;
import com.zpkdxgames.plexonutility.feature.Feature;
import com.zpkdxgames.plexonutility.feedback.FeedbackService;
import com.zpkdxgames.plexonutility.integration.ComplementService;
import com.zpkdxgames.plexonutility.integration.CoreBridge;
import com.zpkdxgames.plexonutility.integration.FamilyCompatibilityService;
import com.zpkdxgames.plexonutility.menu.UtilityMenuService;
import com.zpkdxgames.plexonutility.message.MessageService;
import com.zpkdxgames.plexonutility.placeholder.UtilityPlaceholderExpansion;
import com.zpkdxgames.plexonutility.service.PlayerUtilityService;
import com.zpkdxgames.plexonutility.trash.TrashService;
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
import java.util.UUID;

public final class PlexonUtilityPlugin extends JavaPlugin implements Listener {
    private volatile UtilityConfig utilityConfig;
    private CooldownService cooldowns;
    private MessageService messages;
    private FeedbackService feedback;
    private CoreBridge coreBridge;
    private ComplementService complements;
    private FamilyCompatibilityService family;
    private AfkTracker afkTracker;
    private AfkManager afkManager;
    private AdminDataStore adminData;
    private VanishService vanishService;
    private UtilityPlaceholderExpansion placeholderExpansion;
    private PlexonUtilityAPI api;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();
        if (!new File(getDataFolder(), "messages.yml").exists()) saveResource("messages.yml", false);

        try {
            utilityConfig = loadUtilityConfigCandidate();
            cooldowns = new CooldownService();
            coreBridge = new CoreBridge(this);
            PlexonCoreAPI core = coreBridge.connect(utilityConfig.enabledFeatures());
            messages = new MessageService(this, core.text());
            feedback = new FeedbackService(this, this::utilityConfig, messages);

            complements = new ComplementService(getServer().getPluginManager(), core.integrations());
            complements.refresh();
            family = new FamilyCompatibilityService(this, core.integrations());
            family.refresh();

            afkTracker = new AfkTracker();
            afkManager = new AfkManager(this, this::utilityConfig, feedback, afkTracker, core.scheduler());

            PlayerUtilityService playerUtilities = new PlayerUtilityService(this::utilityConfig, cooldowns, messages, feedback);
            UtilityCommand utilityCommand = new UtilityCommand(this::utilityConfig, messages, playerUtilities);
            Objects.requireNonNull(getCommand("feed")).setExecutor(utilityCommand);
            Objects.requireNonNull(getCommand("heal")).setExecutor(utilityCommand);
            Objects.requireNonNull(getCommand("enderchest")).setExecutor(utilityCommand);
            Objects.requireNonNull(getCommand("workbench")).setExecutor(utilityCommand);
            Objects.requireNonNull(getCommand("afk")).setExecutor(new AfkCommand(this::utilityConfig, afkManager, messages));

            UtilityMenuService utilityMenu = new UtilityMenuService(
                    this::utilityConfig, messages, core.gui(), core.text(), afkManager, family);
            Objects.requireNonNull(getCommand("utility")).setExecutor(utilityMenu);
            Objects.requireNonNull(getCommand("trash")).setExecutor(
                    new TrashService(this::utilityConfig, messages, core.text(), feedback));

            adminData = new AdminDataStore(this, core.scheduler());
            adminData.load();
            AdminAuditService audit = new AdminAuditService(getLogger());
            vanishService = new VanishService(this, this::utilityConfig, adminData, audit);
            ModerationService moderation = new ModerationService(messages, audit);
            PrisonService prison = new PrisonService(this, adminData, audit);
            InventoryInspectionService inventory = new InventoryInspectionService(
                    core.gui(), utilityMenu.itemFactory(), messages);
            AdminMenuService adminMenu = new AdminMenuService(
                    this, this::utilityConfig, messages, core.gui(), core.scheduler(), utilityMenu.itemFactory(),
                    utilityMenu, afkManager, vanishService, moderation, prison, inventory, playerUtilities);
            utilityMenu.setAdminCenterOpener(adminMenu::open);

            AdminActionCommand adminActions = new AdminActionCommand(
                    this::utilityConfig, messages, inventory, vanishService, moderation, prison);
            Objects.requireNonNull(getCommand("invsee")).setExecutor(adminActions);
            Objects.requireNonNull(getCommand("vanish")).setExecutor(adminActions);
            Objects.requireNonNull(getCommand("kick")).setExecutor(adminActions);
            Objects.requireNonNull(getCommand("ban")).setExecutor(adminActions);
            Objects.requireNonNull(getCommand("unban")).setExecutor(adminActions);
            Objects.requireNonNull(getCommand("prison")).setExecutor(adminActions);
            Objects.requireNonNull(getCommand("utilityadmin")).setExecutor(
                    new UtilityAdminCommand(this, cooldowns, messages, afkManager, complements, family, adminMenu));

            getServer().getPluginManager().registerEvents(this, this);
            getServer().getPluginManager().registerEvents(afkManager, this);
            getServer().getPluginManager().registerEvents(family, this);
            getServer().getPluginManager().registerEvents(new VanishListener(this::utilityConfig, vanishService), this);
            getServer().getPluginManager().registerEvents(inventory, this);
            afkManager.start();
            registerPlaceholderExpansion();

            api = new DefaultUtilityAPI();
            getServer().getServicesManager().register(PlexonUtilityAPI.class, api, this, ServicePriority.Normal);
            coreBridge.ready("Core text/gui/scheduler/integrations shared; admin-toolkit=" + utilityConfig.admin().enabled()
                    + "; family-ready=" + family.readyCount() + "/" + family.totalCount()
                    + "; enabled features: " + utilityConfig.enabledFeatures());
            getLogger().info("PlexonUtility " + getPluginMeta().getVersion() + " enabled with " + utilityConfig.enabledFeatures()
                    + "; Core-native text/gui/scheduler/integrations; admin-toolkit=" + utilityConfig.admin().enabled()
                    + "; family-ready=" + family.readyCount() + "/" + family.totalCount()
                    + "; AFK scheduler=" + afkManager.schedulerCount()
                    + "; PlaceholderAPI=" + (placeholderExpansion != null));
        } catch (RuntimeException exception) {
            getLogger().severe("PlexonUtility failed to initialize: " + detail(exception));
            if (coreBridge != null) coreBridge.degraded("Initialization failed: " + detail(exception));
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (placeholderExpansion != null) {
            placeholderExpansion.unregister();
            placeholderExpansion = null;
        }
        if (vanishService != null) vanishService.restoreOwnedVisibility();
        if (adminData != null) adminData.close();
        if (afkManager != null) afkManager.close();
        if (feedback != null) feedback.close();
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

        messages.apply(candidateMessages);
        utilityConfig = candidateConfig;
        if (afkManager != null) afkManager.reload();
        if (vanishService != null) vanishService.reload();
        if (complements != null) complements.refresh();
        if (family != null) family.refresh();

        if (coreBridge != null && coreBridge.core() != null) {
            coreBridge.ready("Reloaded; admin-toolkit=" + candidateConfig.admin().enabled()
                    + "; family-ready=" + (family == null ? "0/0" : family.readyCount() + "/" + family.totalCount())
                    + "; enabled features: " + candidateConfig.enabledFeatures());
        }
    }

    public UtilityConfig utilityConfig() {
        return utilityConfig;
    }

    public PlexonCoreAPI core() {
        return coreBridge == null ? null : coreBridge.core();
    }

    public boolean placeholderRegistered() {
        return placeholderExpansion != null;
    }

    private void registerPlaceholderExpansion() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) return;
        UtilityPlaceholderExpansion expansion = new UtilityPlaceholderExpansion(
                getPluginMeta().getVersion(), this::utilityConfig, afkTracker,
                id -> vanishService != null && vanishService.isVanished(id));
        if (expansion.register()) {
            placeholderExpansion = expansion;
            getLogger().info("PlaceholderAPI expansion registered: %plexonutility_afk%, %plexonutility_is_afk%, %plexonutility_vanished%");
        } else {
            getLogger().warning("PlaceholderAPI was present but the PlexonUtility expansion could not be registered.");
        }
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

        @Override
        public boolean isAfk(UUID playerId) {
            return afkManager != null && afkManager.isAfk(playerId);
        }

        @Override
        public boolean isVanished(UUID playerId) {
            return vanishService != null && vanishService.isVanished(playerId);
        }
    }
}
