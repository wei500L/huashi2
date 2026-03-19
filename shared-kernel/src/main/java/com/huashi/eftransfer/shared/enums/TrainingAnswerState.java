package com.huashi.eftransfer.shared.enums;

import java.util.Arrays;

public enum TrainingAnswerState {
    PENDING("pending", "Pending"),
    ANSWERED("answered", "Answered");

    private final String code;
    private final String label;

    TrainingAnswerState(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public static TrainingAnswerState fromCode(String value) {
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(value) || item.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported trainingAnswerState: " + value));
    }
}
