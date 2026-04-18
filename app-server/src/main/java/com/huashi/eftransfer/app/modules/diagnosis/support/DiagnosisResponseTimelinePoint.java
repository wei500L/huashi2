package com.huashi.eftransfer.app.modules.diagnosis.support;

import com.huashi.eftransfer.shared.enums.DiagnosisTaskType;
import com.huashi.eftransfer.shared.enums.LexicalPairType;

public record DiagnosisResponseTimelinePoint(
        int presentationOrder,
        Long itemResultId,
        DiagnosisTaskType taskType,
        LexicalPairType lexicalPairType,
        long reactionTime,
        boolean correct,
        String errorType
) {
}
