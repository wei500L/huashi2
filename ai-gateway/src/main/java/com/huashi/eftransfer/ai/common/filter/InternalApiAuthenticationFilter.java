package com.huashi.eftransfer.ai.common.filter;

import com.huashi.eftransfer.ai.common.config.InternalApiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huashi.eftransfer.shared.api.ApiResponse;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.security.InternalApiHeaders;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;

@Component
public class InternalApiAuthenticationFilter extends OncePerRequestFilter {

    private final InternalApiProperties properties;
    private final ObjectMapper objectMapper;

    public InternalApiAuthenticationFilter(InternalApiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void validateConfiguration() {
        if (properties.isEnabled() && !StringUtils.hasText(properties.getToken())) {
            throw new IllegalStateException("platform.internal-api.token must be configured when internal API protection is enabled");
        }
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
        if (!properties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = request.getHeader(InternalApiHeaders.INTERNAL_TOKEN);
        if (!StringUtils.hasText(token) || !Objects.equals(properties.getToken(), token)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(
                    response.getWriter(),
                    ApiResponse.failure(ResultCode.FORBIDDEN, "Invalid internal API token", MDC.get("traceId"))
            );
            return;
        }

        filterChain.doFilter(request, response);
    }
}
