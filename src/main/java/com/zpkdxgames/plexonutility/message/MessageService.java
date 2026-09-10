package com.zpkdxgames.plexonutility.message;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
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
            "reloaded",
            "reload-failed");

    private final JavaPlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private volatile YamlConfiguration messages;

    public MessageService(JavaPlugin plugin) {
        this.plugin = plugin;
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
        validateCatalog(candidate);
        return candidate;
    }

    public void apply(YamlConfiguration candidate) {
        validateCatalog(candidate);
        messages = candidate;
    }

    public void reload() {
        apply(loadCandidate());
    }

    public void send(CommandSender sender, String key) {
        send(sender, key, Map.of());
    }

    public void send(CommandSender sender, String key, Map<String, String> replacements) {
        YamlConfiguration catalog = messages;
        String prefix = catalog.getString("prefix", "");
        String template = catalog.getString(key, "<red>Missing message: " + key + "</red>");
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            template = template.replace("<" + entry.getKey() + ">", escape(entry.getValue()));
        }
        Component component = miniMessage.deserialize(prefix + template);
        sender.sendMessage(component);
    }

    static void validateCatalog(YamlConfiguration candidate) {
        for (String key : REQUIRED_KEYS) {
            Object raw = candidate.get(key);
            if (!(raw instanceof String)) {
                throw new IllegalArgumentException("messages.yml key '" + key + "' must be a string");
            }
        }
    }

    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("<", "\\<").replace(">", "\\>");
    }
}
