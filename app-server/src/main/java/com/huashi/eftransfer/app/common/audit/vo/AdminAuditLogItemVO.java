package com.huashi.eftransfer.app.common.audit.vo;

import java.time.LocalDateTime;

public record AdminAuditLogItemVO(
        Long id,
        Long actorUserId,
        String actorUsername,
        String actorDisplayName,
        String actionType,
        String targetType,
        String targetId,
        String requestPath,
        String requestMethod,
        String traceId,
        String requestPayload,
        String responseCode,
        LocalDateTime createdAt
) {
}
