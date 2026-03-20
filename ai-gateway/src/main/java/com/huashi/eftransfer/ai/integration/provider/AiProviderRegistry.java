package com.huashi.eftransfer.ai.integration.provider;

import com.huashi.eftransfer.ai.common.runtime.AiRuntimeConfigService;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class AiProviderRegistry {

    private final AiRuntimeConfigService runtimeConfigService;
    private final Map<String, AiProviderFacade> providers;

    public AiProviderRegistry(AiRuntimeConfigService runtimeConfigService, List<AiProviderFacade> providers) {
        this.runtimeConfigService = runtimeConfigService;
        this.providers = providers.stream().collect(Collectors.toMap(AiProviderFacade::providerName, Function.identity()));
    }

    public AiProviderFacade resolveActiveProvider() {
        String activeProvider = runtimeConfigService.current().config().provider().activeProvider();
        AiProviderFacade provider = providers.get(activeProvider);
        if (provider == null) {
            throw new BusinessException(
                    ResultCode.AI_PROVIDER_UNAVAILABLE,
                    "No implemented AI provider found for " + activeProvider,
                    503
            );
        }
        return provider;
    }
}
