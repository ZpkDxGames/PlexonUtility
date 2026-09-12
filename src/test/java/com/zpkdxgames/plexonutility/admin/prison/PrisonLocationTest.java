package com.zpkdxgames.plexonutility.admin.prison;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PrisonLocationTest {
    @Test void preservesCoordinatesAndRotation() {
        PrisonLocation location = new PrisonLocation("Survival_World", 1.5, 64.25, -8.75, 91.0F, -12.5F);
        assertEquals("Survival_World", location.world());
        assertEquals(1.5, location.x());
        assertEquals(64.25, location.y());
        assertEquals(-8.75, location.z());
        assertEquals(91.0F, location.yaw());
        assertEquals(-12.5F, location.pitch());
        assertEquals("2, 64, -9", location.coordinates());
    }

    @Test void rejectsInvalidWorldAndNonFiniteCoordinates() {
        assertThrows(IllegalArgumentException.class, () -> new PrisonLocation(" ", 0, 64, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new PrisonLocation("world", Double.NaN, 64, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new PrisonLocation("world", 0, 64, 0, Float.NaN, 0));
    }
}
