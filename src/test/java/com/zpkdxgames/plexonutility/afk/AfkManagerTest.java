package com.zpkdxgames.plexonutility.afk;

import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class AfkManagerTest {
    @Test
    void movementWithinSameBlockDoesNotCountAsMeaningfulActivity() {
        World world = mock(World.class);
        Location from = new Location(world, 10.1D, 64.0D, 20.1D, 0.0F, 0.0F);
        Location rotated = new Location(world, 10.2D, 64.0D, 20.2D, 180.0F, 80.0F);

        assertFalse(AfkManager.meaningfulMovement(from, rotated));
    }

    @Test
    void blockCoordinateChangeCountsAsMeaningfulActivity() {
        World world = mock(World.class);
        Location from = new Location(world, 10.9D, 64.0D, 20.0D);
        Location to = new Location(world, 11.0D, 64.0D, 20.0D);

        assertTrue(AfkManager.meaningfulMovement(from, to));
    }

    @Test
    void afkCommandIsExcludedFromCommandActivityReset() {
        assertTrue(AfkManager.isAfkCommand("/afk"));
        assertTrue(AfkManager.isAfkCommand("/PlexonUtility:AFK"));
        assertFalse(AfkManager.isAfkCommand("/home"));
        assertFalse(AfkManager.isAfkCommand("/afkstatus"));
    }
}
