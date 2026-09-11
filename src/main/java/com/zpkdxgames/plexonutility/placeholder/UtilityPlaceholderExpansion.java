package com.zpkdxgames.plexonutility.placeholder;

import com.zpkdxgames.plexonutility.afk.AfkTracker;
import com.zpkdxgames.plexonutility.config.UtilityConfig;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public final class UtilityPlaceholderExpansion extends PlaceholderExpansion {
    private final String version;
    private final Supplier<UtilityConfig> config;
    private final AfkTracker tracker;

    public UtilityPlaceholderExpansion(String version, Supplier<UtilityConfig> config, AfkTracker tracker) {
        this.version = version;
        this.config = config;
        this.tracker = tracker;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "plexonutility";
    }

    @Override
    public @NotNull String getAuthor() {
        return "ZpkDxGames";
    }

    @Override
    public @NotNull String getVersion() {
        return version;
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String identifier) {
        boolean afk = player != null && tracker.isAfk(player.getUniqueId());
        if (identifier.equalsIgnoreCase("afk")) {
            UtilityConfig.AfkConfig afkConfig = config.get().afk();
            return afk ? afkConfig.placeholderAfk() : afkConfig.placeholderActive();
        }
        if (identifier.equalsIgnoreCase("is_afk")) {
            return Boolean.toString(afk);
        }
        return null;
    }
}
