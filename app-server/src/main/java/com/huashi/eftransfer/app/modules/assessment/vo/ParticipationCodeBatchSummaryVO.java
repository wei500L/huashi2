package com.huashi.eftransfer.app.modules.assessment.vo;

import java.time.LocalDateTime;

public record ParticipationCodeBatchSummaryVO(
        String batchId,
        LocalDateTime generatedAt,
        long totalCount,
        long unusedCount,
        long inProgressCount,
        long submittedCount,
        long revokedCount
) {
}
