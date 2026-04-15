package com.huashi.eftransfer.app.modules.analytics.controller;

import com.huashi.eftransfer.app.modules.analytics.service.AdminDashboardService;
import com.huashi.eftransfer.app.modules.analytics.vo.AdminDashboardVO;
import com.huashi.eftransfer.shared.api.ApiResponse;
import org.slf4j.MDC;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    public AdminDashboardController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping
    public ApiResponse<AdminDashboardVO> getDashboard() {
        return ApiResponse.success(adminDashboardService.getDashboard(), MDC.get("traceId"));
    }
}
