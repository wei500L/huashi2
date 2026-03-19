package com.huashi.eftransfer.shared.enums;

import java.util.Arrays;

public enum WrongBookMasteryStatus {
    ACTIVE("active", "Active"),
    REVIEWING("reviewing", "Reviewing"),
    MASTERED("mastered", "Mastered");

    private final String code;
    private final String label;

    WrongBookMasteryStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public static WrongBookMasteryStatus fromCode(String value) {
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(value) || item.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported wrongBookMasteryStatus: " + value));
    }
}
