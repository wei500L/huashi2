package com.huashi.eftransfer.app.modules.analytics.vo;

public record ClassCompletionByModeVO(
        String mode,
        double completionRate,
        long completedStudentCount,
        long studentCount
) {
}
