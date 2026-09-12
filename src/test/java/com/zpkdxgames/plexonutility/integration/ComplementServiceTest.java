package com.zpkdxgames.plexonutility.integration;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComplementServiceTest {
    @Test
    void detectsEnabledSpecialistProvidersWithoutClaimingMissingCategories() {
        Map<String, String> enabled = Map.of(
                "LuckPerms", "5.5.0",
                "CoreProtect", "23.0",
                "Chunky", "1.4.40");

        var statuses = ComplementService.snapshot(enabled::get);

        var permissions = statuses.stream().filter(status -> status.category() == ComplementService.Category.PERMISSIONS).findFirst().orElseThrow();
        var audit = statuses.stream().filter(status -> status.category() == ComplementService.Category.BLOCK_AUDIT).findFirst().orElseThrow();
        var performance = statuses.stream().filter(status -> status.category() == ComplementService.Category.PERFORMANCE).findFirst().orElseThrow();
        var antiCheat = statuses.stream().filter(status -> status.category() == ComplementService.Category.ANTI_CHEAT).findFirst().orElseThrow();

        assertTrue(permissions.detectedAny());
        assertEquals("LuckPerms 5.5.0", permissions.providerSummary());
        assertTrue(audit.detectedAny());
        assertTrue(performance.detectedAny());
        assertFalse(antiCheat.detectedAny());
    }

    @Test
    void returnsEveryComplementCategoryEvenWhenNothingIsInstalled() {
        var statuses = ComplementService.snapshot(ignored -> null);

        assertEquals(ComplementService.Category.values().length, statuses.size());
        assertTrue(statuses.stream().noneMatch(ComplementService.Status::detectedAny));
    }
}
