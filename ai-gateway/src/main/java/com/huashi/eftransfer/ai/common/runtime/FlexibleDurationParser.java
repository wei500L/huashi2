package com.huashi.eftransfer.ai.common.runtime;

import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class FlexibleDurationParser {

    private static final Pattern SIMPLE_PATTERN = Pattern.compile(
            "^([0-9]+(?:\\.[0-9]+)?)(ms|s|m|h|d)$",
            Pattern.CASE_INSENSITIVE
    );

    private FlexibleDurationParser() {
    }

    static Duration parse(String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("duration value must not be blank");
        }

        String trimmed = value.trim();
        try {
            return Duration.parse(trimmed.toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            // Fallback to short-form durations such as 30s, 500ms, 2m.
        }

        Matcher matcher = SIMPLE_PATTERN.matcher(trimmed);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("must be a valid duration");
        }

        BigDecimal amount = new BigDecimal(matcher.group(1));
        String unit = matcher.group(2).toLowerCase(Locale.ROOT);
        BigDecimal millis = switch (unit) {
            case "ms" -> amount;
            case "s" -> amount.multiply(BigDecimal.valueOf(1_000L));
            case "m" -> amount.multiply(BigDecimal.valueOf(60_000L));
            case "h" -> amount.multiply(BigDecimal.valueOf(3_600_000L));
            case "d" -> amount.multiply(BigDecimal.valueOf(86_400_000L));
            default -> throw new IllegalArgumentException("must be a valid duration");
        };
        return Duration.ofNanos(millis.multiply(BigDecimal.valueOf(1_000_000L)).longValue());
    }
}
