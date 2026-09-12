package com.zpkdxgames.plexonutility.admin.moderation;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DurationParserTest {
    @Test void parsesPermanentAliases() {
        assertTrue(DurationParser.parse("perm", 3650).permanent());
        assertTrue(DurationParser.parse("PERMANENT", 3650).permanent());
    }

    @Test void parsesMinutesHoursAndDays() {
        assertEquals(Duration.ofMinutes(30), DurationParser.parse("30m", 3650).duration());
        assertEquals(Duration.ofHours(2), DurationParser.parse("2h", 3650).duration());
        assertEquals(Duration.ofDays(7), DurationParser.parse("7d", 3650).duration());
    }

    @Test void rejectsInvalidUnitZeroNegativeAndOverflow() {
        assertThrows(IllegalArgumentException.class, () -> DurationParser.parse("1w", 3650));
        assertThrows(IllegalArgumentException.class, () -> DurationParser.parse("0m", 3650));
        assertThrows(IllegalArgumentException.class, () -> DurationParser.parse("-1d", 3650));
        assertThrows(IllegalArgumentException.class, () -> DurationParser.parse("999999999999999999999999d", 3650));
    }

    @Test void enforcesConfiguredMaximum() {
        assertEquals(Duration.ofDays(30), DurationParser.parse("30d", 30).duration());
        assertThrows(IllegalArgumentException.class, () -> DurationParser.parse("31d", 30));
    }

    @Test void durationTokenDetectionKeepsPlainReasonAsPermanentBanReason() {
        assertTrue(DurationParser.looksLikeDurationToken("7d"));
        assertTrue(DurationParser.looksLikeDurationToken("perm"));
        assertFalse(DurationParser.looksLikeDurationToken("griefing"));
    }
}
