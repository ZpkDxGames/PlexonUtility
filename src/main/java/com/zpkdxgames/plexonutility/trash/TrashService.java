package com.zpkdxgames.plexonutility.trash;

import com.zpkdxgames.plexoncore.text.TextService;
import com.zpkdxgames.plexoncore.text.TextService.TextMode;
import com.zpkdxgames.plexonutility.config.UtilityConfig;
import com.zpkdxgames.plexonutility.feature.Feature;
import com.zpkdxgames.plexonutility.feedback.FeedbackService;
import com.zpkdxgames.plexonutility.message.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public final class TrashService implements CommandExecutor {
    private static final String TITLE = "<gradient:#57E389:#22D3EE><bold>Trash</bold></gradient> <dark_gray>•</dark_gray> <gray>close to destroy</gray>";

    private final Supplier<UtilityConfig> config;
    private final MessageService messages;
    private final TextService text;
    private final FeedbackService feedback;

    public TrashService(Supplier<UtilityConfig> config, MessageService messages, TextService text) {
        this(config, messages, text, null);
    }

    public TrashService(Supplier<UtilityConfig> config, MessageService messages, TextService text, FeedbackService feedback) {
        this.config = config;
        this.messages = messages;
        this.text = text;
        this.feedback = feedback;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length != 0) return false;
        if (!config.get().enabled(Feature.TRASH)) {
            messages.send(sender, "feature-disabled");
            return true;
        }
        if (!(sender instanceof Player player)) {
            messages.send(sender, "players-only");
            return true;
        }

        TrashHolder holder = new TrashHolder();
        Inventory inventory = Bukkit.createInventory(holder, 27, text.render(TextMode.MINIMESSAGE, TITLE));
        holder.inventory = inventory;
        player.openInventory(inventory);
        if (feedback != null) feedback.success(player, "trash-open");
        else messages.send(player, "trash-open");
        return true;
    }

    private static final class TrashHolder implements InventoryHolder {
        private Inventory inventory;

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
