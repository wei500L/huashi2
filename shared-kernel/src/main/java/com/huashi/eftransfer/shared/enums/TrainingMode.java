package com.huashi.eftransfer.shared.enums;

import java.util.Arrays;

public enum TrainingMode {
    COGNATE_BOOST("cognate_strengthening", "Cognate Strengthening"),
    FALSE_FRIEND_DISCRIM("false_friend_discrimination", "False Friend Discrimination"),
    CONTEXT_FIX("context_repair", "Context Repair"),
    SPEED_CHALLENGE("rapid_recognition", "Rapid Recognition");

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

    public static TrainingMode fromCode(String value) {
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(value)
                        || item.name().equalsIgnoreCase(value)
                        || (item == COGNATE_BOOST && "cognate_boost".equalsIgnoreCase(value))
                        || (item == FALSE_FRIEND_DISCRIM && "false_friend_discrim".equalsIgnoreCase(value))
                        || (item == CONTEXT_FIX && "context_fix".equalsIgnoreCase(value))
                        || (item == SPEED_CHALLENGE && "speed_challenge".equalsIgnoreCase(value)))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported trainingMode: " + value));
    }
}
