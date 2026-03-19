package com.huashi.eftransfer.app.modules.diagnosis.support;

public record DiagnosisContextPerformance(
        String level,
        double accuracy,
        long avgReactionTime,
        long totalCount
) {
}
