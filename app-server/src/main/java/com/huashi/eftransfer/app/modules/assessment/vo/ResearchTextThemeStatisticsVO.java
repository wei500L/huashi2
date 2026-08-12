package com.huashi.eftransfer.app.modules.assessment.vo;

import java.util.List;

public record ResearchTextThemeStatisticsVO(
        ResearchStatisticsMetaVO meta,
        List<ResearchTextThemeStatisticVO> questions
) {
}
