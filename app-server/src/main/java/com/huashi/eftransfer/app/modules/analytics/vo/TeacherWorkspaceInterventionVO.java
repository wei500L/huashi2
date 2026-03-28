package com.huashi.eftransfer.app.modules.analytics.vo;

import java.time.LocalDateTime;

public record TeacherWorkspaceInterventionVO(
        Long id,
        String studentName,
        String priority,
        String status,
        LocalDateTime plannedAt
) {
}
