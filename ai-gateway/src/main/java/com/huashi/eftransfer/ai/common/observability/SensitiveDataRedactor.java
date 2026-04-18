package com.huashi.eftransfer.ai.common.observability;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

@Component
public class SensitiveDataRedactor {

    private static final int MAX_LENGTH = 1024;
    private static final List<Pattern> JSON_SECRET_PATTERNS = List.of(
            Pattern.compile("(?i)(\"apiKey\"\\s*:\\s*\")([^\"]*)(\")"),
            Pattern.compile("(?i)(\"internalToken\"\\s*:\\s*\")([^\"]*)(\")"),
            Pattern.compile("(?i)(\"authorization\"\\s*:\\s*\")([^\"]*)(\")")
    );
    private static final Pattern BEARER_PATTERN = Pattern.compile("(?i)(Bearer\\s+)([A-Za-z0-9._\\-+/=]+)");

    public String redact(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String redacted = value;
        for (Pattern pattern : JSON_SECRET_PATTERNS) {
            redacted = pattern.matcher(redacted).replaceAll("$1***REDACTED***$3");
        }
        redacted = BEARER_PATTERN.matcher(redacted).replaceAll("$1***REDACTED***");
        if (redacted.length() > MAX_LENGTH) {
            return redacted.substring(0, MAX_LENGTH) + "...(truncated)";
        }
        return redacted;
    }
}
