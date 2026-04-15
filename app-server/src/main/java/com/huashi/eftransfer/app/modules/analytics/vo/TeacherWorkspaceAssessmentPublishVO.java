package com.huashi.eftransfer.app.modules.analytics.vo;

import java.time.LocalDateTime;

public record TeacherWorkspaceAssessmentPublishVO(
        long publishId,
        long paperId,
        String title,
        long classId,
        String className,
        LocalDateTime publishedAt,
        LocalDateTime dueAt,
        int assignedCount,
        int submittedCount,
        int pendingCount
) {
}
