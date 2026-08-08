package com.huashi.eftransfer.app.modules.assessment.vo;

import java.util.List;
import java.math.BigDecimal;

public record AssessmentPaperQuestionVO(
        Long questionId,
        String questionType,
        Integer sortOrder,
        String stemText,
        String promptText,
        List<AssessmentOptionVO> options,
        List<String> correctAnswers,
        String explanationText,
        Integer score,
        Long questionVersionId,
        String sectionCode,
        Boolean requiredAnswer,
        BigDecimal weight,
        String transferCategory,
        String contextLevel,
        String constructCode,
        String targetWord,
        String optionExplanationsJson,
        String displayConditionJson
) {
}
