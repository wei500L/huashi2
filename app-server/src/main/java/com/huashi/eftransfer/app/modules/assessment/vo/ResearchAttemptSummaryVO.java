package com.huashi.eftransfer.app.modules.assessment.vo;

import java.time.LocalDateTime;
import java.util.List;

public record ResearchAttemptSummaryVO(
        Long attemptId,
        String participantCode,
        String participantType,
        String status,
        Integer answeredCount,
        Integer questionCount,
        Double percentageScore,
        Long effectiveDurationMs,
        List<String> qualityFlags,
        Integer attachmentCount,
        String aiAnalysisStatus,
        LocalDateTime startedAt,
        LocalDateTime lastSavedAt,
        LocalDateTime submittedAt
) {
}
