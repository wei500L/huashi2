package com.huashi.eftransfer.app.modules.analytics.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeacherInterventionServiceViewTest {

    @Test
    void normalizeViewFallsBackToAllForUnknownValues() {
        assertEquals("ALL", TeacherInterventionService.normalizeView(null));
        assertEquals("ALL", TeacherInterventionService.normalizeView(""));
        assertEquals("ALL", TeacherInterventionService.normalizeView("unknown"));
        assertEquals("PENDING", TeacherInterventionService.normalizeView("pending"));
        assertEquals("OVERDUE", TeacherInterventionService.normalizeView("overdue"));
    }

    @Test
    void explicitStatusWinsOverSemanticView() {
        assertEquals("IN_PROGRESS", TeacherInterventionService.normalizeStatusFilter("in_progress"));
        assertFalse(TeacherInterventionService.shouldApplyPendingView("IN_PROGRESS", "pending"));
        assertFalse(TeacherInterventionService.shouldApplyCompletedView("IN_PROGRESS", "completed"));
        assertFalse(TeacherInterventionService.shouldApplyOverdueView("IN_PROGRESS", "overdue"));
    }

    @Test
    void semanticViewsApplyOnlyWithoutExplicitStatus() {
        assertNull(TeacherInterventionService.normalizeStatusFilter(null));
        assertTrue(TeacherInterventionService.shouldApplyPendingView(null, "pending"));
        assertTrue(TeacherInterventionService.shouldApplyCompletedView(null, "completed"));
        assertTrue(TeacherInterventionService.shouldApplyOverdueView(null, "overdue"));
    }
}
