package com.huashi.eftransfer.shared.ai.config;

import java.time.OffsetDateTime;
import java.util.List;

public record AiOpsConfigApplyResponse(
        String source,
        Long version,
        OffsetDateTime appliedAt,
        List<String> notices
) {
}
