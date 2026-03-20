package com.huashi.eftransfer.shared.ai;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RerankItem(
        int index,
        double relevanceScore,
        String document
) {
}
