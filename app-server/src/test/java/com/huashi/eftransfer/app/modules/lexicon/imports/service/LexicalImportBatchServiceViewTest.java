package com.huashi.eftransfer.app.modules.lexicon.imports.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LexicalImportBatchServiceViewTest {

    @Test
    void normalizeViewFallsBackToAllForUnknownValues() {
        assertEquals("ALL", LexicalImportBatchService.normalizeView(null));
        assertEquals("ALL", LexicalImportBatchService.normalizeView(""));
        assertEquals("ALL", LexicalImportBatchService.normalizeView("unexpected"));
        assertEquals("PENDING", LexicalImportBatchService.normalizeView("pending"));
        assertEquals("FAILED", LexicalImportBatchService.normalizeView("failed"));
    }

    @Test
    void explicitStatusWinsOverSemanticView() {
        assertEquals("DRAFT", LexicalImportBatchService.normalizeStatusFilter("draft"));
        assertFalse(LexicalImportBatchService.shouldApplyPendingView("DRAFT", "failed"));
        assertFalse(LexicalImportBatchService.shouldApplyFailedView("DRAFT", "failed"));
    }

    @Test
    void semanticViewsApplyOnlyWithoutExplicitStatus() {
        assertNull(LexicalImportBatchService.normalizeStatusFilter(null));
        assertTrue(LexicalImportBatchService.shouldApplyPendingView(null, "pending"));
        assertTrue(LexicalImportBatchService.shouldApplyFailedView(null, "failed"));
    }
}
