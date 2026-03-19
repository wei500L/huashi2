package com.huashi.eftransfer.shared.enums;

import java.util.Arrays;

public enum KnowledgeStatus {
    DRAFT("draft", "Draft"),
    READY("ready", "Ready"),
    DISABLED("disabled", "Disabled");

    private final String code;
    private final String label;

    KnowledgeStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public static KnowledgeStatus fromCode(String value) {
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(value) || item.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported knowledgeStatus: " + value));
    }
}
