package com.huashi.eftransfer.app.modules.analytics.support;

import java.util.List;

public final class AnalyticsConstants {

    public static final String PROFILE_SCOPE_STUDENT = "STUDENT";
    public static final String PROFILE_SCOPE_CLASS = "CLASS";

    public static final String SOURCE_DIAGNOSIS = "DIAGNOSIS";
    public static final String SOURCE_TRAINING = "TRAINING";

    public static final String AGGREGATION_LEVEL_SUMMARY = "SUMMARY";
    public static final String AGGREGATION_LEVEL_PAIR = "PAIR";

    public static final String DIMENSION_ALL = "ALL";
    public static final Long SUMMARY_LEXICAL_PAIR_ID = 0L;

    public static final List<String> ERROR_TYPES = List.of(
            "FALSE_FRIEND_CONFUSION",
            "CONTEXT_IGNORED",
            "OVER_TRANSFER",
            "UNDER_TRANSFER",
            "ORTHOGRAPHIC_INTERFERENCE",
            "SEMANTIC_MISFIRE"
    );

    private AnalyticsConstants() {
    }
}
