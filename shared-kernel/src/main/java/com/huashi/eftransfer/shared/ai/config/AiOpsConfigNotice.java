package com.huashi.eftransfer.shared.ai.config;

import java.util.Map;

public record AiOpsConfigNotice(
        String code,
        String severity,
        String defaultMessage,
        Map<String, Object> args
) {
    public AiOpsConfigNotice(String code, String severity, String defaultMessage) {
        this(code, severity, defaultMessage, Map.of());
    }

    public String message() {
        return defaultMessage;
    }
}
