package com.huashi.eftransfer.shared.enums;

import java.util.Arrays;

public enum DiagnosisTaskType {
    REACTION_TIME("reaction_time", "Reaction Time Task"),
    SEMANTIC_JUDGEMENT("semantic_judgement", "Semantic Judgement Task");

    private final String code;
    private final String label;

    DiagnosisTaskType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public static DiagnosisTaskType fromCode(String value) {
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(value)
                        || item.name().equalsIgnoreCase(value)
                        || (item == REACTION_TIME && "reaction_time_task".equalsIgnoreCase(value))
                        || (item == SEMANTIC_JUDGEMENT && "semantic_judgement_task".equalsIgnoreCase(value)))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported diagnosisTaskType: " + value));
    }
}
