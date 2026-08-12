package com.huashi.eftransfer.app.modules.assessment.vo;

import java.util.List;

public record ResearchOptionStatisticsVO(
        ResearchStatisticsMetaVO meta,
        List<ResearchOptionStatisticVO> questions
) {
}
