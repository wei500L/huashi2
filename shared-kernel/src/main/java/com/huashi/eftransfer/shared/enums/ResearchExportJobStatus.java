package com.huashi.eftransfer.shared.enums;

import java.util.Arrays;

public enum ResearchExportJobStatus {
    PENDING("PENDING", "Pending"),
    PROCESSING("PROCESSING", "Processing"),
    COMPLETED("COMPLETED", "Completed"),
    FAILED("FAILED", "Failed");

    private final String code;
    private final String label;

    ResearchExportJobStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public static ResearchExportJobStatus fromCode(String value) {
        String normalized = normalize(value);
        return Arrays.stream(values())
                .filter(item -> normalize(item.code).equals(normalized)
                        || normalize(item.name()).equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported researchExportJobStatus: " + value));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
    }
}
