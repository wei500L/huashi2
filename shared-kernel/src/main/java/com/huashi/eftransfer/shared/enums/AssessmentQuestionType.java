package com.huashi.eftransfer.shared.enums;

import java.util.Arrays;

public enum AssessmentQuestionType {
    SINGLE_CHOICE("SINGLE_CHOICE", "Single Choice"),
    MULTIPLE_CHOICE("MULTIPLE_CHOICE", "Multiple Choice"),
    FILL_BLANK("FILL_BLANK", "Fill Blank");

    private final String code;
    private final String label;

    AssessmentQuestionType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public static AssessmentQuestionType fromCode(String value) {
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(value)
                        || item.name().equalsIgnoreCase(value)
                        || item.code.replace("_", "").equalsIgnoreCase(value.replace("_", "")))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported assessmentQuestionType: " + value));
    }
}
