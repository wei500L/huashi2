package com.huashi.eftransfer.shared.enums;

import java.util.Arrays;

public enum DiagnosisErrorType {
    FALSE_FRIEND_CONFUSION("false_friend_confusion", "False Friend Confusion"),
    CONTEXT_IGNORED("context_ignored", "Context Ignored"),
    OVER_TRANSFER("over_transfer", "Over Transfer"),
    UNDER_TRANSFER("under_transfer", "Under Transfer"),
    ORTHOGRAPHIC_INTERFERENCE("orthographic_interference", "Orthographic Interference"),
    SEMANTIC_MISFIRE("semantic_misfire", "Semantic Misfire");

    private final String code;
    private final String label;

    DiagnosisErrorType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public static DiagnosisErrorType fromCode(String value) {
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(value) || item.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported diagnosisErrorType: " + value));
    }
}
