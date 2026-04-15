package com.huashi.eftransfer.app.common.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

import java.util.Locale;

public final class ClientRequestContextResolver {

    public static final String UNKNOWN_IP = "unknown";
    public static final String UNKNOWN_USER_AGENT = "unknown";
    private static final int MAX_USER_AGENT_LENGTH = 512;

    private ClientRequestContextResolver() {
    }

    public static String resolveIpAddress(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN_IP;
        }

        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            String firstHop = forwardedFor.split(",")[0].trim();
            if (StringUtils.hasText(firstHop)) {
                return firstHop;
            }
        }

        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }

        String remoteAddr = request.getRemoteAddr();
        if (!StringUtils.hasText(remoteAddr)) {
            return UNKNOWN_IP;
        }
        return remoteAddr.trim();
    }

    public static String normalizeUserAgent(String userAgent) {
        if (!StringUtils.hasText(userAgent)) {
            return UNKNOWN_USER_AGENT;
        }

        String normalized = userAgent
                .trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);

        if (normalized.length() > MAX_USER_AGENT_LENGTH) {
            return normalized.substring(0, MAX_USER_AGENT_LENGTH);
        }
        return normalized;
    }
}
