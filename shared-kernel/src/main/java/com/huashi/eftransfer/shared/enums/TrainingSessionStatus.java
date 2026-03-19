package com.huashi.eftransfer.shared.enums;

import java.util.Arrays;

public enum TrainingSessionStatus {
    IN_PROGRESS("in_progress", "In Progress"),
    COMPLETED("completed", "Completed");

    private final String code;
    private final String label;

    TrainingSessionStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public static TrainingSessionStatus fromCode(String value) {
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(value) || item.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported trainingSessionStatus: " + value));
    }
}
