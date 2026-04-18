package com.huashi.eftransfer.shared.ai.config;

import java.time.OffsetDateTime;
import java.util.List;

public record AiOpsConfigEffectiveResponse(
        AiOpsConfigPayload config,
        String source,
        Long version,
        OffsetDateTime appliedAt,
        List<AiOpsConfigNotice> notices
) {
}
