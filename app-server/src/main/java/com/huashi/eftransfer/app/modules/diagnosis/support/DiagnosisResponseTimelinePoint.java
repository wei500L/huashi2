package com.huashi.eftransfer.app.modules.diagnosis.support;

public record DiagnosisResponseTimelinePoint(
        int presentationOrder,
        Long itemResultId,
        String taskType,
        String lexicalPairType,
        long reactionTime,
        boolean correct,
        String errorType
) {
}
