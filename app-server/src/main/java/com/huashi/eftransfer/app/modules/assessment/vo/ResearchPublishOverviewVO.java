package com.huashi.eftransfer.app.modules.assessment.vo;

import java.time.LocalDateTime;

public record ResearchPublishOverviewVO(
        Long publishId,
        Long paperId,
        String paperTitle,
        String releaseCode,
        ResearchFunnelVO funnel,
        ResearchRatesVO rates,
        ResearchDistributionStatsVO timing,
        ResearchDistributionStatsVO score,
        ResearchDataQualityOverviewVO dataQuality,
        ResearchAiStatusOverviewVO ai,
        LocalDateTime latestSubmissionAt,
        LocalDateTime statisticsGeneratedAt
) {
}
