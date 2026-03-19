package com.huashi.eftransfer.shared.enums;

import java.util.Arrays;

public enum DiagnosisAnswerState {
    PENDING("pending", "Pending"),
    ANSWERED("answered", "Answered");

    private final String code;
    private final String label;

    DiagnosisAnswerState(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public static DiagnosisAnswerState fromCode(String value) {
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(value) || item.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported diagnosisAnswerState: " + value));
    }
}
