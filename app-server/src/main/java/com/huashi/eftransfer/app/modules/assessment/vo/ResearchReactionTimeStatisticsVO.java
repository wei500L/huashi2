package com.huashi.eftransfer.app.modules.assessment.vo;

import java.util.List;

public record ResearchReactionTimeStatisticsVO(
        ResearchStatisticsMetaVO meta,
        List<ResearchReactionTimeStatisticVO> questions
) {
}
