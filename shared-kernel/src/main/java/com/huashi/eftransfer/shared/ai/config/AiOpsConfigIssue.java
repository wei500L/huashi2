package com.huashi.eftransfer.shared.ai.config;

import java.util.Map;

public record AiOpsConfigIssue(
        String field,
        String code,
        String defaultMessage,
        Map<String, Object> args
) {
    public AiOpsConfigIssue(String field, String defaultMessage) {
        this(field, "legacy_message", defaultMessage, Map.of());
    }

    public AiOpsConfigIssue(String field, String code, String defaultMessage) {
        this(field, code, defaultMessage, Map.of());
    }

    public String message() {
        return defaultMessage;
    }
}
