package com.huashi.eftransfer.app.modules.analytics.vo;

import java.time.LocalDateTime;

public record AdminDashboardOverviewVO(
        long totalUsers,
        long enabledUsers,
        long registrationsLast30Days,
        long dailyActiveUsers,
        long weeklyActiveUsers,
        long diagnosisCompletedLast30Days,
        long trainingCompletedLast30Days,
        long assessmentCompletedLast30Days,
        long aiCallsLast30Days,
        long aiFallbackCountLast30Days,
        double aiFallbackRateLast30Days,
        LocalDateTime generatedAt
) {
}
