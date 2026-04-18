package com.huashi.eftransfer.app.modules.diagnosis.vo;

import com.huashi.eftransfer.app.modules.diagnosis.support.DiagnosisOptionPayload;
import com.huashi.eftransfer.app.modules.diagnosis.support.DiagnosisStimulusPayload;
import com.huashi.eftransfer.shared.enums.ContextSupportLevel;
import com.huashi.eftransfer.shared.enums.DiagnosisTaskType;
import com.huashi.eftransfer.shared.enums.LexicalPairType;

import java.util.List;

public record DiagnosisItemResultDetailVO(
        Long itemResultId,
        Long templateItemId,
        Integer presentationOrder,
        DiagnosisTaskType taskType,
        Long lexicalPairId,
        String englishWord,
        String frenchWord,
        String chineseGloss,
        LexicalPairType lexicalPairType,
        ContextSupportLevel contextSupportLevel,
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
