package com.huashi.eftransfer.ai.modules.rag.support;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record IngestionJobRecord(
        Long id,
        String jobType,
        String mode,
        String status,
        List<String> sourceTypes,
        List<String> sourceIds,
        String lastCursor,
        OffsetDateTime lastSourceUpdatedAt,
        OffsetDateTime finishedAt,
        Map<String, Object> stats
) {
}
