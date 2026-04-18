package com.huashi.eftransfer.app.modules.assessment.vo;

import com.huashi.eftransfer.shared.enums.AssessmentAttemptStatus;

import java.time.LocalDateTime;
import java.util.List;

public record AssessmentAttemptDetailVO(
        Long attemptId,
        Long publishId,
        Long paperId,
        String paperTitle,
        String paperDescription,
        String className,
        AssessmentAttemptStatus status,
        String instructionsText,
        Integer durationMinutes,
        Integer questionCount,
        Integer answeredCount,
        Integer totalScore,
        LocalDateTime startedAt,
        LocalDateTime expiresAt,
        LocalDateTime submittedAt,
        LocalDateTime lastSavedAt,
        LocalDateTime serverTime,
        List<AssessmentAttemptQuestionVO> questions
) {
}
