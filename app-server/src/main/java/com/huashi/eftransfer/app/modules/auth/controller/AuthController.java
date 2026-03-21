package com.huashi.eftransfer.app.modules.auth.controller;

import com.huashi.eftransfer.app.common.security.JwtPrincipal;
import com.huashi.eftransfer.app.common.security.ratelimit.AuthRequestRateLimiter;
import com.huashi.eftransfer.app.modules.auth.dto.LoginRequest;
import com.huashi.eftransfer.app.modules.auth.dto.RefreshTokenRequest;
import com.huashi.eftransfer.app.modules.auth.service.AuthService;
import com.huashi.eftransfer.app.modules.auth.support.AuthClientContext;
import com.huashi.eftransfer.app.modules.auth.vo.LoginResponse;
import com.huashi.eftransfer.app.modules.user.vo.CurrentUserVO;
import com.huashi.eftransfer.shared.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthRequestRateLimiter authRequestRateLimiter;

    public AuthController(AuthService authService, AuthRequestRateLimiter authRequestRateLimiter) {
        this.authService = authService;
        this.authRequestRateLimiter = authRequestRateLimiter;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(HttpServletRequest httpRequest, @Valid @RequestBody LoginRequest request) {
        authRequestRateLimiter.checkLogin(httpRequest, request);
        return ApiResponse.success(authService.login(request, AuthClientContext.from(httpRequest)), MDC.get("traceId"));
    }

    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refresh(HttpServletRequest httpRequest, @Valid @RequestBody RefreshTokenRequest request) {
        authRequestRateLimiter.checkRefresh(httpRequest, request);
        return ApiResponse.success(authService.refresh(request, AuthClientContext.from(httpRequest)), MDC.get("traceId"));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@AuthenticationPrincipal JwtPrincipal principal) {
        authService.logout(principal);
        return ApiResponse.success("Logout succeeded", MDC.get("traceId"));
    }

    @GetMapping("/me")
    public ApiResponse<CurrentUserVO> me(@AuthenticationPrincipal JwtPrincipal principal) {
        return ApiResponse.success(authService.currentUser(principal), MDC.get("traceId"));
    }
}
