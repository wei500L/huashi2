package com.huashi.eftransfer.shared.enums;

import java.util.Arrays;

public enum TrainingItemType {
    CHOICE("choice", "Choice"),
    JUDGMENT("judgment", "Judgment");

    private final String code;
    private final String label;

    TrainingItemType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public static TrainingItemType fromCode(String value) {
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(value) || item.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported trainingItemType: " + value));
    }
}
