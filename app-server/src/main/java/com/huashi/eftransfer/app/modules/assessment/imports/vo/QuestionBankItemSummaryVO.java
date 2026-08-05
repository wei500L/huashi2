package com.huashi.eftransfer.app.modules.assessment.imports.vo;

import java.time.LocalDateTime;
import java.util.List;

public record QuestionBankItemSummaryVO(
        Long itemId,
        String questionCode,
        Integer version,
        String questionType,
        String stemText,
        String transferCategory,
        String contextLevel,
        String constructCode,
        String targetWord,
        List<String> tags,
        String reviewStatus,
        LocalDateTime updatedAt
) {
}
