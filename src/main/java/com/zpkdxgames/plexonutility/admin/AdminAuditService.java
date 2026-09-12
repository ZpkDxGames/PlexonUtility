package com.zpkdxgames.plexonutility.admin;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.logging.Logger;

/** Concise server-log audit trail for destructive and visibility-changing admin actions. */
public final class AdminAuditService {
    private final Logger logger;

    public AdminAuditService(Logger logger) {
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public void log(String action, CommandSender actor, OfflinePlayer target, String details) {
        StringBuilder line = new StringBuilder("ADMIN action=").append(clean(action));
        if (actor != null) {
            line.append(" actor=").append(clean(actor.getName()));
            if (actor instanceof Player player) line.append(" actor_uuid=").append(player.getUniqueId());
        }
        if (target != null) {
            String name = target.getName();
            line.append(" target=").append(clean(name == null ? "unknown" : name));
            line.append(" target_uuid=").append(target.getUniqueId());
        }
        if (details != null && !details.isBlank()) line.append(' ').append(clean(details));
        logger.info(line.toString());
    }

    public void log(String action, CommandSender actor, String details) {
        log(action, actor, null, details);
    }

    public static String source(CommandSender actor) {
        if (actor == null) return "PlexonUtility";
        if (actor instanceof Player player) return player.getName() + "/" + player.getUniqueId();
        return actor.getName();
    }

    private static String clean(String value) {
        if (value == null) return "-";
        return value.replace('\n', ' ').replace('\r', ' ').replace('\t', ' ').trim();
    }
}
