package com.huashi.eftransfer.app.modules.diagnosis.vo;

import com.huashi.eftransfer.app.modules.diagnosis.support.DiagnosisOptionPayload;
import com.huashi.eftransfer.app.modules.diagnosis.support.DiagnosisStimulusPayload;

import java.util.List;

public record DiagnosisItemResultDetailVO(
        Long itemResultId,
        Long templateItemId,
        Integer presentationOrder,
        String taskType,
        Long lexicalPairId,
        String englishWord,
        String frenchWord,
        String chineseGloss,
        String lexicalPairType,
        String contextSupportLevel,
        Boolean expectedSemanticMatch,
        DiagnosisStimulusPayload stimulus,
        List<DiagnosisOptionPayload> options,
        String correctAnswerKey,
        String selectedAnswerKey,
        Integer reactionTimeMs,
        Integer hesitationTimeMs,
        Boolean correct,
        Boolean semanticConsistent,
        String detectedErrorType,
        Double transferRiskScore,
        Double itemScore
) {
}
