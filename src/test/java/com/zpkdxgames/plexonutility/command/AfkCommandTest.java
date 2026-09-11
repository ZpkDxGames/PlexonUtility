package com.zpkdxgames.plexonutility.command;

import com.zpkdxgames.plexonutility.afk.AfkManager;
import com.zpkdxgames.plexonutility.config.UtilityConfig;
import com.zpkdxgames.plexonutility.message.MessageService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AfkCommandTest {
    @Test
    void afkCommandTogglesCurrentPlayer() {
        UtilityConfig config = UtilityConfig.from(new YamlConfiguration());
        AfkManager manager = mock(AfkManager.class);
        MessageService messages = mock(MessageService.class);
        Player player = mock(Player.class);
        when(player.hasPermission("plexonutility.afk")).thenReturn(true);
        Command command = mock(Command.class);
        AfkCommand executor = new AfkCommand(() -> config, manager, messages);

        assertTrue(executor.onCommand(player, command, "afk", new String[0]));
        verify(manager).toggle(player);
    }

    @Test
    void consoleCannotToggleAfk() {
        UtilityConfig config = UtilityConfig.from(new YamlConfiguration());
        AfkManager manager = mock(AfkManager.class);
        MessageService messages = mock(MessageService.class);
        CommandSender console = mock(CommandSender.class);
        AfkCommand executor = new AfkCommand(() -> config, manager, messages);

        assertTrue(executor.onCommand(console, mock(Command.class), "afk", new String[0]));
        verify(messages).send(console, "players-only");
    }
}
