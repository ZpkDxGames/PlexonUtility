package com.zpkdxgames.plexonutility.command;

import com.zpkdxgames.plexonutility.admin.player.PlayerManagementService;
import com.zpkdxgames.plexonutility.config.UtilityConfig;
import com.zpkdxgames.plexonutility.feature.Feature;
import com.zpkdxgames.plexonutility.message.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlayerAdminCommandTest {
    @Test void selfGamemodeNeedsOnlyBasePermission() {
        Player actor = player("Staff");
        when(actor.hasPermission("plexonutility.admin.gamemode")).thenReturn(true);
        MessageService messages = mock(MessageService.class);
        PlayerManagementService players = mock(PlayerManagementService.class);
        PlayerAdminCommand executor = new PlayerAdminCommand(PlayerAdminCommandTest::enabledConfig, messages, players);
        Command command = command("gamemode");

        executor.onCommand(actor, command, "gamemode", new String[] {"creative"});

        verify(players).setGameMode(actor, actor, GameMode.CREATIVE);
        verify(messages, never()).send(actor, "no-permission");
    }

    @Test void targetingAnotherPlayerRequiresOthersPermission() {
        Player actor = player("Staff");
        Player target = player("Target");
        when(actor.hasPermission("plexonutility.admin.gamemode")).thenReturn(true);
        when(actor.hasPermission("plexonutility.admin.gamemode.others")).thenReturn(false);
        when(target.isOnline()).thenReturn(true);
        when(target.isConnected()).thenReturn(true);
        MessageService messages = mock(MessageService.class);
        PlayerManagementService players = mock(PlayerManagementService.class);
        PlayerAdminCommand executor = new PlayerAdminCommand(PlayerAdminCommandTest::enabledConfig, messages, players);

        try (MockedStatic<Bukkit> bukkit = org.mockito.Mockito.mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayerExact("Target")).thenReturn(target);
            executor.onCommand(actor, command("gamemode"), "gamemode", new String[] {"creative", "Target"});
        }

        verify(players, never()).setGameMode(actor, target, GameMode.CREATIVE);
        verify(messages).send(actor, "no-permission");
    }

    @Test void disabledCapabilityRejectsBeforeMutation() {
        Player actor = player("Staff");
        when(actor.hasPermission("plexonutility.admin.god")).thenReturn(true);
        MessageService messages = mock(MessageService.class);
        PlayerManagementService players = mock(PlayerManagementService.class);
        PlayerAdminCommand executor = new PlayerAdminCommand(PlayerAdminCommandTest::godDisabledConfig, messages, players);

        executor.onCommand(actor, command("god"), "god", new String[0]);

        verify(players, never()).toggleGodMode(actor, actor);
        verify(messages).send(actor, "admin-feature-disabled");
    }

    private static Command command(String name) {
        Command command = mock(Command.class);
        when(command.getName()).thenReturn(name);
        return command;
    }

    private static Player player(String name) {
        Player player = mock(Player.class);
        when(player.getName()).thenReturn(name);
        when(player.getUniqueId()).thenReturn(UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)));
        return player;
    }

    private static UtilityConfig enabledConfig() {
        return config(UtilityConfig.PlayerManagementConfig.defaults());
    }

    private static UtilityConfig godDisabledConfig() {
        UtilityConfig.PlayerManagementConfig defaults = UtilityConfig.PlayerManagementConfig.defaults();
        return config(new UtilityConfig.PlayerManagementConfig(
                defaults.gamemodeEnabled(), defaults.flyEnabled(), false, false,
                defaults.speedEnabled(), defaults.clearInventoryEnabled(), defaults.anvilEnabled()));
    }

    private static UtilityConfig config(UtilityConfig.PlayerManagementConfig playerManagement) {
        return new UtilityConfig(
                EnumSet.allOf(Feature.class), 20, 20.0F, true, 0L,
                true, false, Set.of(), 0L,
                UtilityConfig.AfkConfig.defaults(), UtilityConfig.FeedbackConfig.defaults(),
                new UtilityConfig.AdminConfig(true, UtilityConfig.VanishConfig.defaults(),
                        UtilityConfig.ModerationConfig.defaults(), true, true,
                        UtilityConfig.EntityManagementConfig.defaults(), playerManagement));
    }
}
