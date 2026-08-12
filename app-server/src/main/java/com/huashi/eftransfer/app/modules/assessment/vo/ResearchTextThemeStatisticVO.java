package com.huashi.eftransfer.app.modules.assessment.vo;

public record ResearchTextThemeStatisticVO(
        Long questionId,
        Integer questionOrder,
        String questionCode,
        String questionType,
        long answeredCount,
        long emptyCount,
        String themeStatus
) {
}
