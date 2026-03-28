package com.huashi.eftransfer.app.modules.user.controller;

import com.huashi.eftransfer.app.modules.user.dto.AdminUserAccessUpdateRequest;
import com.huashi.eftransfer.app.modules.user.dto.AdminUserCreateRequest;
import com.huashi.eftransfer.app.modules.user.dto.AdminUserPageQuery;
import com.huashi.eftransfer.app.modules.user.service.AccountActionService;
import com.huashi.eftransfer.app.modules.user.service.UserAdminService;
import com.huashi.eftransfer.app.modules.user.vo.AccountActionLinkVO;
import com.huashi.eftransfer.app.modules.user.vo.AdminUserProvisionResultVO;
import com.huashi.eftransfer.app.modules.user.vo.UserSummaryVO;
import com.huashi.eftransfer.shared.api.ApiResponse;
import com.huashi.eftransfer.shared.page.PageResult;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@PreAuthorize("hasRole('ADMIN')")
@Validated
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserAdminService userAdminService;
    private final AccountActionService accountActionService;

    public AdminUserController(UserAdminService userAdminService, AccountActionService accountActionService) {
        this.userAdminService = userAdminService;
        this.accountActionService = accountActionService;
    }

    @GetMapping
    public ApiResponse<PageResult<UserSummaryVO>> listUsers(@Valid @ModelAttribute AdminUserPageQuery query) {
        return ApiResponse.success(userAdminService.pageUsers(query), MDC.get("traceId"));
    }

    @PostMapping
    public ApiResponse<AdminUserProvisionResultVO> createUser(@Valid @RequestBody AdminUserCreateRequest request) {
        return ApiResponse.success(userAdminService.createUser(request), MDC.get("traceId"));
    }

    @PutMapping("/{userId}/access")
    public ApiResponse<UserSummaryVO> updateUserAccess(
            @PathVariable Long userId,
            @Valid @RequestBody AdminUserAccessUpdateRequest request
    ) {
        return ApiResponse.success(userAdminService.updateUserAccess(userId, request), MDC.get("traceId"));
    }

    @PostMapping("/{userId}/invite-link")
    public ApiResponse<AccountActionLinkVO> createInviteLink(@PathVariable Long userId) {
        return ApiResponse.success(accountActionService.createInviteLink(userId), MDC.get("traceId"));
    }

    @PostMapping("/{userId}/password-reset-link")
    public ApiResponse<AccountActionLinkVO> createPasswordResetLink(@PathVariable Long userId) {
        return ApiResponse.success(accountActionService.createPasswordResetLink(userId), MDC.get("traceId"));
    }
}
