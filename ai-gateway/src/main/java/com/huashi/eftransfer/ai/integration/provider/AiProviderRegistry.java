package com.huashi.eftransfer.ai.integration.provider;

import com.huashi.eftransfer.ai.common.config.AiProviderProperties;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class AiProviderRegistry {

    private final AiProviderProperties properties;
    private final Map<String, AiProviderFacade> providers;

    public AiProviderRegistry(AiProviderProperties properties, List<AiProviderFacade> providers) {
        this.properties = properties;
        this.providers = providers.stream().collect(Collectors.toMap(AiProviderFacade::providerName, Function.identity()));
    }

    public AiProviderFacade resolveActiveProvider() {
        AiProviderFacade provider = providers.get(properties.getActiveProvider());
        if (provider == null) {
            throw new BusinessException(
                    ResultCode.AI_PROVIDER_UNAVAILABLE,
                    "No implemented AI provider found for " + properties.getActiveProvider(),
                    503
            );
        }
        return provider;
    }
}
