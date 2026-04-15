package com.huashi.eftransfer.app.modules.notification.controller;

import com.huashi.eftransfer.app.modules.notification.dto.NotificationPageQuery;
import com.huashi.eftransfer.app.modules.notification.service.NotificationService;
import com.huashi.eftransfer.app.modules.notification.vo.NotificationItemVO;
import com.huashi.eftransfer.app.modules.notification.vo.NotificationUnreadCountVO;
import com.huashi.eftransfer.shared.api.ApiResponse;
import com.huashi.eftransfer.shared.page.PageResult;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ApiResponse<PageResult<NotificationItemVO>> page(@Valid @ModelAttribute NotificationPageQuery query) {
        return ApiResponse.success(notificationService.pageMine(query), MDC.get("traceId"));
    }

    @GetMapping("/unread-count")
    public ApiResponse<NotificationUnreadCountVO> unreadCount() {
        return ApiResponse.success(notificationService.getUnreadCount(), MDC.get("traceId"));
    }

    @PostMapping("/{notificationId}/read")
    public ApiResponse<NotificationItemVO> markRead(@PathVariable Long notificationId) {
        return ApiResponse.success(notificationService.markRead(notificationId), MDC.get("traceId"));
    }

    @PostMapping("/read-all")
    public ApiResponse<NotificationUnreadCountVO> markAllRead() {
        return ApiResponse.success(notificationService.markAllRead(), MDC.get("traceId"));
    }
}
