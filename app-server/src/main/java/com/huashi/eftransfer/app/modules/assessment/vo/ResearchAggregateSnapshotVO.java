package com.huashi.eftransfer.app.modules.assessment.vo;

import java.time.LocalDateTime;

public record ResearchAggregateSnapshotVO(
        Long snapshotId,
        Long publishId,
        Long paperId,
        String snapshotVersion,
        String snapshotKey,
        long sampleCount,
        long submittedCount,
        LocalDateTime sourceMaxUpdatedAt,
        LocalDateTime createdAt
) {
}
