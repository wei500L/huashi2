package com.huashi.eftransfer.shared.enums;

import java.util.Arrays;

public enum TrainingPlanStatus {
    GENERATED("generated", "Generated"),
    STARTED("started", "Started"),
    COMPLETED("completed", "Completed");

    private final String code;
    private final String label;

    TrainingPlanStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public static TrainingPlanStatus fromCode(String value) {
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(value) || item.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported trainingPlanStatus: " + value));
    }
}
