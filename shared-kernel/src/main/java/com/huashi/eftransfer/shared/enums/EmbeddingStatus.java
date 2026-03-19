package com.huashi.eftransfer.shared.enums;

import java.util.Arrays;

public enum EmbeddingStatus {
    PENDING("pending", "Pending"),
    EMBEDDED("embedded", "Embedded"),
    FAILED("failed", "Failed");

    private final String code;
    private final String label;

    EmbeddingStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public static EmbeddingStatus fromCode(String value) {
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(value) || item.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported embeddingStatus: " + value));
    }
}
