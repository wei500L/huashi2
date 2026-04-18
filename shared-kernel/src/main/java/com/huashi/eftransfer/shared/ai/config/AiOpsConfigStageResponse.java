package com.huashi.eftransfer.shared.ai.config;

import java.time.OffsetDateTime;
import java.util.List;

public record AiOpsConfigStageResponse(
        String stageId,
        String source,
        Long version,
        OffsetDateTime stagedAt,
        List<AiOpsConfigNotice> notices
) {
}
