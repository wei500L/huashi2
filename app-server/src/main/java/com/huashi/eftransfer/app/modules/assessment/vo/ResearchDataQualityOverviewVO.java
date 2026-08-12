package com.huashi.eftransfer.app.modules.assessment.vo;

import java.util.List;

public record ResearchDataQualityOverviewVO(
        long valid,
        long flagged,
        List<ResearchFlagCountVO> flagDistribution
) {
}
