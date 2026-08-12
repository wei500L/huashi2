package com.huashi.eftransfer.shared.enums;

import java.util.Arrays;

public enum AssessmentFileBindingStatus {
    TEMPORARY("TEMPORARY", "Temporary"),
    BOUND("BOUND", "Bound"),
    ORPHANED("ORPHANED", "Orphaned"),
    DELETED("DELETED", "Deleted");

    private final String code;
    private final String label;

    AssessmentFileBindingStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public static AssessmentFileBindingStatus fromCode(String value) {
        String normalized = normalize(value);
        return Arrays.stream(values())
                .filter(item -> normalize(item.code).equals(normalized)
                        || normalize(item.name()).equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported assessmentFileBindingStatus: " + value));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
    }
}
