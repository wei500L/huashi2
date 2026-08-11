package com.huashi.eftransfer.app.modules.assessment.vo;

import java.time.LocalDateTime;
import java.util.List;

public record PublicAssessmentAttemptVO(
        Long attemptId,
        String releaseCode,
        String paperTitle,
        String paperDescription,
        String instructionsText,
        String status,
        Integer durationMinutes,
        Integer questionCount,
        Integer answeredCount,
        LocalDateTime startedAt,
        LocalDateTime expiresAt,
        LocalDateTime lastSavedAt,
        Long version,
        LocalDateTime serverTime,
        Long activeElapsedMs,
        List<PublicAssessmentQuestionVO> questions
) {
}
