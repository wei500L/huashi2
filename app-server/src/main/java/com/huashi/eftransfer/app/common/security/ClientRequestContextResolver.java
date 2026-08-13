package com.huashi.eftransfer.app.common.security;

import com.huashi.eftransfer.app.modules.assessment.support.AssessmentClientIpNormalizer;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Locale;

public final class ClientRequestContextResolver {

    public static final String UNKNOWN_IP = "unknown";
    public static final String UNKNOWN_USER_AGENT = "unknown";
    public static final List<String> DEFAULT_TRUSTED_PROXY_CIDRS = List.of(
            "127.0.0.0/8",
            "::1/128",
            "10.0.0.0/8",
            "172.16.0.0/12",
            "192.168.0.0/16",
            "fc00::/7"
    );
    private static final int MAX_USER_AGENT_LENGTH = 512;

    private static volatile List<CidrBlock> trustedProxies = parseCidrs(DEFAULT_TRUSTED_PROXY_CIDRS);

    private ClientRequestContextResolver() {
    }

    public static void configureTrustedProxies(List<String> cidrs) {
        trustedProxies = parseCidrs(cidrs == null || cidrs.isEmpty() ? DEFAULT_TRUSTED_PROXY_CIDRS : cidrs);
    }

    public static String resolveIpAddress(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN_IP;
        }

        String remoteAddr = trimToNull(request.getRemoteAddr());
        if (isTrustedProxy(remoteAddr)) {
            String forwarded = firstForwardedHop(request.getHeader("X-Forwarded-For"));
            String normalizedForwarded = AssessmentClientIpNormalizer.normalize(forwarded);
            if (normalizedForwarded != null) {
                return normalizedForwarded;
            }
            String realIp = AssessmentClientIpNormalizer.normalize(trimToNull(request.getHeader("X-Real-IP")));
            if (realIp != null) {
                return realIp;
            }
        }

        String normalizedRemote = AssessmentClientIpNormalizer.normalize(remoteAddr);
        return normalizedRemote == null ? (remoteAddr == null ? UNKNOWN_IP : remoteAddr) : normalizedRemote;
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

    static boolean isTrustedProxy(String remoteAddr) {
        InetAddress address = parseAddress(remoteAddr);
        if (address == null) {
            return false;
        }
        List<CidrBlock> blocks = trustedProxies;
        for (CidrBlock block : blocks) {
            if (block.contains(address)) {
                return true;
            }
        }
        return false;
    }

    private static String firstForwardedHop(String forwardedFor) {
        if (!StringUtils.hasText(forwardedFor)) {
            return null;
        }
        String firstHop = forwardedFor.split(",")[0].trim();
        return StringUtils.hasText(firstHop) ? firstHop : null;
    }

    private static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private static InetAddress parseAddress(String value) {
        String normalized = AssessmentClientIpNormalizer.normalize(value);
        if (normalized == null) {
            return null;
        }
        try {
            return InetAddress.getByName(normalized);
        } catch (UnknownHostException exception) {
            return null;
        }
    }

    private static List<CidrBlock> parseCidrs(List<String> cidrs) {
        return cidrs.stream().map(CidrBlock::parse).toList();
    }
}
