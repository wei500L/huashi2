package com.huashi.eftransfer.app.modules.diagnosis.support;

import com.huashi.eftransfer.shared.enums.LexicalPairType;

public record DiagnosisLexicalTypePerformance(
        LexicalPairType lexicalPairType,
        double accuracy,
        long avgReactionTime,
        long totalCount
) {
}
