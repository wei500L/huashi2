package com.huashi.eftransfer.app.modules.assessment.imports.vo;

public record QuestionBankImportIssueVO(
        String sheet,
        Integer rowNumber,
        String field,
        String severity,
        String code,
        String itemCode,
        String message,
        Long issueId
) {
}
