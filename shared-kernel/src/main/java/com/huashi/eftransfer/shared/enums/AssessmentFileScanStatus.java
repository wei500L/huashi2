package com.huashi.eftransfer.shared.enums;

import java.util.Arrays;

public enum AssessmentFileScanStatus {
    PENDING("PENDING", "Pending scan"),
    CLEAN("CLEAN", "Clean"),
    INFECTED("INFECTED", "Infected"),
    FAILED("FAILED", "Scan failed");

    private final String code;
    private final String label;

    AssessmentFileScanStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public static AssessmentFileScanStatus fromCode(String value) {
        String normalized = normalize(value);
        return Arrays.stream(values())
                .filter(item -> normalize(item.code).equals(normalized)
                        || normalize(item.name()).equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported assessmentFileScanStatus: " + value));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
    }
}
