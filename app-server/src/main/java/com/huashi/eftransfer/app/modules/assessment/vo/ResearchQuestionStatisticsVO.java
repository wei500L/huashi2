package com.huashi.eftransfer.app.modules.assessment.vo;

import java.util.List;

public record ResearchQuestionStatisticsVO(
        ResearchStatisticsMetaVO meta,
        List<ResearchQuestionStatisticVO> questions
) {
}
