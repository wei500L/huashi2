package com.huashi.eftransfer.shared.enums;

import java.util.Arrays;

public enum LexicalPairType {
    COGNATE("cognate", "Cognate"),
    FALSE_FRIEND("false_friend", "False Friend"),
    PARTIAL_COGNATE("partial_cognate", "Partial Cognate"),
    ORTHOGRAPHIC_SIMILAR("orthographic_similar", "Orthographic Similar");

    private final String code;
    private final String label;

    LexicalPairType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public static LexicalPairType fromCode(String value) {
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(value) || item.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported lexicalPairType: " + value));
    }
}
