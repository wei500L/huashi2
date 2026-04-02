package com.huashi.eftransfer.app.modules.analytics.vo;

import java.time.LocalDateTime;

public record TeacherWorkspaceAssessmentPublishVO(
        Long publishId,
        Long paperId,
        String title,
        Long classId,
        String className,
        LocalDateTime publishedAt,
        LocalDateTime dueAt,
        Integer assignedCount,
        Integer submittedCount,
        Integer pendingCount
) {
}
