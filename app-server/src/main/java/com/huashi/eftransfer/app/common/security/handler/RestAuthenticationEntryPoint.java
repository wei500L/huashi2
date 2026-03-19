package com.huashi.eftransfer.app.common.security.handler;

import com.huashi.eftransfer.app.common.security.JwtAuthenticationFilter;
import com.huashi.eftransfer.shared.api.ApiResponse;
import com.huashi.eftransfer.shared.api.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        ResultCode resultCode = (ResultCode) request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_CODE);
        String message = (String) request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_MESSAGE);

        if (resultCode == null) {
            resultCode = ResultCode.UNAUTHORIZED;
        }
        if (message == null || message.isBlank()) {
            message = resultCode.message();
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(
                response.getWriter(),
                ApiResponse.failure(resultCode, message, traceId())
        );
    }

    private String traceId() {
        return MDC.get("traceId");
    }
}
