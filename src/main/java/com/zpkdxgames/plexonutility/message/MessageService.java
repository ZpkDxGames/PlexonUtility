package com.zpkdxgames.plexonutility.message;

import com.zpkdxgames.plexoncore.text.TextService;
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
            "afk-announcement-on",
            "afk-announcement-off",
            "reloaded",
            "reload-failed");

    private final JavaPlugin plugin;
    private final TextService text;
    private final YamlConfiguration defaults;
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
        applyDefaultsAndValidate(candidate, defaults);
        validateFormatting(candidate);
        return candidate;
    }

    public void apply(YamlConfiguration candidate) {
        applyDefaultsAndValidate(candidate, defaults);
        validateFormatting(candidate);
        messages = candidate;
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
        YamlConfiguration catalog = messages;
        String template = catalog.getString(key, "<red>Missing message: " + key + "</red>");
        return text.renderTemplate(prefix(catalog) + template, replacements);
    }

    static void applyDefaultsAndValidate(YamlConfiguration candidate, YamlConfiguration defaults) {
        candidate.setDefaults(defaults);
        validateCatalog(candidate);
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
            String validationTemplate = value
                    .replace("<player>", "player")
                    .replace("<seconds>", "seconds")
                    .replace("<reason>", "reason");
            var result = text.validateMiniMessage(validationTemplate);
            if (!result.valid()) {
                throw new IllegalArgumentException("messages.yml key '" + key + "' has invalid MiniMessage: " + result.reason());
            }
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
