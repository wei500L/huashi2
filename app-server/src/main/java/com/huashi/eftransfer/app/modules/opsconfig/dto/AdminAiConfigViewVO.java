package com.huashi.eftransfer.app.modules.opsconfig.dto;

import com.huashi.eftransfer.shared.ai.config.AiOpsConfigNotice;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigPayload;

import java.time.OffsetDateTime;
import java.util.List;

public record AdminAiConfigViewVO(
        AiOpsConfigPayload config,
        AdminAiSecretFieldsVO secrets,
        String source,
        String version,
        OffsetDateTime updatedAt,
        List<AiOpsConfigNotice> notices,
        AdminAiRuntimeStateVO runtime,
        AdminAiStoredStateVO stored
) {
}
