package com.huashi.eftransfer.app.modules.user.controller;

import com.huashi.eftransfer.app.modules.user.dto.AdminUserAccessUpdateRequest;
import com.huashi.eftransfer.app.modules.user.dto.AdminUserCreateRequest;
import com.huashi.eftransfer.app.modules.user.service.UserAdminService;
import com.huashi.eftransfer.app.modules.user.service.UserQueryService;
import com.huashi.eftransfer.app.modules.user.vo.UserSummaryVO;
import com.huashi.eftransfer.shared.api.ApiResponse;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@PreAuthorize("hasRole('ADMIN')")
@Validated
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserQueryService userQueryService;
    private final UserAdminService userAdminService;

    public AdminUserController(UserQueryService userQueryService, UserAdminService userAdminService) {
        this.userQueryService = userQueryService;
        this.userAdminService = userAdminService;
    }

    @GetMapping
    public ApiResponse<List<UserSummaryVO>> listUsers() {
        return ApiResponse.success(userQueryService.listAllUsers(), MDC.get("traceId"));
    }

    @PostMapping
    public ApiResponse<UserSummaryVO> createUser(@Valid @RequestBody AdminUserCreateRequest request) {
        return ApiResponse.success(userAdminService.createUser(request), MDC.get("traceId"));
    }

    @PutMapping("/{userId}/access")
    public ApiResponse<UserSummaryVO> updateUserAccess(
            @PathVariable Long userId,
            @Valid @RequestBody AdminUserAccessUpdateRequest request
    ) {
        return ApiResponse.success(userAdminService.updateUserAccess(userId, request), MDC.get("traceId"));
    }
}
