package com.huashi.eftransfer.app.modules.health.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record AppHealthPayload(
        String service,
        String status,
        List<String> activeProfiles,
        String aiGatewayBaseUrl,
        OffsetDateTime timestamp
) {
}
