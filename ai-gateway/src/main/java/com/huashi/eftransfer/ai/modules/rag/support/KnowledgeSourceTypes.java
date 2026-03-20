package com.huashi.eftransfer.ai.modules.rag.support;

import java.util.LinkedHashSet;
import java.util.Set;

public final class KnowledgeSourceTypes {

    public static final String LEXICAL_PAIR = "LEXICAL_PAIR";
    public static final String LEXICAL_SENSE = "LEXICAL_SENSE";
    public static final String LEXICAL_EXAMPLE = "LEXICAL_EXAMPLE";
    public static final String ERROR_TYPE = "ERROR_TYPE";
    public static final String INTERVENTION_TEMPLATE = "INTERVENTION_TEMPLATE";
    public static final String TRAINING_GUIDE = "TRAINING_GUIDE";
    public static final String COURSE_GUIDE = "COURSE_GUIDE";

    public static final Set<String> APP_SERVER_SOURCE_TYPES = Set.of(LEXICAL_PAIR, LEXICAL_SENSE, LEXICAL_EXAMPLE);
    public static final Set<String> SEED_SOURCE_TYPES = Set.of(ERROR_TYPE, INTERVENTION_TEMPLATE, TRAINING_GUIDE, COURSE_GUIDE);
    public static final Set<String> ALL_SOURCE_TYPES = Set.of(
            LEXICAL_PAIR,
            LEXICAL_SENSE,
            LEXICAL_EXAMPLE,
            ERROR_TYPE,
            INTERVENTION_TEMPLATE,
            TRAINING_GUIDE,
            COURSE_GUIDE
    );

    private KnowledgeSourceTypes() {
    }

    public static Set<String> normalizeRequestedTypes(Set<String> sourceTypes) {
        if (sourceTypes == null || sourceTypes.isEmpty()) {
            return ALL_SOURCE_TYPES;
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String sourceType : sourceTypes) {
            if (sourceType == null) {
                continue;
            }
            normalized.add(sourceType.trim().toUpperCase());
        }
        return normalized;
    }
}
