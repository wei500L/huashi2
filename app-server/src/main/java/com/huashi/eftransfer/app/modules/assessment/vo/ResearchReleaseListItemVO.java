package com.huashi.eftransfer.app.modules.assessment.vo;

import java.time.LocalDateTime;

public record ResearchReleaseListItemVO(
        Long publishId,
        Long paperId,
        String paperTitle,
        String releaseCode,
        LocalDateTime publishedAt,
        String status,
        Integer startedCount,
        Integer submittedCount,
        LocalDateTime latestSubmissionAt,
        String aiReportStatus
) {
}
