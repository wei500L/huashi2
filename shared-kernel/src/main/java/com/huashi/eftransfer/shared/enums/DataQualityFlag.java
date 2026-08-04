package com.huashi.eftransfer.shared.enums;

import java.util.Arrays;

public enum DataQualityFlag {
    FAST_ITEM("FAST_ITEM", "Item response below 300 ms"),
    SHORT_TOTAL_DURATION("SHORT_TOTAL_DURATION", "Effective duration below 10 minutes"),
    TIMING_GAP("TIMING_GAP", "Timing data is incomplete");

    private final String code;
    private final String label;

    DataQualityFlag(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public static DataQualityFlag fromCode(String value) {
        String normalized = normalize(value);
        if (normalized.startsWith("FASTITEM")) {
            return FAST_ITEM;
        }
        if (normalized.equals("TOTALTIMETOOSHORT") || normalized.equals("SHORTDURATION")) {
            return SHORT_TOTAL_DURATION;
        }
        return Arrays.stream(values())
                .filter(item -> normalize(item.code).equals(normalized)
                        || normalize(item.name()).equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported dataQualityFlag: " + value));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
    }
}
