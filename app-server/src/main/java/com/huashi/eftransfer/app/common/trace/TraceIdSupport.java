package com.huashi.eftransfer.app.common.trace;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;

import java.util.UUID;

public final class TraceIdSupport {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String TRACE_ID_MDC_KEY = "traceId";
    public static final String USER_ID_MDC_KEY = "userId";

    private TraceIdSupport() {
    }

    public static String resolveIncoming(HttpServletRequest request) {
        String header = request.getHeader(TRACE_ID_HEADER);
        if (hasText(header)) {
            return header;
        }
        return UUID.randomUUID().toString();
    }

    public static String currentOrResolve(HttpServletRequest request) {
        String current = MDC.get(TRACE_ID_MDC_KEY);
        if (hasText(current)) {
            return current;
        }
        if (request != null) {
            String header = request.getHeader(TRACE_ID_HEADER);
            if (hasText(header)) {
                return header;
            }
        }
        return UUID.randomUUID().toString();
    }

    public static String currentTraceId() {
        return MDC.get(TRACE_ID_MDC_KEY);
    }

    public static void bind(String traceId) {
        MDC.put(TRACE_ID_MDC_KEY, traceId);
    }

    public static void bindUserId(Long userId) {
        if (userId == null) {
            return;
        }
        MDC.put(USER_ID_MDC_KEY, String.valueOf(userId));
    }

    public static void clear() {
        MDC.remove(TRACE_ID_MDC_KEY);
        MDC.remove(USER_ID_MDC_KEY);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
