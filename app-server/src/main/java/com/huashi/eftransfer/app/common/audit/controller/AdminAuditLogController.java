package com.huashi.eftransfer.app.common.audit.controller;

import com.huashi.eftransfer.app.common.audit.dto.AdminAuditLogPageQuery;
import com.huashi.eftransfer.app.common.audit.service.AdminAuditLogQueryService;
import com.huashi.eftransfer.app.common.audit.vo.AdminAuditLogItemVO;
import com.huashi.eftransfer.shared.api.ApiResponse;
import com.huashi.eftransfer.shared.page.PageResult;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@PreAuthorize("hasRole('ADMIN')")
@Validated
@RestController
@RequestMapping("/api/admin/audit-logs")
public class AdminAuditLogController {

    private final AdminAuditLogQueryService adminAuditLogQueryService;

    public AdminAuditLogController(AdminAuditLogQueryService adminAuditLogQueryService) {
        this.adminAuditLogQueryService = adminAuditLogQueryService;
    }

    @GetMapping
    public ApiResponse<PageResult<AdminAuditLogItemVO>> page(@Valid @ModelAttribute AdminAuditLogPageQuery query) {
        return ApiResponse.success(adminAuditLogQueryService.page(query), MDC.get("traceId"));
    }
}
