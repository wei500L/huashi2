package com.huashi.eftransfer.app.modules.assessment.imports.vo;

public record QuestionBankImportReviewVO(
        Long importId,
        boolean publishable,
        long openReviewCount,
        long rejectedCount,
        String status
) {
}
