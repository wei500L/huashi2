package com.huashi.eftransfer.app.modules.assessment.support;

import java.net.Inet6Address;
import java.net.InetAddress;

public final class AssessmentClientIpNormalizer {
    private AssessmentClientIpNormalizer() { }

    public static String normalize(String rawAddress) {
        if (rawAddress == null) return null;
        String value = rawAddress.trim();
        if (value.isEmpty() || "unknown".equalsIgnoreCase(value)) return null;
        if (value.matches("^\\d{1,3}(?:\\.\\d{1,3}){3}$")) {
            String[] parts = value.split("\\.");
            for (String part : parts) {
                int octet;
                try {
                    octet = Integer.parseInt(part);
                } catch (NumberFormatException exception) {
                    return null;
                }
                if (octet < 0 || octet > 255) return null;
            }
            return String.join(".", java.util.Arrays.stream(parts)
                    .map(part -> Integer.toString(Integer.parseInt(part))).toList());
        }
        if (!value.contains(":") || value.contains("%") || !value.matches("^[0-9A-Fa-f:.]+$")) return null;
        try {
            InetAddress address = InetAddress.getByName(value);
            return address instanceof Inet6Address ? address.getHostAddress() : null;
        } catch (Exception exception) {
            return null;
        }
    }
}
