package com.huashi.eftransfer.app.modules.assessment.vo;

import java.time.LocalDateTime;
import java.util.List;

public record PublicAssessmentResultVO(
        Long attemptId,
        String releaseCode,
        String paperTitle,
        String status,
        Integer questionCount,
        Integer answeredCount,
        Integer correctCount,
        Integer objectiveScore,
        Integer totalScore,
        LocalDateTime submittedAt,
        boolean scoreVisible,
        AssessmentMetricSnapshotVO metricSnapshot,
        List<String> qualityFlags,
        String aiAnalysisStatus,
        AssessmentAiAnalysisVO aiAnalysis,
        List<AssessmentAttemptResultQuestionVO> questions
) {
}
