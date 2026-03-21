package com.huashi.eftransfer.shared.ai.config;

import java.util.Map;

public record AiOpsProviderConfig(
        String activeProvider,
        String fallbackProvider,
        Map<String, AiOpsProviderDefinition> providers
) {
}
