package com.huashi.eftransfer.app.modules.analytics.vo;

public record AdminDashboardAiTrendPointVO(
        String date,
        long totalCalls,
        long fallbackCalls,
        double fallbackRate
) {
}
