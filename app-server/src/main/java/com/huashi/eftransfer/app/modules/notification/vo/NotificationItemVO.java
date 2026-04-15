package com.huashi.eftransfer.app.modules.notification.vo;

import java.time.LocalDateTime;

public record NotificationItemVO(
        Long id,
        String category,
        String level,
        String title,
        String content,
        String actionUrl,
        String actionLabel,
        String status,
        String payloadJson,
        LocalDateTime createdAt,
        LocalDateTime readAt
) {
}
