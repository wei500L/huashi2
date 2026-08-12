package com.huashi.eftransfer.app.modules.assessment.vo;

import java.time.LocalDateTime;
import java.util.List;

public record TeacherResearchAttemptDetailVO(
        ResearchAttemptParticipantVO participant,
        ResearchAttemptStateVO attempt,
        ResearchAttemptResultSummaryVO result,
        ResearchAttemptAiVO ai,
        List<ResearchAttemptQuestionVO> questions
) {
    public record ResearchAttemptParticipantVO(
            String participantCode,
            String participantType,
            LocalDateTime consentedAt
    ) {
    }

    public record ResearchAttemptStateVO(
            Long attemptId,
            Long publishId,
            Long paperId,
            String paperTitle,
            String status,
            Integer answeredCount,
            Integer questionCount,
            LocalDateTime startedAt,
            LocalDateTime lastSavedAt,
            LocalDateTime submittedAt,
            String submitReason
    ) {
    }

    public record ResearchAttemptResultSummaryVO(
            Integer objectiveScore,
            Integer totalScore,
            Double percentageScore,
            AssessmentMetricSnapshotVO metricSnapshot,
            List<String> qualityFlags
    ) {
    }

    public record ResearchAttemptAiVO(
            String status,
            AssessmentAiAnalysisVO analysis,
            String modelName,
            LocalDateTime completedAt,
            String fallbackReason
    ) {
    }

    public record ResearchAttemptQuestionVO(
            Long questionId,
            Integer questionOrder,
            String questionType,
            String questionCode,
            String sectionTitle,
            Boolean formalSection,
            String stemText,
            String promptText,
            List<AssessmentOptionVO> options,
            List<String> responses,
            List<String> correctAnswers,
            String justification,
            Boolean correct,
            Integer scoreAwarded,
            Integer questionScore,
            String explanationText,
            Long effectiveDurationMs,
            Integer responseChangeCount,
            List<ResearchAttachmentVO> attachments
    ) {
    }
}
