package com.huashi.eftransfer.app.modules.assessment.vo;

import java.util.List;

public record ResearchDimensionStatisticsVO(
        ResearchStatisticsMetaVO meta,
        List<ResearchDimensionStatisticVO> dimensions
) {
}
