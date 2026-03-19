package com.huashi.eftransfer.app.modules.diagnosis.vo;

import com.huashi.eftransfer.app.modules.diagnosis.support.DiagnosisStimulusPayload;

import java.util.List;

public record DiagnosisQuestionItemVO(
        Long itemResultId,
        Long templateItemId,
        String taskType,
        Integer presentationOrder,
        Long lexicalPairId,
        String englishWord,
        String frenchWord,
        String chineseGloss,
        String lexicalPairType,
        String contextSupportLevel,
        DiagnosisStimulusPayload stimulus,
        List<DiagnosisOptionViewVO> options
) {
}
