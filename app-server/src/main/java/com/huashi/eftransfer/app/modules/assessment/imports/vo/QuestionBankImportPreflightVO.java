package com.huashi.eftransfer.app.modules.assessment.imports.vo;

import java.util.List;

public record QuestionBankImportPreflightVO(
        Long importId,
        String status,
        String sourceFileName,
        int rowCount,
        long errorCount,
        long warningCount,
        long reviewRequiredCount,
        List<QuestionBankImportIssueVO> issues
) {
}
