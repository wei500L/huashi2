package com.huashi.eftransfer.shared.enums;

import java.util.Arrays;

public enum AssessmentSubmitReason {
    MANUAL,
    TIMEOUT,
    SCHEDULER;

    public static AssessmentSubmitReason fromCode(String value) {
        return Arrays.stream(values())
                .filter(item -> item.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported assessmentSubmitReason: " + value));
    }
}
