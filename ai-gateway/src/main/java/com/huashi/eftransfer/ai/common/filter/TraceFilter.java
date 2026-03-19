package com.huashi.eftransfer.ai.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class TraceFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String TRACE_ID_KEY = "traceId";
    private static final Logger log = LoggerFactory.getLogger(TraceFilter.class);

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        long start = System.currentTimeMillis();
        String traceId = resolveTraceId(request);
        MDC.put(TRACE_ID_KEY, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - start;
            log.info(
                    "event=request_completed method={} path={} status={} durationMs={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    duration
            );
            MDC.remove(TRACE_ID_KEY);
        }
    }

    private String resolveTraceId(HttpServletRequest request) {
        String header = request.getHeader(TRACE_ID_HEADER);
        return header != null && !header.isBlank() ? header : UUID.randomUUID().toString();
    }
}
