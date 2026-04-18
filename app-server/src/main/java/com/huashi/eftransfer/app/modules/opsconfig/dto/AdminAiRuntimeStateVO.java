package com.huashi.eftransfer.app.modules.opsconfig.dto;

import java.time.OffsetDateTime;

public record AdminAiRuntimeStateVO(
        boolean available,
        String source,
        String version,
        OffsetDateTime appliedAt,
        boolean inSync
) {
}
