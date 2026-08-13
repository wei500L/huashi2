package com.huashi.eftransfer.app.common.security;

import com.huashi.eftransfer.shared.api.ApiResponse;
import com.huashi.eftransfer.shared.api.ResultCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Set;

/**
 * Cookie-backed login refresh mutations need a custom header so classic
 * cross-site form POST cannot reuse {@code EF_REFRESH}. Bearer JWT APIs stay
 * CSRF-exempt.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 41)
public class AuthCookieCsrfHeaderFilter extends OncePerRequestFilter {

    public static final String REQUIRED_HEADER = PublicAssessmentCsrfHeaderFilter.REQUIRED_HEADER;
    public static final String REQUIRED_VALUE = PublicAssessmentCsrfHeaderFilter.REQUIRED_VALUE;
    private static final Set<String> PROTECTED_PATHS = Set.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh",
            "/api/auth/logout"
    );

    private final ObjectMapper objectMapper;

    public AuthCookieCsrfHeaderFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String method = request.getMethod();
        if (method == null
                || "GET".equalsIgnoreCase(method)
                || "HEAD".equalsIgnoreCase(method)
                || "OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }
        String path = request.getRequestURI();
        return path == null || !PROTECTED_PATHS.contains(path);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String header = request.getHeader(REQUIRED_HEADER);
        if (REQUIRED_VALUE.equalsIgnoreCase(header)) {
            filterChain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(
                response.getWriter(),
                ApiResponse.failure(
                        ResultCode.FORBIDDEN,
                        "Authentication mutations require the X-Requested-With header",
                        MDC.get("traceId")
                )
        );
    }
}
