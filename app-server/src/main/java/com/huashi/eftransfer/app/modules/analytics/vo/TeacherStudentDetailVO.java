package com.huashi.eftransfer.app.modules.analytics.vo;

public record TeacherStudentDetailVO(
        Long studentUserId,
        String studentName,
        int classRank,
        double classPercentile,
        StudentAnalyticsDetailVO analysis
) {
}
