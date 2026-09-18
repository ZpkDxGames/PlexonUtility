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
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validated message catalog. Bundled messages.yml is the schema authority; candidate preparation
 * never writes operator files. Persistence happens only after the surrounding runtime generation
 * has committed successfully.
 */
public final class MessageService {
    private static final Pattern SIMPLE_TAG = Pattern.compile("<(/?)([A-Za-z0-9_-]+)>");
    private static final Set<String> MINIMESSAGE_SIMPLE_TAGS = Set.of(
            "black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple", "gold", "gray", "dark_gray",
            "blue", "green", "aqua", "red", "light_purple", "yellow", "white",
            "bold", "b", "italic", "i", "underlined", "u", "strikethrough", "st", "obfuscated", "obf", "reset",
            "color", "colour", "gradient", "rainbow");

    private final JavaPlugin plugin;
    private final TextService text;
    private final YamlConfiguration defaults;
    private final Set<String> requiredKeys;
    private final Map<String, Component> staticRenderCache = new ConcurrentHashMap<>();
    private final Map<String, Component> staticUnprefixedCache = new ConcurrentHashMap<>();
    private volatile YamlConfiguration messages;

    public MessageService(JavaPlugin plugin, TextService text) {
        this.plugin = plugin;
        this.text = text;
        this.defaults = loadBundledDefaults(plugin);
        this.requiredKeys = requiredKeys(defaults);
        validateCatalog(defaults, requiredKeys);
        validateFormatting(defaults);
        this.messages = copy(defaults);
    }

    /** Prepares and validates a catalog without mutating disk or the active runtime catalog. */
    public Candidate prepareCandidate() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        boolean existed = file.isFile();
        YamlConfiguration candidate = new YamlConfiguration();
        if (existed) {
            try {
                candidate.load(file);
            } catch (IOException | InvalidConfigurationException exception) {
                throw new IllegalArgumentException("messages.yml could not be loaded: " + exception.getMessage(), exception);
            }
        }

        int migrated = applyDefaultsAndValidate(candidate, defaults);
        validateFormatting(candidate);
        return new Candidate(candidate, migrated, existed);
    }

    /** Compatibility name retained for internal callers; still side-effect-free. */
    public YamlConfiguration loadCandidate() {
        return prepareCandidate().catalog();
    }

    public void apply(Candidate candidate) {
        if (candidate == null) throw new IllegalArgumentException("candidate");
        apply(candidate.catalog());
    }

    public void apply(YamlConfiguration candidate) {
        applyDefaultsAndValidate(candidate, defaults);
        validateFormatting(candidate);
        messages = candidate;
        staticRenderCache.clear();
        staticUnprefixedCache.clear();
    }

    /** Returns an independent copy suitable for rollback. */
    public YamlConfiguration snapshot() {
        return copy(messages);
    }

    /**
     * Persists only a committed migration. Existing operator files are backed up immediately before
     * rewrite; a fresh file is simply created.
     */
    public void persistCommittedMigration(Candidate candidate) {
        if (candidate == null || candidate.migrated() <= 0) return;
        File file = new File(plugin.getDataFolder(), "messages.yml");
        try {
            Files.createDirectories(file.toPath().getParent());
            if (candidate.sourceExisted() && file.isFile()) {
                Files.copy(file.toPath(), new File(plugin.getDataFolder(), "messages.yml.bak").toPath(),
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
            }
            candidate.catalog().save(file);
            plugin.getLogger().info("Migrated " + candidate.migrated()
                    + " messages.yml schema key(s) from bundled defaults.");
        } catch (IOException exception) {
            throw new IllegalArgumentException("messages.yml migration could not be saved: " + exception.getMessage(), exception);
        }
    }

    public void reload() {
        Candidate candidate = prepareCandidate();
        apply(candidate);
        persistCommittedMigration(candidate);
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
        Set<String> required = requiredKeys(defaults);
        int migrated = 0;
        for (String key : required) {
            Object raw = candidate.get(key);
            if (raw == null) {
                candidate.set(key, defaults.get(key));
                migrated++;
            } else if (!(raw instanceof String)) {
                throw new IllegalArgumentException("messages.yml key '" + key + "' must be a string");
            }
        }
        validateCatalog(candidate, required);
        return migrated;
    }

    static Set<String> requiredKeys(YamlConfiguration defaults) {
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        for (Map.Entry<String, Object> entry : defaults.getValues(true).entrySet()) {
            if (entry.getValue() instanceof String) keys.add(entry.getKey());
        }
        if (keys.isEmpty()) throw new IllegalArgumentException("Bundled messages.yml contains no message strings");
        return Set.copyOf(keys);
    }

    static void validateCatalog(YamlConfiguration candidate) {
        validateCatalog(candidate, requiredKeys(candidate));
    }

    private static void validateCatalog(YamlConfiguration candidate, Set<String> required) {
        for (String key : required) {
            Object raw = candidate.get(key);
            if (!(raw instanceof String)) {
                throw new IllegalArgumentException("messages.yml key '" + key + "' must be a string");
            }
        }
    }

    private void validateFormatting(YamlConfiguration candidate) {
        for (String key : requiredKeys) {
            String value = candidate.getString(key);
            String validationTemplate = replaceRuntimeTags(value == null ? "" : value);
            var result = text.validateMiniMessage(validationTemplate);
            if (!result.valid()) {
                throw new IllegalArgumentException("messages.yml key '" + key
                        + "' has invalid MiniMessage: " + result.reason());
            }
        }
    }

    private static String replaceRuntimeTags(String template) {
        Matcher matcher = SIMPLE_TAG.matcher(template);
        StringBuffer output = new StringBuffer();
        while (matcher.find()) {
            boolean closing = !matcher.group(1).isEmpty();
            String name = matcher.group(2).toLowerCase(java.util.Locale.ROOT);
            if (!closing && !MINIMESSAGE_SIMPLE_TAGS.contains(name)) {
                matcher.appendReplacement(output, "value");
            } else {
                matcher.appendReplacement(output, Matcher.quoteReplacement(matcher.group()));
            }
        }
        matcher.appendTail(output);
        return output.toString();
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
            return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw new IllegalStateException("Bundled messages.yml could not be read", exception);
        }
    }

    private static YamlConfiguration copy(YamlConfiguration source) {
        YamlConfiguration copy = new YamlConfiguration();
        try {
            copy.loadFromString(source.saveToString());
            return copy;
        } catch (InvalidConfigurationException exception) {
            throw new IllegalStateException("Unable to copy validated message catalog", exception);
        }
    }

    public record Candidate(YamlConfiguration catalog, int migrated, boolean sourceExisted) {
        public Candidate {
            if (catalog == null) throw new IllegalArgumentException("catalog");
            if (migrated < 0) throw new IllegalArgumentException("migrated");
        }
    }
}
