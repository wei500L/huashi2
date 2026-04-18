package com.huashi.eftransfer.ai.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class TraceFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String TRACE_ID_KEY = "traceId";
    private static final Logger log = LoggerFactory.getLogger(TraceFilter.class);
    private final ObjectProvider<Tracer> tracerProvider;

    public TraceFilter(ObjectProvider<Tracer> tracerProvider) {
        this.tracerProvider = tracerProvider;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        long start = System.currentTimeMillis();
        String traceId = resolveCurrentTraceId(resolveTraceId(request));
        MDC.put(TRACE_ID_KEY, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            String finalTraceId = resolveCurrentTraceId(traceId);
            MDC.put(TRACE_ID_KEY, finalTraceId);
            response.setHeader(TRACE_ID_HEADER, finalTraceId);
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

    private String resolveCurrentTraceId(String fallback) {
        Tracer tracer = tracerProvider.getIfAvailable();
        if (tracer != null && tracer.currentSpan() != null && tracer.currentSpan().context() != null) {
            String traceId = tracer.currentSpan().context().traceId();
            if (traceId != null && !traceId.isBlank()) {
                return traceId;
            }
        }
        String current = MDC.get(TRACE_ID_KEY);
        return current != null && !current.isBlank() ? current : fallback;
    }
}
