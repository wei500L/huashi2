package com.huashi.eftransfer.shared.ai;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatResponse(
        String provider,
        String model,
        String content,
        String finishReason,
        String providerRequestId,
        TokenUsage usage
) {
}
