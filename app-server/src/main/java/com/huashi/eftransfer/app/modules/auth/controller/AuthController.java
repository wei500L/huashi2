package com.huashi.eftransfer.app.modules.auth.controller;

import com.huashi.eftransfer.app.common.security.JwtPrincipal;
import com.huashi.eftransfer.app.common.security.ratelimit.AuthRequestRateLimiter;
import com.huashi.eftransfer.app.modules.auth.dto.ChangePasswordRequest;
import com.huashi.eftransfer.app.modules.auth.dto.LoginRequest;
import com.huashi.eftransfer.app.modules.auth.dto.ResolveStudentRegistrationContextRequest;
import com.huashi.eftransfer.app.modules.auth.dto.RegisterStudentRequest;
import com.huashi.eftransfer.app.modules.auth.dto.RefreshTokenRequest;
import com.huashi.eftransfer.app.modules.auth.service.AuthService;
import com.huashi.eftransfer.app.modules.auth.support.AuthClientContext;
import com.huashi.eftransfer.app.modules.auth.support.AuthRefreshCookie;
import com.huashi.eftransfer.app.modules.auth.vo.IssuedAuthSession;
import com.huashi.eftransfer.app.modules.auth.vo.LoginResponse;
import com.huashi.eftransfer.app.modules.auth.vo.StudentRegistrationContextVO;
import com.huashi.eftransfer.app.modules.user.dto.CompleteAccountActionRequest;
import com.huashi.eftransfer.app.modules.user.service.AccountActionService;
import com.huashi.eftransfer.app.modules.user.vo.AccountActionPreviewVO;
import com.huashi.eftransfer.app.modules.user.vo.CurrentUserVO;
import com.huashi.eftransfer.app.modules.user.vo.SessionOverviewVO;
import com.huashi.eftransfer.shared.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthRequestRateLimiter authRequestRateLimiter;
    private final AccountActionService accountActionService;

    public AuthController(
            AuthService authService,
            AuthRequestRateLimiter authRequestRateLimiter,
            AccountActionService accountActionService
    ) {
        this.authService = authService;
        this.authRequestRateLimiter = authRequestRateLimiter;
        this.accountActionService = accountActionService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(HttpServletRequest httpRequest, @Valid @RequestBody LoginRequest request) {
        authRequestRateLimiter.checkLogin(httpRequest, request);
        return sessionResponse(httpRequest, authService.login(request, AuthClientContext.from(httpRequest)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(
            HttpServletRequest httpRequest,
            @CookieValue(value = AuthRefreshCookie.NAME, required = false) String refreshCookie,
            @RequestBody(required = false) RefreshTokenRequest request
    ) {
        String refreshToken = firstNonBlank(refreshCookie, request == null ? null : request.refreshToken());
        if (!StringUtils.hasText(refreshToken)) {
            return anonymousSessionResponse(httpRequest);
        }
        authRequestRateLimiter.checkRefresh(httpRequest, refreshToken);
        return sessionResponse(httpRequest, authService.refresh(refreshToken, AuthClientContext.from(httpRequest)));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<LoginResponse>> registerStudent(
            HttpServletRequest httpRequest,
            @Valid @RequestBody RegisterStudentRequest request
    ) {
        authRequestRateLimiter.checkRegister(httpRequest);
        return sessionResponse(httpRequest, authService.registerStudent(request, AuthClientContext.from(httpRequest)));
    }

    @PostMapping("/register/context")
    public ApiResponse<StudentRegistrationContextVO> resolveRegistrationContext(
            HttpServletRequest httpRequest,
            @Valid @RequestBody ResolveStudentRegistrationContextRequest request
    ) {
        authRequestRateLimiter.checkRegistrationContext(httpRequest);
        return ApiResponse.success(authService.resolveRegistrationContext(request, AuthClientContext.from(httpRequest)), MDC.get("traceId"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest httpRequest,
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        authService.logout(principal);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, AuthRefreshCookie.clear(httpRequest.isSecure()).toString())
                .body(ApiResponse.success("Logout succeeded", MDC.get("traceId")));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            HttpServletRequest httpRequest,
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        authRequestRateLimiter.checkChangePassword(httpRequest, principal);
        authService.changePassword(principal, request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, AuthRefreshCookie.clear(httpRequest.isSecure()).toString())
                .body(ApiResponse.success("Password changed", MDC.get("traceId")));
    }

    @GetMapping("/me")
    public ApiResponse<CurrentUserVO> me(@AuthenticationPrincipal JwtPrincipal principal) {
        return ApiResponse.success(authService.currentUser(principal), MDC.get("traceId"));
    }

    @GetMapping("/session-overview")
    public ApiResponse<SessionOverviewVO> sessionOverview(@AuthenticationPrincipal JwtPrincipal principal) {
        return ApiResponse.success(accountActionService.getSessionOverview(principal), MDC.get("traceId"));
    }

    @GetMapping("/account-actions/{token}")
    public ApiResponse<AccountActionPreviewVO> previewAccountAction(@PathVariable String token) {
        return ApiResponse.success(accountActionService.preview(token), MDC.get("traceId"));
    }

    @PostMapping("/account-actions/{token}/complete")
    public ApiResponse<Void> completeAccountAction(
            @PathVariable String token,
            @Valid @RequestBody CompleteAccountActionRequest request
    ) {
        accountActionService.complete(token, request);
        return ApiResponse.success("Account action completed", MDC.get("traceId"));
    }

    private ResponseEntity<ApiResponse<LoginResponse>> sessionResponse(
            HttpServletRequest httpRequest,
            IssuedAuthSession issued
    ) {
        ResponseCookie cookie = AuthRefreshCookie.issue(
                issued.refreshToken(),
                issued.refreshTtl(),
                httpRequest.isSecure()
        );
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.success(issued.response(), MDC.get("traceId")));
    }

    private ResponseEntity<ApiResponse<LoginResponse>> anonymousSessionResponse(HttpServletRequest httpRequest) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, AuthRefreshCookie.clear(httpRequest.isSecure()).toString())
                .body(ApiResponse.success((LoginResponse) null, MDC.get("traceId")));
    }

    private static String firstNonBlank(String primary, String fallback) {
        if (StringUtils.hasText(primary)) {
            return primary.trim();
        }
        if (StringUtils.hasText(fallback)) {
            return fallback.trim();
        }
        return null;
    }
}
