package com.huashi.eftransfer.shared.enums;

import java.util.Arrays;

public enum TransferCategory {
    COGNATE("COGNATE", "Cognate"),
    FALSE_FRIEND("FALSE_FRIEND", "False Friend"),
    FRENCH_CONTROL("FRENCH_CONTROL", "French-only Control");

    private final String code;
    private final String label;

    TransferCategory(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public static TransferCategory fromCode(String value) {
        String normalized = normalize(value);
        if (normalized.equals("PUREFRENCH") || normalized.equals("FRENCHONLY") || normalized.equals("PUREFRENCHCONTROL")) {
            return FRENCH_CONTROL;
        }
        return Arrays.stream(values())
                .filter(item -> normalize(item.code).equals(normalized)
                        || normalize(item.name()).equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported transferCategory: " + value));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
    }
}
