package com.huashi.eftransfer.app.modules.analytics.vo;

import java.time.LocalDateTime;

public record TeacherWorkspaceClassActivityVO(
        Long classId,
        String classCode,
        String className,
        long studentCount,
        long highRiskStudentCount,
        LocalDateTime lastActiveAt
) {
}
