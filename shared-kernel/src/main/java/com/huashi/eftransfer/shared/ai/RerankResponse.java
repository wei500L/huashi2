package com.huashi.eftransfer.shared.ai;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RerankResponse(
        String provider,
        String model,
        String providerRequestId,
        Integer totalTokens,
        List<RerankItem> items
) {
}
