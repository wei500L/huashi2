package com.huashi.eftransfer.app.common.idempotency;

import java.time.OffsetDateTime;

public record IdempotencyRecord(
        Long id,
        String requestKey,
        String requestPath,
        String requestMethod,
        String requestHash,
        String responseCode,
        String responseBody,
        OffsetDateTime expiresAt
) {
}
