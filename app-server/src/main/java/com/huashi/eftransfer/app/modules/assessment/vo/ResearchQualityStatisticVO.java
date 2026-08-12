package com.huashi.eftransfer.app.modules.assessment.vo;

import java.util.List;

public record ResearchQualityStatisticVO(
        ResearchStatisticsMetaVO meta,
        long validCount,
        long flaggedCount,
        List<ResearchFlagCountVO> flagDistribution
) {
}
