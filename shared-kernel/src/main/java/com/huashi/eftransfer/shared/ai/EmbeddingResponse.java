package com.huashi.eftransfer.shared.ai;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EmbeddingResponse(
        String provider,
        String model,
        Integer dimension,
        String providerRequestId,
        TokenUsage usage,
        List<EmbeddingItem> items
) {
}
