package com.huashi.eftransfer.app.modules.assessment.vo;

public record ResearchReactionTimeStatisticVO(
        Long questionId,
        Integer questionOrder,
        String questionCode,
        long sampleCount,
        Long medianMs,
        Long q1Ms,
        Long q3Ms,
        Long p90Ms
) {
}
