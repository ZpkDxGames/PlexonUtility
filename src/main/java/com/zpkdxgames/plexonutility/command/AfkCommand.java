package com.zpkdxgames.plexonutility.command;

import com.zpkdxgames.plexonutility.afk.AfkManager;
import com.zpkdxgames.plexonutility.config.UtilityConfig;
import com.zpkdxgames.plexonutility.feature.Feature;
import com.zpkdxgames.plexonutility.message.MessageService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public final class AfkCommand implements CommandExecutor {
    private final Supplier<UtilityConfig> config;
    private final AfkManager afk;
    private final MessageService messages;

    public AfkCommand(Supplier<UtilityConfig> config, AfkManager afk, MessageService messages) {
        this.config = config;
        this.afk = afk;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length != 0) return false;
        if (!config.get().enabled(Feature.AFK)) {
            messages.send(sender, "feature-disabled");
            return true;
        }
        if (!(sender instanceof Player player)) {
            messages.send(sender, "players-only");
            return true;
        }
        if (!sender.hasPermission("plexonutility.afk")) {
            messages.send(sender, "no-permission");
            return true;
        }
        afk.toggle(player);
        return true;
    }
}
