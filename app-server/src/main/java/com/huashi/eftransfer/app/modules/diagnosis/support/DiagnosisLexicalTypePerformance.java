package com.huashi.eftransfer.app.modules.diagnosis.support;

public record DiagnosisLexicalTypePerformance(
        String lexicalPairType,
        double accuracy,
        long avgReactionTime,
        long totalCount
) {
}
