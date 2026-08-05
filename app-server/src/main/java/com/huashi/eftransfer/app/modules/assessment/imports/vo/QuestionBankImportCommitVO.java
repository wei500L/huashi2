package com.huashi.eftransfer.app.modules.assessment.imports.vo;

public record QuestionBankImportCommitVO(
        Long importId,
        String status,
        Long questionBankId,
        Long questionnaireId,
        Long questionnaireVersionId,
        Long paperId,
        int importedItemCount,
        int reviewRequiredCount
) {
}
