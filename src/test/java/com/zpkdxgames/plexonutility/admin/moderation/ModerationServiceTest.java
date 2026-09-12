package com.zpkdxgames.plexonutility.admin.moderation;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.zpkdxgames.plexonutility.admin.AdminAuditService;
import com.zpkdxgames.plexonutility.message.MessageService;
import net.kyori.adventure.text.Component;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.ban.ProfileBanList;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ModerationServiceTest {
    private final MessageService messages = mock(MessageService.class);
    private final AdminAuditService audit = mock(AdminAuditService.class);
    private final ModerationService service = new ModerationService(messages, audit);

    @Test void defaultAndCustomReasonAreNormalized() {
        assertEquals("Removed by staff.", service.normalizeReason("", "Removed by staff."));
        assertEquals("Griefing near spawn", service.normalizeReason("  Griefing near spawn  ", "fallback"));
    }

    @Test void multilineAndOverlongReasonsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.normalizeReason("line1\nline2", "fallback"));
        assertThrows(IllegalArgumentException.class, () -> service.normalizeReason("x".repeat(161), "fallback"));
    }

    @Test void kickUsesConfiguredDefaultReasonAndComponentFeedback() {
        CommandSender actor = actor("Console");
        Player target = onlinePlayer("Target");
        Component component = Component.text("Removed by staff.");
        when(messages.renderUnprefixed("admin-kick-screen", Map.of("reason", "Removed by staff."))).thenReturn(component);

        assertEquals(ModerationService.ActionResult.SUCCESS,
                service.kick(actor, target, "", "Removed by staff."));

        verify(target).kick(component);
        verify(audit).log("KICK", actor, target, "reason=Removed by staff.");
    }

    @Test void kickRejectsDisconnectRaceBeforeRenderingOrAuditing() {
        CommandSender actor = actor("Console");
        Player target = mock(Player.class);
        when(target.isOnline()).thenReturn(true);
        when(target.isConnected()).thenReturn(false);

        assertEquals(ModerationService.ActionResult.TARGET_OFFLINE,
                service.kick(actor, target, "reason", "fallback"));

        verify(target, never()).kick(org.mockito.ArgumentMatchers.any(Component.class));
        verify(messages, never()).renderUnprefixed(anyString(), org.mockito.ArgumentMatchers.anyMap());
        verify(audit, never()).log(anyString(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(OfflinePlayer.class), anyString());
    }

    @Test void permanentOnlineBanUsesNativePlayerBanAndActorSource() {
        CommandSender actor = actor("Console");
        OfflinePlayer target = mock(OfflinePlayer.class);
        Player online = onlinePlayer("Target");
        when(target.getPlayer()).thenReturn(online);
        DurationParser.ParsedDuration parsed = DurationParser.parse("perm", 3650);

        assertEquals(ModerationService.ActionResult.SUCCESS,
                service.ban(actor, target, parsed, "Griefing", "fallback"));

        verify(online).ban("Griefing", (Duration) null, "Console", true);
        verify(audit).log("BAN", actor, target, "duration=permanent reason=Griefing");
    }

    @Test void temporaryOfflineBanUsesNativeOfflineProfileAuthority() {
        CommandSender actor = actor("Console");
        OfflinePlayer target = mock(OfflinePlayer.class);
        when(target.getPlayer()).thenReturn(null);
        DurationParser.ParsedDuration parsed = DurationParser.parse("2h", 3650);

        assertEquals(ModerationService.ActionResult.SUCCESS,
                service.ban(actor, target, parsed, "Testing", "fallback"));

        verify(target).ban("Testing", Duration.ofHours(2), "Console");
        verify(audit).log("BAN", actor, target, "duration=2h reason=Testing");
    }

    @Test void existingBanIsUpdatedThroughTheSameNativeAuthority() {
        CommandSender actor = actor("Console");
        OfflinePlayer target = mock(OfflinePlayer.class);
        when(target.isBanned()).thenReturn(true);
        DurationParser.ParsedDuration parsed = DurationParser.parse("1d", 3650);

        assertEquals(ModerationService.ActionResult.SUCCESS,
                service.ban(actor, target, parsed, "Updated reason", "fallback"));

        verify(target).ban("Updated reason", Duration.ofDays(1), "Console");
    }

    @Test void unbanExistingProfilePardonsAndAudits() {
        CommandSender actor = actor("Console");
        OfflinePlayer target = mock(OfflinePlayer.class);
        PlayerProfile profile = mock(PlayerProfile.class);
        ProfileBanList bans = mock(ProfileBanList.class);
        when(target.getPlayerProfile()).thenReturn(profile);
        when(bans.isBanned(profile)).thenReturn(true);

        try (MockedStatic<Bukkit> bukkit = org.mockito.Mockito.mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getBanList(BanList.Type.PROFILE)).thenReturn(bans);
            assertEquals(ModerationService.ActionResult.SUCCESS, service.unban(actor, target));
        }

        verify(bans).pardon(profile);
        verify(audit).log("UNBAN", actor, target, "profile=true");
    }

    @Test void unbanMissingProfileReturnsNotBannedWithoutPardon() {
        CommandSender actor = actor("Console");
        OfflinePlayer target = mock(OfflinePlayer.class);
        PlayerProfile profile = mock(PlayerProfile.class);
        ProfileBanList bans = mock(ProfileBanList.class);
        when(target.getPlayerProfile()).thenReturn(profile);
        when(bans.isBanned(profile)).thenReturn(false);

        try (MockedStatic<Bukkit> bukkit = org.mockito.Mockito.mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getBanList(BanList.Type.PROFILE)).thenReturn(bans);
            assertEquals(ModerationService.ActionResult.NOT_BANNED, service.unban(actor, target));
        }

        verify(bans, never()).pardon(profile);
    }

    private static CommandSender actor(String name) {
        CommandSender actor = mock(CommandSender.class);
        when(actor.getName()).thenReturn(name);
        return actor;
    }

    private static Player onlinePlayer(String name) {
        Player target = mock(Player.class);
        when(target.getName()).thenReturn(name);
        when(target.isOnline()).thenReturn(true);
        when(target.isConnected()).thenReturn(true);
        return target;
    }
}
