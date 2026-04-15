package com.huashi.eftransfer.app.modules.analytics.vo;

import java.util.List;

public record AdminDashboardVO(
        AdminDashboardOverviewVO overview,
        List<AdminDashboardRegistrationTrendPointVO> registrationTrend,
        List<AdminDashboardCompletionTrendPointVO> completionTrend,
        List<AdminDashboardAiTrendPointVO> aiTrend,
        List<AdminDashboardSceneDistributionVO> aiSceneDistribution
) {
}
