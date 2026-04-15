package com.huashi.eftransfer.app.modules.analytics.vo;

public record AdminDashboardSceneDistributionVO(
        String scene,
        long count,
        double ratio
) {
}
