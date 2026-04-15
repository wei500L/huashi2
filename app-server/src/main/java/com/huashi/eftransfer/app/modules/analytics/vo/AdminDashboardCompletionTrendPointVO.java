package com.huashi.eftransfer.app.modules.analytics.vo;

public record AdminDashboardCompletionTrendPointVO(
        String date,
        long diagnosisCompleted,
        long trainingCompleted,
        long assessmentCompleted
) {
}
