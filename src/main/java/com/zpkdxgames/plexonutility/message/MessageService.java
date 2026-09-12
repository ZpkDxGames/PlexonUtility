package com.zpkdxgames.plexonutility.message;

import com.zpkdxgames.plexoncore.text.TextService;
import com.zpkdxgames.plexoncore.text.TextService.TextMode;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class MessageService {
    static final List<String> REQUIRED_KEYS = List.of(
            "prefix",
            "no-permission",
            "players-only",
            "player-not-found",
            "feature-disabled",
            "cooldown",
            "feed-self",
            "feed-other",
            "heal-self",
            "heal-other",
            "heal-unavailable",
            "enderchest-other",
            "trash-open",
            "afk-self-on",
            "afk-self-off",
            "afk-bossbar",
            "afk-return-actionbar",
            "afk-announcement-on",
            "afk-announcement-off",
            "reloaded",
            "reload-failed",
            "admin-feature-disabled",
            "admin-target-offline",
            "admin-player-unknown",
            "admin-self-action-denied",
            "admin-invalid-duration",
            "admin-invalid-reason",
            "admin-vanish-on",
            "admin-vanish-off",
            "admin-kick-screen",
            "admin-kick-success",
            "admin-ban-success",
            "admin-not-banned",
            "admin-unban-success",
            "admin-prison-not-configured",
            "admin-prison-set",
            "admin-prison-goto",
            "admin-prison-sent",
            "admin-prison-cleared",
            "admin-prison-world-missing",
            "admin-prison-teleport-failed");

    private static final List<String> TEMPLATE_TAGS = List.of(
            "player", "seconds", "reason", "duration", "world", "coordinates", "uuid", "actor", "source", "expiry");

    private final JavaPlugin plugin;
    private final TextService text;
    private final YamlConfiguration defaults;
    private final Map<String, Component> staticRenderCache = new ConcurrentHashMap<>();
    private final Map<String, Component> staticUnprefixedCache = new ConcurrentHashMap<>();
    private volatile YamlConfiguration messages;

    public MessageService(JavaPlugin plugin, TextService text) {
        this.plugin = plugin;
        this.text = text;
        this.defaults = loadBundledDefaults(plugin);
        validateFormatting(defaults);
        this.messages = loadCandidate();
    }

    public YamlConfiguration loadCandidate() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        YamlConfiguration candidate = new YamlConfiguration();
        try {
            candidate.load(file);
        } catch (IOException | InvalidConfigurationException exception) {
            throw new IllegalArgumentException("messages.yml could not be loaded: " + exception.getMessage(), exception);
        }

        int migrated = applyDefaultsAndValidate(candidate, defaults);
        validateFormatting(candidate);
        if (migrated > 0) persistMigratedCatalog(file, candidate, migrated);
        return candidate;
    }

    public void apply(YamlConfiguration candidate) {
        applyDefaultsAndValidate(candidate, defaults);
        validateFormatting(candidate);
        messages = candidate;
        staticRenderCache.clear();
        staticUnprefixedCache.clear();
    }

    public void reload() {
        apply(loadCandidate());
    }

    public void send(CommandSender sender, String key) {
        send(sender, key, Map.of());
    }

    public void send(CommandSender sender, String key, Map<String, String> replacements) {
        sender.sendMessage(render(key, replacements));
    }

    public void sendRaw(CommandSender sender, String trustedTemplate) {
        sendRaw(sender, trustedTemplate, Map.of());
    }

    public void sendRaw(CommandSender sender, String trustedTemplate, Map<String, ?> replacements) {
        sender.sendMessage(text.renderTemplate(prefix() + trustedTemplate, replacements));
    }

    public void broadcast(String key, Map<String, String> replacements) {
        Component component = render(key, replacements);
        for (Player player : plugin.getServer().getOnlinePlayers()) player.sendMessage(component);
    }

    public Component render(String key, Map<String, String> replacements) {
        return renderInternal(key, replacements, true);
    }

    public Component renderUnprefixed(String key, Map<String, String> replacements) {
        return renderInternal(key, replacements, false);
    }

    private Component renderInternal(String key, Map<String, String> replacements, boolean includePrefix) {
        YamlConfiguration catalog = messages;
        String template = catalog.getString(key, "<red>Missing message: " + key + "</red>");
        String complete = includePrefix ? prefix(catalog) + template : template;
        if (replacements == null || replacements.isEmpty()) {
            Map<String, Component> cache = includePrefix ? staticRenderCache : staticUnprefixedCache;
            return cache.computeIfAbsent(key, ignored -> text.render(TextMode.MINIMESSAGE, complete));
        }
        return text.renderTemplate(complete, replacements);
    }

    static int applyDefaultsAndValidate(YamlConfiguration candidate, YamlConfiguration defaults) {
        int migrated = 0;
        for (String key : REQUIRED_KEYS) {
            if (!candidate.isSet(key)) {
                Object bundled = defaults.get(key);
                if (bundled != null) {
                    candidate.set(key, bundled);
                    migrated++;
                }
            }
        }
        candidate.setDefaults(defaults);
        validateCatalog(candidate);
        return migrated;
    }

    static void validateCatalog(YamlConfiguration candidate) {
        for (String key : REQUIRED_KEYS) {
            Object raw = candidate.get(key);
            if (!(raw instanceof String)) {
                throw new IllegalArgumentException("messages.yml key '" + key + "' must be a string");
            }
        }
    }

    private void validateFormatting(YamlConfiguration candidate) {
        for (String key : REQUIRED_KEYS) {
            String value = candidate.getString(key, "");
            String validationTemplate = value;
            for (String tag : TEMPLATE_TAGS) validationTemplate = validationTemplate.replace("<" + tag + ">", "value");
            var result = text.validateMiniMessage(validationTemplate);
            if (!result.valid()) {
                throw new IllegalArgumentException("messages.yml key '" + key + "' has invalid MiniMessage: " + result.reason());
            }
        }
    }

    private void persistMigratedCatalog(File file, YamlConfiguration candidate, int migrated) {
        try {
            candidate.save(file);
            plugin.getLogger().info("Migrated " + migrated + " missing messages.yml key(s) from bundled defaults.");
        } catch (IOException exception) {
            throw new IllegalArgumentException("messages.yml migration could not be saved: " + exception.getMessage(), exception);
        }
    }

    private String prefix() {
        return prefix(messages);
    }

    private static String prefix(YamlConfiguration catalog) {
        return catalog.getString("prefix", "");
    }

    private static YamlConfiguration loadBundledDefaults(JavaPlugin plugin) {
        try (InputStream stream = plugin.getResource("messages.yml")) {
            if (stream == null) throw new IllegalStateException("Bundled messages.yml is missing");
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(stream, StandardCharsets.UTF_8));
            validateCatalog(defaults);
            return defaults;
        } catch (IOException exception) {
            throw new IllegalStateException("Bundled messages.yml could not be read", exception);
        }
    }
}
