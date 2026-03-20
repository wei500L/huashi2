package com.huashi.eftransfer.app.modules.analytics.vo;

import java.time.LocalDateTime;

public record TeacherInterventionSummaryVO(
        Long id,
        Long studentUserId,
        String studentName,
        Long classId,
        String className,
        String priority,
        String status,
        LocalDateTime plannedAt,
        LocalDateTime completedAt,
        String patternDetected,
        String suggestedAction
) {
}
