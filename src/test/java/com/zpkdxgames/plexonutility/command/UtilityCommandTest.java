package com.zpkdxgames.plexonutility.command;

import com.zpkdxgames.plexonutility.config.UtilityConfig;
import com.zpkdxgames.plexonutility.cooldown.CooldownService;
import com.zpkdxgames.plexonutility.feature.Feature;
import com.zpkdxgames.plexonutility.message.MessageService;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UtilityCommandTest {
    @Test
    void feedSelfAppliesConfiguredValues() {
        Player player = player("Alex");
        MessageService messages = mock(MessageService.class);
        UtilityCommand executor = executor(config(0L, 0L), messages, ignored -> null);

        assertTrue(executor.onCommand(player, command("feed"), "feed", new String[0]));

        verify(player).setFoodLevel(20);
        verify(player).setSaturation(20.0F);
        verify(player).setExhaustion(0.0F);
        verify(messages).send(player, "feed-self", Map.of("player", "Alex"));
    }

    @Test
    void feedTargetRequiresOthersPermission() {
        CommandSender sender = mock(CommandSender.class);
        Player target = player("Target");
        MessageService messages = mock(MessageService.class);
        UtilityCommand executor = executor(config(0L, 0L), messages, ignored -> target);

        assertTrue(executor.onCommand(sender, command("feed"), "feed", new String[]{"Target"}));

        verify(messages).send(sender, "no-permission");
        verify(target, never()).setFoodLevel(20);
    }

    @Test
    void feedTargetUsesExactInjectedLookupAndOthersPermission() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("plexonutility.feed.others")).thenReturn(true);
        Player target = player("Target");
        MessageService messages = mock(MessageService.class);
        UtilityCommand executor = executor(config(0L, 0L), messages, name -> name.equals("Target") ? target : null);

        assertTrue(executor.onCommand(sender, command("feed"), "feed", new String[]{"Target"}));

        verify(target).setFoodLevel(20);
        verify(messages).send(sender, "feed-other", Map.of("player", "Target"));
    }

    @Test
    void healUsesActualMaxHealthAndClearsFire() {
        Player player = player("Alex");
        when(player.isValid()).thenReturn(true);
        AttributeInstance maxHealth = mock(AttributeInstance.class);
        when(maxHealth.getValue()).thenReturn(40.0D);
        when(player.getAttribute(Attribute.MAX_HEALTH)).thenReturn(maxHealth);
        MessageService messages = mock(MessageService.class);
        UtilityCommand executor = executor(config(0L, 0L), messages, ignored -> null);

        assertTrue(executor.onCommand(player, command("heal"), "heal", new String[0]));

        verify(player).setHealth(40.0D);
        verify(player).setFireTicks(0);
        verify(messages).send(player, "heal-self", Map.of("player", "Alex"));
    }

    @Test
    void healRejectsDeadOrInvalidPlayerState() {
        Player player = player("Alex");
        when(player.isDead()).thenReturn(true);
        MessageService messages = mock(MessageService.class);
        UtilityCommand executor = executor(config(0L, 0L), messages, ignored -> null);

        assertTrue(executor.onCommand(player, command("heal"), "heal", new String[0]));

        verify(player, never()).setHealth(org.mockito.ArgumentMatchers.anyDouble());
        verify(messages).send(player, "heal-unavailable", Map.of("player", "Alex"));
    }

    @Test
    void enderChestSelfOpensTheRealPlayerInventory() {
        Player player = player("Alex");
        Inventory enderChest = mock(Inventory.class);
        when(player.getEnderChest()).thenReturn(enderChest);
        MessageService messages = mock(MessageService.class);
        UtilityCommand executor = executor(config(0L, 0L), messages, ignored -> null);

        assertTrue(executor.onCommand(player, command("enderchest"), "enderchest", new String[0]));

        verify(player).openInventory(enderChest);
    }

    @Test
    void workbenchOpensWithoutPersistentInventoryState() {
        Player player = player("Alex");
        MessageService messages = mock(MessageService.class);
        UtilityCommand executor = executor(config(0L, 0L), messages, ignored -> null);

        assertTrue(executor.onCommand(player, command("workbench"), "workbench", new String[0]));

        verify(player).openWorkbench(null, true);
    }

    @Test
    void selfCooldownBlocksImmediateRepeatWithoutCreatingSchedulers() {
        Player player = player("Alex");
        MessageService messages = mock(MessageService.class);
        UtilityCommand executor = executor(config(5_000_000_000L, 0L), messages, ignored -> null);

        assertTrue(executor.onCommand(player, command("feed"), "feed", new String[0]));
        assertTrue(executor.onCommand(player, command("feed"), "feed", new String[0]));

        verify(messages).send(org.mockito.ArgumentMatchers.eq(player), org.mockito.ArgumentMatchers.eq("cooldown"), anyMap());
    }

    @Test
    void extraArgumentsReturnUsageFailureDeterministically() {
        Player player = player("Alex");
        MessageService messages = mock(MessageService.class);
        UtilityCommand executor = executor(config(0L, 0L), messages, ignored -> null);

        assertFalse(executor.onCommand(player, command("feed"), "feed", new String[]{"one", "two"}));
        verify(player, never()).setFoodLevel(20);
    }

    private static UtilityCommand executor(UtilityConfig config, MessageService messages, java.util.function.Function<String, Player> lookup) {
        return new UtilityCommand(() -> config, new CooldownService(), messages, lookup);
    }

    private static UtilityConfig config(long feedCooldown, long healCooldown) {
        return new UtilityConfig(
                EnumSet.allOf(Feature.class),
                20,
                20.0F,
                true,
                feedCooldown,
                true,
                false,
                Set.of(),
                healCooldown);
    }

    private static Player player(String name) {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.nameUUIDFromBytes(name.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        when(player.getName()).thenReturn(name);
        return player;
    }

    private static Command command(String name) {
        Command command = mock(Command.class);
        when(command.getName()).thenReturn(name);
        return command;
    }
}
