package com.huashi.eftransfer.ai.common.runtime;

import com.huashi.eftransfer.shared.ai.config.AiOpsConfigPayload;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.client.RestClient;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.Map;

public record AiRuntimeBundle(
        AiOpsConfigPayload config,
        Map<String, AiProviderRuntime> providerRuntimes,
        RestClient appServerRestClient,
        String source,
        Long version,
        OffsetDateTime appliedAt
) {

    public AiProviderRuntime providerRuntime(String providerName) {
        return providerRuntimes == null ? null : providerRuntimes.get(providerName);
    }

    public ChatClient chatClient() {
        if (config == null || config.provider() == null || !StringUtils.hasText(config.provider().activeProvider())) {
            return null;
        }
        AiProviderRuntime runtime = providerRuntime(config.provider().activeProvider());
        return runtime == null ? null : runtime.chatClient();
    }
}
