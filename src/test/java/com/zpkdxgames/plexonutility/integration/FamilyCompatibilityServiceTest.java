package com.zpkdxgames.plexonutility.integration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FamilyCompatibilityServiceTest {
    @Test
    void exposesKnownPlexonFamilyModulesWithoutPolling() {
        var known = FamilyCompatibilityService.knownFamilyPlugins();

        assertEquals("PlexonHomes", known.get("PLEXON_HOMES"));
        assertEquals("PlexonRanks", known.get("PLEXON_RANKS"));
        assertEquals("PlexonTools", known.get("PLEXON_TOOLS"));
        assertTrue(FamilyCompatibilityService.isFamilyPlugin("PlexonHomes"));
        assertTrue(FamilyCompatibilityService.isFamilyPlugin("PlexonRanks"));
        assertFalse(FamilyCompatibilityService.isFamilyPlugin("Essentials"));
    }
}
