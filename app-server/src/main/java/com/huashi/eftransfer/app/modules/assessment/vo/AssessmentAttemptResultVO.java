package com.huashi.eftransfer.app.modules.assessment.vo;

import java.time.LocalDateTime;
import java.util.List;

public record AssessmentAttemptResultVO(
        Long attemptId,
        Long publishId,
        Long paperId,
        String paperTitle,
        String paperDescription,
        String className,
        String status,
        String instructionsText,
        Integer questionCount,
        Integer answeredCount,
        Integer correctCount,
        Integer objectiveScore,
        Integer totalScore,
        LocalDateTime startedAt,
        LocalDateTime expiresAt,
        LocalDateTime submittedAt,
        List<AssessmentAttemptResultQuestionVO> questions
) {
}
