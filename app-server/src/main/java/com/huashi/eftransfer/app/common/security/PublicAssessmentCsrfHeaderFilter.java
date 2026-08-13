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

/**
 * Cookie-backed public assessment mutations need a custom header so classic
 * cross-site form POST cannot reuse {@code LEXIBRIDGE_SESSION}. JWT APIs stay
 * CSRF-exempt.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 40)
public class PublicAssessmentCsrfHeaderFilter extends OncePerRequestFilter {

    public static final String REQUIRED_HEADER = "X-Requested-With";
    public static final String REQUIRED_VALUE = "XMLHttpRequest";
    static final String PUBLIC_ASSESSMENT_PREFIX = "/api/public/assessments/";

    private final ObjectMapper objectMapper;

    public PublicAssessmentCsrfHeaderFilter(ObjectMapper objectMapper) {
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
        return path == null || !path.startsWith(PUBLIC_ASSESSMENT_PREFIX);
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
                        "Public assessment mutations require the X-Requested-With header",
                        MDC.get("traceId")
                )
        );
    }
}
