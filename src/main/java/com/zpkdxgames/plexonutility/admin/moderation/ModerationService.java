package com.zpkdxgames.plexonutility.admin.moderation;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.zpkdxgames.plexonutility.admin.AdminAuditService;
import com.zpkdxgames.plexonutility.message.MessageService;
import org.bukkit.BanEntry;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.ban.ProfileBanList;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Native profile-ban and component-kick authority for the intentionally small Utility admin surface. */
public final class ModerationService {
    public static final int MAX_REASON_LENGTH = 160;

    private final MessageService messages;
    private final AdminAuditService audit;

    public ModerationService(MessageService messages, AdminAuditService audit) {
        this.messages = Objects.requireNonNull(messages, "messages");
        this.audit = Objects.requireNonNull(audit, "audit");
    }

    public Optional<OfflinePlayer> resolveKnown(String exactName) {
        if (exactName == null || exactName.isBlank()) return Optional.empty();
        Player online = Bukkit.getPlayerExact(exactName);
        if (online != null) return Optional.of(online);
        OfflinePlayer cached = Bukkit.getOfflinePlayerIfCached(exactName);
        if (cached == null || cached.getName() == null || !cached.getName().equalsIgnoreCase(exactName)) return Optional.empty();
        return Optional.of(cached);
    }

    public String normalizeReason(String input, String fallback) {
        String reason = input == null || input.isBlank() ? fallback : input.trim();
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("Reason must not be blank");
        if (reason.length() > MAX_REASON_LENGTH) throw new IllegalArgumentException("Reason must be at most " + MAX_REASON_LENGTH + " characters");
        if (reason.indexOf('\n') >= 0 || reason.indexOf('\r') >= 0) throw new IllegalArgumentException("Reason must be a single line");
        return reason;
    }

    public ActionResult kick(CommandSender actor, Player target, String reason, String fallbackReason) {
        if (target == null || !target.isOnline() || !target.isConnected()) return ActionResult.TARGET_OFFLINE;
        String normalized = normalizeReason(reason, fallbackReason);
        target.kick(messages.renderUnprefixed("admin-kick-screen", Map.of("reason", normalized)));
        audit.log("KICK", actor, target, "reason=" + normalized);
        return ActionResult.SUCCESS;
    }

    public ActionResult ban(CommandSender actor, OfflinePlayer target, DurationParser.ParsedDuration parsed,
                            String reason, String fallbackReason) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(parsed, "parsed");
        String normalized = normalizeReason(reason, fallbackReason);
        String source = AdminAuditService.source(actor);
        Duration duration = parsed.permanent() ? null : parsed.duration();
        Player online = target.getPlayer();
        if (online != null && online.isOnline() && online.isConnected()) {
            online.ban(normalized, duration, source, true);
        } else {
            target.ban(normalized, duration, source);
        }
        audit.log("BAN", actor, target, "duration=" + parsed.display() + " reason=" + normalized);
        return ActionResult.SUCCESS;
    }

    public ActionResult unban(CommandSender actor, OfflinePlayer target) {
        Objects.requireNonNull(target, "target");
        return unbanProfile(actor, target.getPlayerProfile(), target);
    }

    public ActionResult unbanProfile(CommandSender actor, PlayerProfile profile) {
        return unbanProfile(actor, profile, null);
    }

    private ActionResult unbanProfile(CommandSender actor, PlayerProfile profile, OfflinePlayer knownTarget) {
        ProfileBanList bans = profileBans();
        if (!bans.isBanned(profile)) return ActionResult.NOT_BANNED;
        bans.pardon(profile);
        if (knownTarget != null) audit.log("UNBAN", actor, knownTarget, "profile=true");
        else audit.log("UNBAN", actor, "target_profile=" + profileLabel(profile));
        return ActionResult.SUCCESS;
    }

    public boolean isBanned(OfflinePlayer target) {
        return target != null && target.isBanned();
    }

    public List<BanView> activeBans() {
        List<BanView> result = new ArrayList<>();
        for (BanEntry<?> raw : profileBans().getEntries()) {
            Object target = raw.getBanTarget();
            if (!(target instanceof PlayerProfile profile)) continue;
            Date expiration = raw.getExpiration();
            result.add(new BanView(
                    profile,
                    profileLabel(profile),
                    raw.getReason() == null ? "Unspecified" : raw.getReason(),
                    raw.getSource(),
                    expiration == null ? null : expiration.toInstant().toEpochMilli()));
        }
        result.sort(Comparator.comparing(BanView::label, String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(result);
    }

    @SuppressWarnings("deprecation")
    private static ProfileBanList profileBans() {
        return (ProfileBanList) Bukkit.getBanList(BanList.Type.PROFILE);
    }

    private static String profileLabel(PlayerProfile profile) {
        String name = profile.getName();
        UUID id = profile.getId();
        if (name != null && !name.isBlank()) return name;
        return id == null ? "Unknown profile" : id.toString();
    }

    public enum ActionResult {
        SUCCESS,
        TARGET_OFFLINE,
        NOT_BANNED
    }

    public record BanView(PlayerProfile profile, String label, String reason, String source, Long expirationEpochMillis) {
        public boolean permanent() {
            return expirationEpochMillis == null;
        }
    }
}
