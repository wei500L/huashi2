package com.huashi.eftransfer.app.modules.assessment.vo;

import java.time.LocalDateTime;
import java.util.List;

public record AssessmentPaperDetailVO(
        Long paperId,
        String paperCode,
        String title,
        String description,
        String status,
        String paperPurpose,
        Integer durationMinutes,
        Integer questionCount,
        Integer totalScore,
        LocalDateTime latestPublishAt,
        List<AssessmentPaperQuestionVO> questions,
        List<AssessmentPublishSummaryVO> publishes
) {
}
