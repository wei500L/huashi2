package com.huashi.eftransfer.app.modules.diagnosis.support;

public record DiagnosisScoringProfilePayload(
        String formulaKey,
        Double pairWeight,
        Double riskAmplifier,
        Integer maxReactionTimeMs
) {
}
