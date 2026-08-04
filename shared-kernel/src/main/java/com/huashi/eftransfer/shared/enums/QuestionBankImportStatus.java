package com.huashi.eftransfer.shared.enums;

import java.util.Arrays;

public enum QuestionBankImportStatus {
    UPLOADED("UPLOADED", "Uploaded"),
    PREFLIGHT_FAILED("PREFLIGHT_FAILED", "Preflight Failed"),
    REVIEW_REQUIRED("REVIEW_REQUIRED", "Review Required"),
    READY("READY", "Ready"),
    COMMITTED("COMMITTED", "Committed");

    private final String code;
    private final String label;

    QuestionBankImportStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public static QuestionBankImportStatus fromCode(String value) {
        String normalized = normalize(value);
        if (normalized.equals("PREFLIGHTED") || normalized.equals("READYTOCOMMIT")) {
            return READY;
        }
        return Arrays.stream(values())
                .filter(item -> normalize(item.code).equals(normalized)
                        || normalize(item.name()).equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported questionBankImportStatus: " + value));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
    }
}
