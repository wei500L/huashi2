package com.huashi.eftransfer.app.modules.analytics.vo;

import java.time.LocalDateTime;

public record TeacherClassStudentVO(
        Long studentUserId,
        String studentName,
        String username,
        String studentNo,
        String gradeName,
        LocalDateTime joinedAt
) {
}
