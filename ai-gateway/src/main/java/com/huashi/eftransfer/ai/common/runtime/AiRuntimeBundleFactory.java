package com.huashi.eftransfer.ai.common.runtime;

import com.huashi.eftransfer.ai.common.config.AiProviderProperties;
import com.huashi.eftransfer.ai.common.config.AiResilienceProperties;
import com.huashi.eftransfer.ai.common.exception.ProviderErrorSupport;
import com.huashi.eftransfer.ai.modules.rag.config.RagProperties;
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
import com.huashi.eftransfer.shared.ai.config.AiOpsFlexibleDurationParser;
import com.huashi.eftransfer.shared.security.InternalApiHeaders;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class AiRuntimeBundleFactory {

    private final RestClient.Builder restClientBuilder;
    private final ClientHttpRequestInterceptor providerRequestCaptureInterceptor;
    private final ProviderErrorSupport providerErrorSupport;
    private final AiCircuitBreakerManager circuitBreakerManager;

    @Autowired
    public AiRuntimeBundleFactory(
            RestClient.Builder restClientBuilder,
            ClientHttpRequestInterceptor providerRequestCaptureInterceptor,
            ProviderErrorSupport providerErrorSupport,
            AiCircuitBreakerManager circuitBreakerManager
    ) {
        this.restClientBuilder = restClientBuilder;
        this.providerRequestCaptureInterceptor = providerRequestCaptureInterceptor;
        this.providerErrorSupport = providerErrorSupport;
        this.circuitBreakerManager = circuitBreakerManager;
    }

    public AiRuntimeBundleFactory(
            RestClient.Builder restClientBuilder,
            ClientHttpRequestInterceptor providerRequestCaptureInterceptor,
            ProviderErrorSupport providerErrorSupport
    ) {
        this(restClientBuilder, providerRequestCaptureInterceptor, providerErrorSupport, new AiCircuitBreakerManager(providerErrorSupport));
    }

    public AiRuntimeBundle fromProperties(
            AiProviderProperties providerProperties,
            AiResilienceProperties resilienceProperties,
            RagProperties ragProperties,
            String source,
            Long version
    ) {
        Map<String, AiOpsProviderDefinition> providerDefinitions = new LinkedHashMap<>();
        for (Map.Entry<String, AiProviderProperties.ProviderProperties> entry : providerProperties.getProviders().entrySet()) {
            providerDefinitions.put(entry.getKey(), toProviderDefinition(entry.getValue()));
        }
        ensureProviderDefinition(providerDefinitions, providerProperties, providerProperties.getActiveProvider());
        ensureProviderDefinition(providerDefinitions, providerProperties, providerProperties.getFallbackProvider());
        Map<String, AiOpsProviderDefinition> orderedProviderDefinitions = orderProviderDefinitions(
                providerDefinitions,
                providerProperties.getActiveProvider(),
                providerProperties.getFallbackProvider()
        );

        return build(
                new AiOpsConfigPayload(
                        new AiOpsProviderConfig(
                                providerProperties.getActiveProvider(),
                                providerProperties.getFallbackProvider(),
                                orderedProviderDefinitions
                        ),
                        new AiOpsResilienceConfig(
                                resilienceProperties.getMaxAttempts(),
                                formatDuration(resilienceProperties.getWaitDuration()),
                                resilienceProperties.getFailureRateThreshold(),
                                resilienceProperties.getSlidingWindowSize(),
                                formatDuration(resilienceProperties.getOpenStateDuration())
                        ),
                        new AiOpsRagConfig(
                                new AiOpsRagAppServerConfig(
                                        ragProperties.getAppServer().getBaseUrl(),
                                        ragProperties.getAppServer().getInternalToken(),
                                        formatDuration(ragProperties.getAppServer().getConnectTimeout()),
                                        formatDuration(ragProperties.getAppServer().getReadTimeout())
                                ),
                                new AiOpsRagIngestionConfig(
                                        ragProperties.getIngestion().getExportPageSize(),
                                        ragProperties.getIngestion().getEmbeddingBatchSize(),
                                        ragProperties.getIngestion().isFailedRetryEnabled(),
                                        ragProperties.getIngestion().getFailedRetryLimit()
                                ),
                                new AiOpsRagRetrievalConfig(
                                        ragProperties.getRetrieval().getRecallTopK(),
                                        ragProperties.getRetrieval().getRecallThreshold(),
                                        ragProperties.getRetrieval().getRerankTopN(),
                                        ragProperties.getRetrieval().getRerankThreshold(),
                                        ragProperties.getRetrieval().getFinalTopK(),
                                        ragProperties.getRetrieval().getHnswEfSearch()
                                )
                        )
                ),
                source,
                version
        );
    }

    public AiRuntimeBundle build(AiOpsConfigPayload payload, String source, Long version) {
        AiOpsConfigPayload canonicalPayload = canonicalizePayload(payload);
        Map<String, AiProviderRuntime> providerRuntimes = new LinkedHashMap<>();
        for (Map.Entry<String, AiOpsProviderDefinition> entry : canonicalPayload.provider().providers().entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            providerRuntimes.put(entry.getKey(), buildProviderRuntime(entry.getKey(), entry.getValue(), canonicalPayload.resilience()));
        }

        return new AiRuntimeBundle(
                canonicalPayload,
                providerRuntimes,
                appServerRestClient(canonicalPayload.rag().appServer()),
                source,
                version,
                OffsetDateTime.now()
        );
    }

    private AiOpsConfigPayload canonicalizePayload(AiOpsConfigPayload payload) {
        if (payload == null || payload.provider() == null || payload.provider().providers() == null) {
            return payload;
        }
        Map<String, AiOpsProviderDefinition> orderedProviderDefinitions = orderProviderDefinitions(
                payload.provider().providers(),
                payload.provider().activeProvider(),
                payload.provider().fallbackProvider()
        );
        return new AiOpsConfigPayload(
                new AiOpsProviderConfig(
                        payload.provider().activeProvider(),
                        payload.provider().fallbackProvider(),
                        orderedProviderDefinitions
                ),
                payload.resilience(),
                payload.rag()
        );
    }

    private AiProviderRuntime buildProviderRuntime(
            String providerName,
            AiOpsProviderDefinition definition,
            AiOpsResilienceConfig resilience
    ) {
        AiOpsChatConfig chatConfig = definition.chat();
        AiOpsEmbeddingConfig embeddingConfig = definition.embedding();
        AiOpsRerankConfig rerankConfig = definition.rerank();

        requireSupportedChatProtocol(providerName, chatConfig.protocol());
        requireSupportedProtocol(providerName, "embedding", embeddingConfig.protocol(), AiOpsProtocols.OPENAI_COMPAT);
        requireSupportedRerankProtocol(providerName, rerankConfig.protocol());

        OpenAiApi chatApi = OpenAiApi.builder()
                .baseUrl(normalizeOpenAiBaseUrl(chatConfig.baseUrl()))
                .apiKey(defaultString(chatConfig.apiKey()))
                .restClientBuilder(providerRestClientBuilder(
                        chatConfig.baseUrl(),
                        chatConfig.apiKey(),
                        parseDuration(chatConfig.connectTimeout()),
                        parseDuration(chatConfig.readTimeout())
                ))
                .build();
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(chatApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(chatConfig.model())
                        .temperature(chatConfig.temperature())
                        .maxTokens(chatConfig.maxTokens())
                        .build())
                .retryTemplate(singleAttemptRetryTemplate())
                .build();
        RestClient chatRestClient = providerRestClientBuilder(
                normalizeOpenAiBaseUrl(chatConfig.baseUrl()),
                chatConfig.apiKey(),
                parseDuration(chatConfig.connectTimeout()),
                parseDuration(chatConfig.readTimeout())
        ).build();

        OpenAiApi embeddingApi = OpenAiApi.builder()
                .baseUrl(normalizeOpenAiBaseUrl(embeddingConfig.baseUrl()))
                .apiKey(defaultString(embeddingConfig.apiKey()))
                .restClientBuilder(providerRestClientBuilder(
                        embeddingConfig.baseUrl(),
                        embeddingConfig.apiKey(),
                        parseDuration(embeddingConfig.connectTimeout()),
                        parseDuration(embeddingConfig.readTimeout())
                ))
                .build();
        EmbeddingModel embeddingModel = new OpenAiEmbeddingModel(
                embeddingApi,
                MetadataMode.EMBED,
                OpenAiEmbeddingOptions.builder()
                        .model(embeddingConfig.model())
                        .dimensions(embeddingConfig.dimension())
                        .build(),
                singleAttemptRetryTemplate()
        );

        RestClient.Builder rerankBuilder = providerRestClientBuilder(
                normalizeOpenAiBaseUrl(rerankConfig.baseUrl()),
                rerankConfig.apiKey(),
                parseDuration(rerankConfig.connectTimeout()),
                parseDuration(rerankConfig.readTimeout())
        );
        if (StringUtils.hasText(rerankConfig.baseUrl())) {
            rerankBuilder.baseUrl(normalizeOpenAiBaseUrl(rerankConfig.baseUrl()));
        }

        RetryRegistry retryRegistry = RetryRegistry.of(RetryConfig.custom()
                .maxAttempts(resilience.maxAttempts())
                .waitDuration(parseDuration(resilience.waitDuration()))
                .retryOnException(providerErrorSupport::isRetryable)
                .failAfterMaxAttempts(true)
                .build());
        return new AiProviderRuntime(
                providerName,
                definition,
                org.springframework.ai.chat.client.ChatClient.builder(chatModel).build(),
                chatModel,
                chatRestClient,
                embeddingModel,
                rerankBuilder.build(),
                resilience,
                retryRegistry,
                circuitBreakerManager
        );
    }

    private RestClient appServerRestClient(AiOpsRagAppServerConfig appServerConfig) {
        requireText("rag.appServer.internalToken", appServerConfig.internalToken());
        Duration connectTimeout = parseDuration(appServerConfig.connectTimeout());
        Duration readTimeout = parseDuration(appServerConfig.readTimeout());
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);
        return restClientBuilder.clone()
                .baseUrl(appServerConfig.baseUrl())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(InternalApiHeaders.INTERNAL_TOKEN, appServerConfig.internalToken())
                .requestFactory(requestFactory)
                .build();
    }

    private RestClient.Builder providerRestClientBuilder(
            String baseUrl,
            String apiKey,
            Duration connectTimeout,
            Duration readTimeout
    ) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);

        RestClient.Builder builder = restClientBuilder.clone()
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.USER_AGENT, "PostmanRuntime/7.43.0")
                .requestInterceptor(providerRequestCaptureInterceptor);

        if (StringUtils.hasText(baseUrl)) {
            builder.baseUrl(baseUrl);
        }
        if (StringUtils.hasText(apiKey)) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
        }
        return builder;
    }

    private AiOpsProviderDefinition toProviderDefinition(AiProviderProperties.ProviderProperties providerProperties) {
        return new AiOpsProviderDefinition(
                new AiOpsChatConfig(
                        providerProperties.getChat().getProtocol(),
                        providerProperties.getChat().getBaseUrl(),
                        providerProperties.getChat().getApiKey(),
                        providerProperties.getChat().getModel(),
                        formatDuration(providerProperties.getChat().getConnectTimeout()),
                        formatDuration(providerProperties.getChat().getReadTimeout()),
                        providerProperties.getChat().getTemperature(),
                        providerProperties.getChat().getMaxTokens()
                ),
                new AiOpsEmbeddingConfig(
                        providerProperties.getEmbedding().getProtocol(),
                        providerProperties.getEmbedding().getBaseUrl(),
                        providerProperties.getEmbedding().getApiKey(),
                        providerProperties.getEmbedding().getModel(),
                        providerProperties.getEmbedding().getMultimodalModel(),
                        formatDuration(providerProperties.getEmbedding().getConnectTimeout()),
                        formatDuration(providerProperties.getEmbedding().getReadTimeout()),
                        providerProperties.getEmbedding().getDimension()
                ),
                new AiOpsRerankConfig(
                        providerProperties.getRerank().getProtocol(),
                        providerProperties.getRerank().getBaseUrl(),
                        providerProperties.getRerank().getApiKey(),
                        providerProperties.getRerank().getModel(),
                        providerProperties.getRerank().getMultimodalModel(),
                        formatDuration(providerProperties.getRerank().getConnectTimeout()),
                        formatDuration(providerProperties.getRerank().getReadTimeout())
                )
        );
    }

    private void ensureProviderDefinition(
            Map<String, AiOpsProviderDefinition> providerDefinitions,
            AiProviderProperties properties,
            String providerName
    ) {
        if (!StringUtils.hasText(providerName) || providerDefinitions.containsKey(providerName)) {
            return;
        }
        AiProviderProperties.ProviderProperties providerProperties = properties.getProviderProperties(providerName);
        providerDefinitions.put(providerName, toProviderDefinition(
                providerProperties == null ? new AiProviderProperties.ProviderProperties() : providerProperties
        ));
    }

    private Map<String, AiOpsProviderDefinition> orderProviderDefinitions(
            Map<String, AiOpsProviderDefinition> providerDefinitions,
            String activeProvider,
            String fallbackProvider
    ) {
        Map<String, AiOpsProviderDefinition> ordered = new LinkedHashMap<>();
        addProviderDefinition(ordered, providerDefinitions, activeProvider);
        addProviderDefinition(ordered, providerDefinitions, fallbackProvider);
        providerDefinitions.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> ordered.putIfAbsent(entry.getKey(), entry.getValue()));
        return ordered;
    }

    private void addProviderDefinition(
            Map<String, AiOpsProviderDefinition> ordered,
            Map<String, AiOpsProviderDefinition> providerDefinitions,
            String providerName
    ) {
        if (!StringUtils.hasText(providerName) || providerDefinitions == null || !providerDefinitions.containsKey(providerName)) {
            return;
        }
        ordered.putIfAbsent(providerName, providerDefinitions.get(providerName));
    }

    private String normalizeOpenAiBaseUrl(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            return baseUrl;
        }
        String normalized = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;
        return normalized.endsWith("/v1")
                ? normalized.substring(0, normalized.length() - 3)
                : normalized;
    }

    private void requireText(String field, String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }

    private Duration parseDuration(String value) {
        Duration duration = AiOpsFlexibleDurationParser.parse(value);
        if (duration.isNegative() || duration.isZero()) {
            throw new IllegalArgumentException("duration must be positive");
        }
        return duration;
    }

    private String formatDuration(Duration value) {
        return value == null ? null : value.toString();
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private RetryTemplate singleAttemptRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setRetryPolicy(new SimpleRetryPolicy(1));
        return retryTemplate;
    }

    private void requireSupportedProtocol(String providerName, String capability, String actual, String expected) {
        if (!expected.equals(actual)) {
            throw new IllegalArgumentException(
                    "Unsupported " + capability + " protocol '" + actual + "' for provider " + providerName + "; expected " + expected
            );
        }
    }

    private void requireSupportedRerankProtocol(String providerName, String actual) {
        if (AiOpsProtocols.OPENAI_RERANK.equals(actual)
                || AiOpsProtocols.OPENAI_CHAT_RERANK.equals(actual)
                || AiOpsProtocols.OPENAI_COMPAT.equals(actual)) {
            return;
        }
        throw new IllegalArgumentException(
                "Unsupported rerank protocol '" + actual + "' for provider " + providerName
                        + "; expected one of " + AiOpsProtocols.OPENAI_RERANK + ", " + AiOpsProtocols.OPENAI_CHAT_RERANK
        );
    }

    private void requireSupportedChatProtocol(String providerName, String actual) {
        if (AiOpsProtocols.OPENAI_COMPAT.equals(actual)
                || AiOpsProtocols.OPENAI_RESPONSES.equals(actual)) {
            return;
        }
        throw new IllegalArgumentException(
                "provider " + providerName + " chat protocol is " + actual
                        + "; expected one of " + AiOpsProtocols.OPENAI_COMPAT + ", " + AiOpsProtocols.OPENAI_RESPONSES
        );
    }
}
