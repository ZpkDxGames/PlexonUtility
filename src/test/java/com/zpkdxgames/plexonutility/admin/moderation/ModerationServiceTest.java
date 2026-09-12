package com.zpkdxgames.plexonutility.admin.moderation;

import com.zpkdxgames.plexonutility.admin.AdminAuditService;
import com.zpkdxgames.plexonutility.message.MessageService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class ModerationServiceTest {
    private final ModerationService service = new ModerationService(
            mock(MessageService.class), mock(AdminAuditService.class));

    @Test void defaultAndCustomReasonAreNormalized() {
        assertEquals("Removed by staff.", service.normalizeReason("", "Removed by staff."));
        assertEquals("Griefing near spawn", service.normalizeReason("  Griefing near spawn  ", "fallback"));
    }

    @Test void multilineAndOverlongReasonsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.normalizeReason("line1\nline2", "fallback"));
        assertThrows(IllegalArgumentException.class, () -> service.normalizeReason("x".repeat(161), "fallback"));
    }
}
