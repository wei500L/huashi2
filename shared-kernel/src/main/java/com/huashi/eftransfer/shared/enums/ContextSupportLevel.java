package com.huashi.eftransfer.shared.enums;

public enum ContextSupportLevel {
    LOW("low", "Low"),
    MEDIUM("medium", "Medium"),
    HIGH("high", "High");

    private final String code;
    private final String label;

    ContextSupportLevel(String code, String label) {
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
