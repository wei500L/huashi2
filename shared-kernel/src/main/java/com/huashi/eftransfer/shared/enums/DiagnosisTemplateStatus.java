package com.huashi.eftransfer.shared.enums;

import java.util.Arrays;

public enum DiagnosisTemplateStatus {
    DRAFT("draft", "Draft"),
    PUBLISHED("published", "Published"),
    ARCHIVED("archived", "Archived");

    private final String code;
    private final String label;

    DiagnosisTemplateStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public static DiagnosisTemplateStatus fromCode(String value) {
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(value) || item.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported diagnosisTemplateStatus: " + value));
    }
}
