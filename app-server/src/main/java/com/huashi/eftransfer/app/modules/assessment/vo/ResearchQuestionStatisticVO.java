package com.huashi.eftransfer.app.modules.assessment.vo;

public record ResearchQuestionStatisticVO(
        Long questionId,
        Integer questionOrder,
        String questionCode,
        String sectionTitle,
        String questionType,
        long answeredCount,
        long skippedCount,
        Double correctRate,
        Long medianReactionMs,
        boolean qualityWarning
) {
}
