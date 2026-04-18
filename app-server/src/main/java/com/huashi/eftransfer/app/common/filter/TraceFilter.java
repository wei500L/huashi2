package com.huashi.eftransfer.app.common.filter;

import com.huashi.eftransfer.app.common.trace.TraceIdSupport;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class TraceFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_HEADER = TraceIdSupport.TRACE_ID_HEADER;
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
        String traceId = resolveTraceId(request);
        TraceIdSupport.bind(traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            String finalTraceId = currentTraceId(traceId);
            TraceIdSupport.bind(finalTraceId);
            response.setHeader(TRACE_ID_HEADER, finalTraceId);
            long duration = System.currentTimeMillis() - start;
            log.info(
                    "event=request_completed method={} path={} status={} durationMs={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    duration
            );
            TraceIdSupport.clear();
        }
    }

    private String resolveTraceId(HttpServletRequest request) {
        return currentTraceId(TraceIdSupport.resolveIncoming(request));
    }

    private String currentTraceId(String fallback) {
        Tracer tracer = tracerProvider.getIfAvailable();
        if (tracer != null && tracer.currentSpan() != null && tracer.currentSpan().context() != null) {
            String traceId = tracer.currentSpan().context().traceId();
            if (traceId != null && !traceId.isBlank()) {
                return traceId;
            }
        }
        String current = TraceIdSupport.currentTraceId();
        return current != null && !current.isBlank() ? current : fallback;
    }
}
