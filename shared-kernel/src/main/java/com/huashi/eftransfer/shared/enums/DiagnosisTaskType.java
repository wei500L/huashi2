package com.huashi.eftransfer.shared.enums;

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
}
