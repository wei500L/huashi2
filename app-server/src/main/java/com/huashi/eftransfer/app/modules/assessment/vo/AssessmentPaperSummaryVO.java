package com.huashi.eftransfer.app.modules.assessment.vo;

import java.time.LocalDateTime;

public record AssessmentPaperSummaryVO(
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
        LocalDateTime updatedAt
) {
}
