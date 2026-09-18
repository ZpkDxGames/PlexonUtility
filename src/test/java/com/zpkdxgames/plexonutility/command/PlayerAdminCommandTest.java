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
import static org.mockito.Mockito.times;
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

    @Test void selfFlightUsesProgressionPermissionRatherThanAdminFly() {
        Player actor = player("RankedPlayer");
        when(actor.hasPermission("plexonutility.fly")).thenReturn(true);
        when(actor.hasPermission("plexonutility.admin.fly")).thenReturn(false);
        MessageService messages = mock(MessageService.class);
        PlayerManagementService players = mock(PlayerManagementService.class);
        when(players.canManageFlight(actor)).thenReturn(true);
        when(players.toggleFlight(actor, actor)).thenReturn(true);
        PlayerAdminCommand executor = new PlayerAdminCommand(PlayerAdminCommandTest::enabledConfig, messages, players);

        executor.onCommand(actor, command("fly"), "fly", new String[0]);

        verify(players).toggleFlight(actor, actor);
        verify(messages, never()).send(actor, "no-permission");
    }

    @Test void selfFlightWithoutProgressionPermissionIsDeniedEvenWithLegacyAdminFly() {
        Player actor = player("LegacyAdmin");
        when(actor.hasPermission("plexonutility.fly")).thenReturn(false);
        when(actor.hasPermission("plexonutility.admin.fly")).thenReturn(true);
        MessageService messages = mock(MessageService.class);
        PlayerManagementService players = mock(PlayerManagementService.class);
        PlayerAdminCommand executor = new PlayerAdminCommand(PlayerAdminCommandTest::enabledConfig, messages, players);

        executor.onCommand(actor, command("fly"), "fly", new String[0]);

        verify(players, never()).toggleFlight(actor, actor);
        verify(messages).send(actor, "no-permission");
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

    @Test void otherPlayerClearInventoryRequiresExplicitSecondCommand() {
        Player actor = player("Staff");
        Player target = player("Target");
        when(actor.hasPermission("plexonutility.admin.clearinventory")).thenReturn(true);
        when(actor.hasPermission("plexonutility.admin.clearinventory.others")).thenReturn(true);
        when(target.isOnline()).thenReturn(true);
        when(target.isConnected()).thenReturn(true);
        MessageService messages = mock(MessageService.class);
        PlayerManagementService players = mock(PlayerManagementService.class);
        when(players.inventoryFingerprint(target)).thenReturn(12345);
        when(players.occupiedStacks(target)).thenReturn(7);
        when(players.clearInventory(actor, target)).thenReturn(7);
        PlayerAdminCommand executor = new PlayerAdminCommand(PlayerAdminCommandTest::enabledConfig, messages, players);

        try (MockedStatic<Bukkit> bukkit = org.mockito.Mockito.mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayerExact("Target")).thenReturn(target);
            bukkit.when(() -> Bukkit.getPlayer(target.getUniqueId())).thenReturn(target);

            executor.onCommand(actor, command("clearinventory"), "clearinventory", new String[] {"Target"});
            verify(players, never()).clearInventory(actor, target);
            verify(messages).send(actor, "admin-clearinventory-confirm",
                    java.util.Map.of("player", "Target", "count", "7", "seconds", "15"));

            executor.onCommand(actor, command("clearinventory"), "clearinventory", new String[] {"confirm"});
        }

        verify(players, times(1)).clearInventory(actor, target);
        verify(messages).send(actor, "admin-clearinventory-success",
                java.util.Map.of("player", "Target", "count", "7"));
    }

    @Test void changedTargetInventoryRequiresReconfirmationBeforeClear() {
        Player actor = player("Staff");
        Player target = player("Target");
        when(actor.hasPermission("plexonutility.admin.clearinventory")).thenReturn(true);
        when(actor.hasPermission("plexonutility.admin.clearinventory.others")).thenReturn(true);
        when(target.isOnline()).thenReturn(true);
        when(target.isConnected()).thenReturn(true);
        MessageService messages = mock(MessageService.class);
        PlayerManagementService players = mock(PlayerManagementService.class);
        when(players.inventoryFingerprint(target)).thenReturn(100, 200, 200);
        when(players.occupiedStacks(target)).thenReturn(4, 5);
        when(players.clearInventory(actor, target)).thenReturn(5);
        PlayerAdminCommand executor = new PlayerAdminCommand(PlayerAdminCommandTest::enabledConfig, messages, players);

        try (MockedStatic<Bukkit> bukkit = org.mockito.Mockito.mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayerExact("Target")).thenReturn(target);
            bukkit.when(() -> Bukkit.getPlayer(target.getUniqueId())).thenReturn(target);

            executor.onCommand(actor, command("clearinventory"), "clearinventory", new String[] {"Target"});
            executor.onCommand(actor, command("clearinventory"), "clearinventory", new String[] {"confirm"});

            verify(players, never()).clearInventory(actor, target);
            verify(messages).send(actor, "admin-clearinventory-changed",
                    java.util.Map.of("player", "Target", "count", "5", "seconds", "15"));

            executor.onCommand(actor, command("clearinventory"), "clearinventory", new String[] {"confirm"});
        }

        verify(players, times(1)).clearInventory(actor, target);
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
