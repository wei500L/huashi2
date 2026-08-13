package com.huashi.eftransfer.app.modules.assessment.vo;

import java.time.LocalDateTime;
import java.util.List;

public record PublicAssessmentReleaseSummaryVO(
        Long publishId,
        Long paperId,
        String paperCode,
        String paperTitle,
        String releaseCode,
        String status,
        LocalDateTime publishedAt,
        boolean qrEntryEnabled,
        int maxAttempts,
        int codeCount,
        long unusedCount,
        long inProgressCount,
        long submittedCount,
        long revokedCount,
        long qrParticipantCount,
        List<ParticipationCodeBatchSummaryVO> batches
) {
}
