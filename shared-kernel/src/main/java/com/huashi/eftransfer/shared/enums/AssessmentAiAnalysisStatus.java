package com.huashi.eftransfer.shared.enums;

import java.util.Arrays;

public enum AssessmentAiAnalysisStatus {
    PENDING("PENDING", "Pending"),
    PROCESSING("PROCESSING", "Processing"),
    COMPLETED("COMPLETED", "Completed"),
    FALLBACK("FALLBACK", "Rule-based Fallback"),
    FAILED("FAILED", "Failed");

    private final String code;
    private final String label;

    AssessmentAiAnalysisStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public static AssessmentAiAnalysisStatus fromCode(String value) {
        String normalized = normalize(value);
        if (normalized.equals("RUNNING")) {
            return PROCESSING;
        }
        if (normalized.equals("RULEFALLBACK")) {
            return FALLBACK;
        }
        if (normalized.equals("SUCCEEDED") || normalized.equals("SUCCESS")) {
            return COMPLETED;
        }
        return Arrays.stream(values())
                .filter(item -> normalize(item.code).equals(normalized)
                        || normalize(item.name()).equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported assessmentAiAnalysisStatus: " + value));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
    }
}
