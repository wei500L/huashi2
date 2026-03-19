package com.huashi.eftransfer.shared.enums;

import java.util.Arrays;

public enum ReviewScheduleStatus {
    PENDING("pending", "Pending"),
    COMPLETED("completed", "Completed"),
    SKIPPED("skipped", "Skipped");

    private final String code;
    private final String label;

    ReviewScheduleStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public static ReviewScheduleStatus fromCode(String value) {
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(value) || item.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported reviewScheduleStatus: " + value));
    }
}
