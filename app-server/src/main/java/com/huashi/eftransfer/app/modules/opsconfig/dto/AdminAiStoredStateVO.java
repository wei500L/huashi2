package com.huashi.eftransfer.app.modules.opsconfig.dto;

import java.time.OffsetDateTime;

public record AdminAiStoredStateVO(
        boolean present,
        Long version,
        OffsetDateTime updatedAt
) {
}
