package com.huashi.eftransfer.shared.enums;

public enum TrainingMode {
    RULE_BASED("rule_based", "Rule Based"),
    AI_ASSISTED("ai_assisted", "AI Assisted"),
    MIXED("mixed", "Mixed");

    private final String code;
    private final String label;

    TrainingMode(String code, String label) {
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
