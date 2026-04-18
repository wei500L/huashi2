package com.huashi.eftransfer.app.modules.assessment.vo;

import com.huashi.eftransfer.shared.enums.AssessmentAttemptStatus;

import java.time.LocalDateTime;
import java.util.List;

public record TeacherAssessmentAttemptResultVO(
        Long attemptId,
        Long publishId,
        Long paperId,
        Long teachingClassId,
        Long studentUserId,
        String studentName,
        String paperTitle,
        String paperDescription,
        String className,
        AssessmentAttemptStatus status,
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
