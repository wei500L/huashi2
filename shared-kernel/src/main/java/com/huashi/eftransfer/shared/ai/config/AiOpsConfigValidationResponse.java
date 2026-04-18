package com.huashi.eftransfer.shared.ai.config;

import java.util.List;

public record AiOpsConfigValidationResponse(
        boolean valid,
        List<AiOpsConfigIssue> issues,
        List<AiOpsConfigNotice> notices
) {
}
