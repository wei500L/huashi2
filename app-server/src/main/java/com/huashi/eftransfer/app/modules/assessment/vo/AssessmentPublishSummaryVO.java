package com.huashi.eftransfer.app.modules.assessment.vo;

import java.time.LocalDateTime;
import java.util.List;

public record AssessmentPublishSummaryVO(
        Long publishId,
        Long teachingClassId,
        String className,
        String status,
        Integer durationMinutes,
        Integer questionCount,
        Integer totalScore,
        String instructionsText,
        LocalDateTime startsAt,
        LocalDateTime dueAt,
        String resultReleasePolicy,
        LocalDateTime publishedAt,
        Integer assignedCount,
        Integer attemptCount,
        Integer submittedCount,
        Integer pendingCount,
        String deliveryMode,
        String releaseCode,
        List<String> participationCodes
) {
    public AssessmentPublishSummaryVO(
            Long publishId, Long teachingClassId, String className, String status, Integer durationMinutes,
            Integer questionCount, Integer totalScore, String instructionsText, LocalDateTime startsAt,
            LocalDateTime dueAt, String resultReleasePolicy, LocalDateTime publishedAt, Integer assignedCount,
            Integer attemptCount, Integer submittedCount, Integer pendingCount
    ) {
        this(publishId, teachingClassId, className, status, durationMinutes, questionCount, totalScore,
                instructionsText, startsAt, dueAt, resultReleasePolicy, publishedAt, assignedCount,
                attemptCount, submittedCount, pendingCount, "CLASS", null, List.of());
    }

    public AssessmentPublishSummaryVO withPublicDelivery(String mode, String code, List<String> codes) {
        return new AssessmentPublishSummaryVO(publishId, teachingClassId, className, status, durationMinutes,
                questionCount, totalScore, instructionsText, startsAt, dueAt, resultReleasePolicy, publishedAt,
                assignedCount, attemptCount, submittedCount, pendingCount, mode, code,
                codes == null ? List.of() : List.copyOf(codes));
    }
}
