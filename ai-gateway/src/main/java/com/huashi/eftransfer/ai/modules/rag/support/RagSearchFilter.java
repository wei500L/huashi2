package com.huashi.eftransfer.ai.modules.rag.support;

import java.util.LinkedHashSet;
import java.util.Set;

public record RagSearchFilter(
        Set<String> sourceTypes,
        Set<String> sourceIds
) {

    public static RagSearchFilter empty() {
        return new RagSearchFilter(Set.of(), Set.of());
    }

    public RagSearchFilter {
        sourceTypes = sourceTypes == null ? Set.of() : new LinkedHashSet<>(sourceTypes);
        sourceIds = sourceIds == null ? Set.of() : new LinkedHashSet<>(sourceIds);
    }
}
