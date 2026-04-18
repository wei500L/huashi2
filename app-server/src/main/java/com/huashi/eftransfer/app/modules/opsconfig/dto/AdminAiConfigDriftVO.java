package com.huashi.eftransfer.app.modules.opsconfig.dto;

import com.huashi.eftransfer.shared.ai.config.AiOpsConfigNotice;

import java.time.OffsetDateTime;
import java.util.List;

public record AdminAiConfigDriftVO(
        AdminAiRuntimeStateVO runtime,
        AdminAiStoredStateVO stored,
        boolean driftDetected,
        String syncJobStatus,
        Integer attemptCount,
        OffsetDateTime nextAttemptAt,
        List<AiOpsConfigNotice> notices
) {
}
