package com.huashi.eftransfer.app.modules.notification.service;

public record NotificationCreateCommand(
        Long recipientUserId,
        String category,
        String level,
        String title,
        String content,
        String actionUrl,
        String actionLabel,
        String payloadJson
) {
}
