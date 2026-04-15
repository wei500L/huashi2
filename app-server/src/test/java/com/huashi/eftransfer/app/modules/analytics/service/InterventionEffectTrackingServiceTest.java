package com.huashi.eftransfer.app.modules.analytics.service;

import com.huashi.eftransfer.app.modules.analytics.vo.TeacherInterventionEffectDiffVO;
import com.huashi.eftransfer.app.modules.analytics.vo.TeacherInterventionEffectSnapshotVO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InterventionEffectTrackingServiceTest {

    @Test
    void snapshotScopeFitsLearningProfileScopeColumn() {
        String scope = InterventionEffectTrackingService.buildInterventionSnapshotScope(Long.MAX_VALUE, 'B');

        assertTrue(scope.length() <= 16);
        assertTrue(scope.endsWith("B"));
    }

    @Test
    void metricDiffUsesCompletionMinusBaseline() {
        TeacherInterventionEffectDiffVO diff = InterventionEffectTrackingService.buildMetricDiff(
                new TeacherInterventionEffectSnapshotVO(1L, null, "HIGH", "MODE_A", 8, 5, 0.42d, 0.71d, 1300L),
                new TeacherInterventionEffectSnapshotVO(2L, null, "MEDIUM", "MODE_B", 3, 2, 0.67d, 0.33d, 980L)
        );

        assertNotNull(diff);
        assertEquals(0.25d, diff.recentAccuracyDelta(), 1e-9);
        assertEquals(-0.38d, diff.recentNegativeTransferRiskDelta(), 1e-9);
        assertEquals(-320L, diff.recentAvgReactionTimeMsDelta());
        assertEquals(-5, diff.pendingReviewCountDelta());
        assertEquals(-3, diff.highRiskPairCountDelta());
    }

    @Test
    void metricDiffRequiresBothSnapshots() {
        assertNull(InterventionEffectTrackingService.buildMetricDiff(
                new TeacherInterventionEffectSnapshotVO(1L, null, "HIGH", "MODE_A", 8, 5, 0.42d, 0.71d, 1300L),
                null
        ));
    }
}
