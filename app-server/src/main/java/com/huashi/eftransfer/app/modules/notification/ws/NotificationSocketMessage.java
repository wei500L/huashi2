package com.huashi.eftransfer.app.modules.notification.ws;

import com.huashi.eftransfer.app.modules.notification.vo.NotificationItemVO;

import java.time.LocalDateTime;

public record NotificationSocketMessage(
        String type,
        NotificationItemVO notification,
        long unreadCount,
        LocalDateTime sentAt
) {
}
