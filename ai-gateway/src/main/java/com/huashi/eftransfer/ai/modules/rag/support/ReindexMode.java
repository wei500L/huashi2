package com.huashi.eftransfer.ai.modules.rag.support;

import java.util.Locale;

public enum ReindexMode {
    FULL,
    INCREMENTAL,
    SOURCE;

    public static ReindexMode fromValue(String value) {
        if (value == null || value.isBlank()) {
            return INCREMENTAL;
        }
        return ReindexMode.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
