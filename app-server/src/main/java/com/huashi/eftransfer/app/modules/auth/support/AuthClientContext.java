package com.huashi.eftransfer.app.modules.auth.support;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

import java.util.Locale;

public record AuthClientContext(
        String ipAddress,
        String userAgentFingerprint
) {

    private static final int MAX_USER_AGENT_LENGTH = 512;
    private static final String UNKNOWN_IP = "unknown";
    private static final String UNKNOWN_USER_AGENT = "unknown";

    public static AuthClientContext from(HttpServletRequest request) {
        if (request == null) {
            return new AuthClientContext(UNKNOWN_IP, UNKNOWN_USER_AGENT);
        }
        return new AuthClientContext(resolveIpAddress(request), normalizeUserAgent(request.getHeader("User-Agent")));
    }

    public boolean matchesUserAgent(String expectedFingerprint) {
        if (!StringUtils.hasText(expectedFingerprint)) {
            return true;
        }
        return userAgentFingerprint.equals(expectedFingerprint);
    }

    public boolean isIpChanged(String previousIpAddress) {
        if (!StringUtils.hasText(previousIpAddress)) {
            return false;
        }
        return !ipAddress.equals(previousIpAddress);
    }

    private static String resolveIpAddress(HttpServletRequest request) {
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

    private static String normalizeUserAgent(String userAgent) {
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
