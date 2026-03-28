package com.huashi.eftransfer.app.modules.diagnosis.vo;

import com.huashi.eftransfer.app.modules.diagnosis.support.DiagnosisOptionPayload;
import com.huashi.eftransfer.app.modules.diagnosis.support.DiagnosisScoringProfilePayload;
import com.huashi.eftransfer.app.modules.diagnosis.support.DiagnosisStimulusPayload;

import java.util.List;

public record DiagnosisTemplateDraftItemVO(
        String draftItemId,
        Long lexicalPairId,
        String englishWord,
        String frenchWord,
        String chineseGloss,
        String lexicalPairType,
        String taskType,
        String blockCode,
        Integer sortOrder,
        String contextSupportLevel,
        Boolean expectedSemanticMatch,
        DiagnosisStimulusPayload stimulus,
        List<DiagnosisOptionPayload> options,
        String correctAnswerKey,
        DiagnosisScoringProfilePayload scoringProfile
) {
}
