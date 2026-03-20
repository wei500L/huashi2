package com.huashi.eftransfer.shared.ai;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record StructuredChatResponse(
        String provider,
        String model,
        String rawContent,
        Map<String, Object> structuredData,
        String finishReason,
        String providerRequestId,
        TokenUsage usage
) {
}
