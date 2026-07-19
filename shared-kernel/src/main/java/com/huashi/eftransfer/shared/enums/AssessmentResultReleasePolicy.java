package com.huashi.eftransfer.shared.enums;

import java.util.Arrays;

public enum AssessmentResultReleasePolicy {
    IMMEDIATE,
    AFTER_DUE;

    public static AssessmentResultReleasePolicy fromCode(String value) {
        return Arrays.stream(values())
                .filter(item -> item.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported assessmentResultReleasePolicy: " + value));
    }
}
