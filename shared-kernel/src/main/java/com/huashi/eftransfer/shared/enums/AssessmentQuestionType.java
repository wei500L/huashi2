package com.huashi.eftransfer.shared.enums;

import java.util.Arrays;

public enum AssessmentQuestionType {
    INSTRUCTION("INSTRUCTION", "Instruction"),
    INFORMED_CONSENT("INFORMED_CONSENT", "Informed Consent"),
    SHORT_TEXT("SHORT_TEXT", "Short Text"),
    NUMBER("NUMBER", "Number"),
    SINGLE_CHOICE("SINGLE_CHOICE", "Single Choice"),
    MULTIPLE_CHOICE("MULTIPLE_CHOICE", "Multiple Choice"),
    FILL_BLANK("FILL_BLANK", "Fill Blank"),
    TRUE_FALSE("TRUE_FALSE", "True/False"),
    TRUE_FALSE_WITH_JUSTIFICATION("TRUE_FALSE_WITH_JUSTIFICATION", "True/False with Justification"),
    SPELLING("SPELLING", "Spelling with Hint");

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
        String normalized = normalize(value);
        return Arrays.stream(values())
                .filter(item -> normalize(item.code).equals(normalized)
                        || normalize(item.name()).equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported assessmentQuestionType: " + value));
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
    }
}
