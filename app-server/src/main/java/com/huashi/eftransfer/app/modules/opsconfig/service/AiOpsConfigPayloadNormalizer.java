package com.huashi.eftransfer.app.modules.opsconfig.service;

import com.huashi.eftransfer.shared.ai.config.AiOpsChatConfig;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigPayload;
import com.huashi.eftransfer.shared.ai.config.AiOpsEmbeddingConfig;
import com.huashi.eftransfer.shared.ai.config.AiOpsProviderConfig;
import com.huashi.eftransfer.shared.ai.config.AiOpsProviderDefinition;
import com.huashi.eftransfer.shared.ai.config.AiOpsProtocols;
import com.huashi.eftransfer.shared.ai.config.AiOpsRagAppServerConfig;
import com.huashi.eftransfer.shared.ai.config.AiOpsRagConfig;
import com.huashi.eftransfer.shared.ai.config.AiOpsRagIngestionConfig;
import com.huashi.eftransfer.shared.ai.config.AiOpsRagRetrievalConfig;
import com.huashi.eftransfer.shared.ai.config.AiOpsRerankConfig;
import com.huashi.eftransfer.shared.ai.config.AiOpsResilienceConfig;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class AiOpsConfigPayloadNormalizer {

    public AiOpsConfigPayload normalize(AiOpsConfigPayload payload) {
        if (payload == null) {
            return new AiOpsConfigPayload(
                    new AiOpsProviderConfig(null, null, Map.of()),
                    new AiOpsResilienceConfig(null, null, null, null, null),
                    new AiOpsRagConfig(
                            new AiOpsRagAppServerConfig(null, null, null, null),
                            new AiOpsRagIngestionConfig(null, null),
                            new AiOpsRagRetrievalConfig(null, null, null, null, null, null)
                    )
            );
        }

        AiOpsProviderConfig provider = payload.provider() == null
                ? new AiOpsProviderConfig(null, null, Map.of())
                : payload.provider();
        Map<String, AiOpsProviderDefinition> providers = new LinkedHashMap<>();
        if (provider.providers() != null) {
            for (Map.Entry<String, AiOpsProviderDefinition> entry : provider.providers().entrySet()) {
                providers.put(entry.getKey(), normalizeProviderDefinition(entry.getValue()));
            }
        }
        Map<String, AiOpsProviderDefinition> orderedProviders = canonicalizeProviders(
                providers,
                provider.activeProvider(),
                provider.fallbackProvider()
        );
        AiOpsResilienceConfig resilience = payload.resilience() == null
                ? new AiOpsResilienceConfig(null, null, null, null, null)
                : payload.resilience();

        AiOpsRagConfig rag = payload.rag() == null
                ? new AiOpsRagConfig(null, null, null)
                : payload.rag();
        AiOpsRagAppServerConfig appServer = rag.appServer() == null
                ? new AiOpsRagAppServerConfig(null, null, null, null)
                : rag.appServer();
        AiOpsRagIngestionConfig ingestion = rag.ingestion() == null
                ? new AiOpsRagIngestionConfig(null, null)
                : rag.ingestion();
        AiOpsRagRetrievalConfig retrieval = rag.retrieval() == null
                ? new AiOpsRagRetrievalConfig(null, null, null, null, null, null)
                : rag.retrieval();

        return new AiOpsConfigPayload(
                new AiOpsProviderConfig(provider.activeProvider(), provider.fallbackProvider(), orderedProviders),
                resilience,
                new AiOpsRagConfig(appServer, ingestion, retrieval)
        );
    }

    public AiOpsProviderDefinition normalizeProviderDefinition(AiOpsProviderDefinition definition) {
        if (definition == null) {
            return new AiOpsProviderDefinition(
                    normalizeChatConfig(null),
                    normalizeEmbeddingConfig(null),
                    normalizeRerankConfig(null)
            );
        }
        return new AiOpsProviderDefinition(
                normalizeChatConfig(definition.chat()),
                normalizeEmbeddingConfig(definition.embedding()),
                normalizeRerankConfig(definition.rerank())
        );
    }

    private AiOpsChatConfig normalizeChatConfig(AiOpsChatConfig config) {
        if (config == null) {
            return new AiOpsChatConfig(AiOpsProtocols.OPENAI_COMPAT, null, null, null, null, null, null, null);
        }
        return new AiOpsChatConfig(
                defaultProtocol(config.protocol(), AiOpsProtocols.OPENAI_COMPAT),
                config.baseUrl(),
                config.apiKey(),
                config.model(),
                config.connectTimeout(),
                config.readTimeout(),
                config.temperature(),
                config.maxTokens()
        );
    }

    private AiOpsEmbeddingConfig normalizeEmbeddingConfig(AiOpsEmbeddingConfig config) {
        if (config == null) {
            return new AiOpsEmbeddingConfig(AiOpsProtocols.OPENAI_COMPAT, null, null, null, null, null, null);
        }
        return new AiOpsEmbeddingConfig(
                defaultProtocol(config.protocol(), AiOpsProtocols.OPENAI_COMPAT),
                config.baseUrl(),
                config.apiKey(),
                config.model(),
                config.connectTimeout(),
                config.readTimeout(),
                config.dimension()
        );
    }

    private AiOpsRerankConfig normalizeRerankConfig(AiOpsRerankConfig config) {
        if (config == null) {
            return new AiOpsRerankConfig(AiOpsProtocols.QWEN_RERANK, null, null, null, null, null);
        }
        return new AiOpsRerankConfig(
                defaultProtocol(config.protocol(), AiOpsProtocols.QWEN_RERANK),
                config.baseUrl(),
                config.apiKey(),
                config.model(),
                config.connectTimeout(),
                config.readTimeout()
        );
    }

    private String defaultProtocol(String protocol, String defaultProtocol) {
        return StringUtils.hasText(protocol) ? protocol : defaultProtocol;
    }

    private Map<String, AiOpsProviderDefinition> canonicalizeProviders(
            Map<String, AiOpsProviderDefinition> providers,
            String activeProvider,
            String fallbackProvider
    ) {
        Map<String, AiOpsProviderDefinition> ordered = new LinkedHashMap<>();
        addOrderedProvider(ordered, providers, activeProvider);
        addOrderedProvider(ordered, providers, fallbackProvider);
        if (providers != null) {
            providers.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> ordered.putIfAbsent(entry.getKey(), entry.getValue()));
        }
        return ordered;
    }

    private void addOrderedProvider(
            Map<String, AiOpsProviderDefinition> ordered,
            Map<String, AiOpsProviderDefinition> providers,
            String providerName
    ) {
        if (!StringUtils.hasText(providerName) || providers == null || !providers.containsKey(providerName)) {
            return;
        }
        ordered.putIfAbsent(providerName, providers.get(providerName));
    }
}
