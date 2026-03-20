package com.huashi.eftransfer.app.common.security;

import com.huashi.eftransfer.shared.api.ApiResponse;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.exception.BusinessException;
import com.huashi.eftransfer.shared.security.InternalApiHeaders;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
public class InternalApiAuthenticationFilter extends OncePerRequestFilter {

    private final InternalApiTokenAuthenticator authenticator;
    private final ObjectMapper objectMapper;

    public InternalApiAuthenticationFilter(InternalApiTokenAuthenticator authenticator, ObjectMapper objectMapper) {
        this.authenticator = authenticator;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/internal/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            authenticator.authenticate(request.getHeader(InternalApiHeaders.INTERNAL_TOKEN));
            filterChain.doFilter(request, response);
        } catch (BusinessException exception) {
            response.setStatus(exception.getHttpStatus());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(
                    response.getWriter(),
                    ApiResponse.failure(ResultCode.FORBIDDEN, exception.getMessage(), MDC.get("traceId"))
            );
        }
    }
}
