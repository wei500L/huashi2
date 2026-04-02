package com.huashi.eftransfer.shared.enums;

import java.util.Arrays;

public enum AssessmentAttemptStatus {
    IN_PROGRESS("IN_PROGRESS", "In Progress"),
    SUBMITTED("SUBMITTED", "Submitted");

    private final String code;
    private final String label;

    AssessmentAttemptStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public static AssessmentAttemptStatus fromCode(String value) {
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(value) || item.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported assessmentAttemptStatus: " + value));
    }
}
