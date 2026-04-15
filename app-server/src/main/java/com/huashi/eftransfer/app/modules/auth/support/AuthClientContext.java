package com.huashi.eftransfer.app.modules.auth.support;

import com.huashi.eftransfer.app.common.security.ClientRequestContextResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

public record AuthClientContext(
        String ipAddress,
        String userAgentFingerprint
) {

    public static AuthClientContext from(HttpServletRequest request) {
        if (request == null) {
            return new AuthClientContext(
                    ClientRequestContextResolver.UNKNOWN_IP,
                    ClientRequestContextResolver.UNKNOWN_USER_AGENT
            );
        }
        return new AuthClientContext(
                ClientRequestContextResolver.resolveIpAddress(request),
                ClientRequestContextResolver.normalizeUserAgent(request.getHeader("User-Agent"))
        );
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

}
