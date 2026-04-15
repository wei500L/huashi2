package com.huashi.eftransfer.app.modules.analytics.vo;

public record TeacherClassStudentCandidateVO(
        Long studentUserId,
        String studentName,
        String username,
        String studentNo,
        String gradeName,
        boolean assigned,
        long activeClassCount
) {
}
