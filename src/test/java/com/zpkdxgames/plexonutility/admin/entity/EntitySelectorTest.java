package com.zpkdxgames.plexonutility.admin.entity;

import org.bukkit.entity.Ambient;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Monster;
import org.bukkit.entity.WaterMob;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EntitySelectorTest {
    @Test void normalizesExplicitLivingEntityNames() {
        EntitySelector.Selection selection = EntitySelector.parse("zombie-villager");
        assertEquals(EntityType.ZOMBIE_VILLAGER, selection.type());
        assertEquals("zombie_villager", selection.canonical());
    }

    @Test void hostileAndMonsterAliasesShareCategory() {
        assertEquals(EntitySelector.Category.HOSTILE, EntitySelector.parse("hostile").category());
        assertEquals(EntitySelector.Category.HOSTILE, EntitySelector.parse("monsters").category());
    }

    @Test void passiveAndAnimalAliasesShareCategory() {
        assertEquals(EntitySelector.Category.PASSIVE, EntitySelector.parse("passive").category());
        assertEquals(EntitySelector.Category.PASSIVE, EntitySelector.parse("animals").category());
    }

    @Test void rejectsPlayersAsExplicitTargets() {
        assertThrows(IllegalArgumentException.class, () -> EntitySelector.parse("player"));
    }

    @Test void categoryMatchingUsesBukkitEntityFamilies() {
        assertTrue(EntitySelector.matches(EntitySelector.parse("hostile"), mock(Monster.class)));
        assertTrue(EntitySelector.matches(EntitySelector.parse("animals"), mock(Animals.class)));
        assertTrue(EntitySelector.matches(EntitySelector.parse("ambient"), mock(Ambient.class)));
        assertTrue(EntitySelector.matches(EntitySelector.parse("water"), mock(WaterMob.class)));
    }

    @Test void explicitTypeOnlyMatchesThatType() {
        Entity zombie = mock(Entity.class);
        when(zombie.getType()).thenReturn(EntityType.ZOMBIE);
        Entity creeper = mock(Entity.class);
        when(creeper.getType()).thenReturn(EntityType.CREEPER);
        EntitySelector.Selection selection = EntitySelector.parse("zombie");
        assertTrue(EntitySelector.matches(selection, zombie));
        assertFalse(EntitySelector.matches(selection, creeper));
    }

    @Test void bossesRequireBossCategoryOrExplicitBossType() {
        assertTrue(EntitySelector.parse("bosses").explicitlyAllowsBoss());
        assertTrue(EntitySelector.parse("wither").explicitlyAllowsBoss());
        assertFalse(EntitySelector.parse("all").explicitlyAllowsBoss());
    }

    @Test void livingCompletionNeverContainsPlayer() {
        assertFalse(EntitySelector.livingNames().contains("player"));
        assertTrue(EntitySelector.livingNames().contains("zombie"));
    }
}
