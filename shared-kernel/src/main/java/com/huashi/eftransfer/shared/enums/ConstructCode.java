package com.huashi.eftransfer.shared.enums;

import java.util.Arrays;

/** Research construct measured by a scored questionnaire item. */
public enum ConstructCode {
    LEXICAL_TRANSFER("LEXICAL_TRANSFER", "Lexical Transfer"),
    SEMANTIC_DISCRIMINATION("SEMANTIC_DISCRIMINATION", "Semantic Discrimination"),
    CONTEXT_REPAIR("CONTEXT_REPAIR", "Context Repair");

    private final String code;
    private final String label;

    ConstructCode(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public static ConstructCode fromCode(String value) {
        String normalized = normalize(value);
        return Arrays.stream(values())
                .filter(item -> normalize(item.code).equals(normalized)
                        || normalize(item.name()).equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported constructCode: " + value));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
    }
}
