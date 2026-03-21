package com.huashi.eftransfer.ai.common.runtime;

import com.huashi.eftransfer.shared.ai.config.AiOpsConfigPayload;
import org.springframework.web.client.RestClient;

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
}
