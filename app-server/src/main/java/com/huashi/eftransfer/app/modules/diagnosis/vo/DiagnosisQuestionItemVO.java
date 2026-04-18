package com.huashi.eftransfer.app.modules.diagnosis.vo;

import com.huashi.eftransfer.app.modules.diagnosis.support.DiagnosisStimulusPayload;
import com.huashi.eftransfer.shared.enums.ContextSupportLevel;
import com.huashi.eftransfer.shared.enums.DiagnosisTaskType;
import com.huashi.eftransfer.shared.enums.LexicalPairType;

import java.util.List;

public record DiagnosisQuestionItemVO(
        Long itemResultId,
        Long templateItemId,
        DiagnosisTaskType taskType,
        Integer presentationOrder,
        Long lexicalPairId,
        String englishWord,
        String frenchWord,
        String chineseGloss,
        LexicalPairType lexicalPairType,
        ContextSupportLevel contextSupportLevel,
        DiagnosisStimulusPayload stimulus,
        List<DiagnosisOptionViewVO> options
) {
}
