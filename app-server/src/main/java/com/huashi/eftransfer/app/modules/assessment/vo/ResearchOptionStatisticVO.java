package com.huashi.eftransfer.app.modules.assessment.vo;

import java.util.List;

public record ResearchOptionStatisticVO(
        Long questionId,
        Integer questionOrder,
        String questionCode,
        String questionType,
        Double exactCorrectRate,
        List<ResearchOptionShareVO> options
) {
    public record ResearchOptionShareVO(
            String optionKey,
            String optionLabel,
            long count,
            Double answeredShare,
            Double submittedShare
    ) {
    }
}
