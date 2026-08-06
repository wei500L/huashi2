package com.huashi.eftransfer.shared.enums;

import java.util.Arrays;

/**
 * Separates classroom assessment content from research questionnaire content
 * while keeping both flows on the same paper model and editor.
 */
public enum AssessmentPaperPurpose {
    CLASS_ASSESSMENT("CLASS_ASSESSMENT", "Class assessment"),
    RESEARCH_SURVEY("RESEARCH_SURVEY", "Research survey");

    private final String code;
    private final String label;

    AssessmentPaperPurpose(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public static AssessmentPaperPurpose fromCode(String value) {
        if (value == null || value.isBlank()) {
            return CLASS_ASSESSMENT;
        }
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(value.trim()) || item.name().equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported assessmentPaperPurpose: " + value));
    }
}
