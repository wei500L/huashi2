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
import com.huashi.eftransfer.shared.ai.config.AiOpsRagAppServerConfig;
import com.huashi.eftransfer.shared.ai.config.AiOpsRagConfig;
import com.huashi.eftransfer.shared.ai.config.AiOpsRagIngestionConfig;
import com.huashi.eftransfer.shared.ai.config.AiOpsRagRetrievalConfig;
import com.huashi.eftransfer.shared.ai.config.AiOpsRerankConfig;
import com.huashi.eftransfer.shared.ai.config.AiOpsResilienceConfig;
import com.huashi.eftransfer.shared.ai.config.AiOpsFlexibleDurationParser;
import com.huashi.eftransfer.shared.security.InternalApiHeaders;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.retry.RetryUtils;
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

    public AiRuntimeBundleFactory(
            RestClient.Builder restClientBuilder,
            ClientHttpRequestInterceptor providerRequestCaptureInterceptor,
            ProviderErrorSupport providerErrorSupport
    ) {
        this.restClientBuilder = restClientBuilder;
        this.providerRequestCaptureInterceptor = providerRequestCaptureInterceptor;
        this.providerErrorSupport = providerErrorSupport;
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

        return build(
                new AiOpsConfigPayload(
                        new AiOpsProviderConfig(
                                providerProperties.getActiveProvider(),
                                providerProperties.getFallbackProvider(),
                                providerDefinitions
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
                                        ragProperties.getIngestion().getEmbeddingBatchSize()
                                ),
                                new AiOpsRagRetrievalConfig(
                                        ragProperties.getRetrieval().getRecallTopK(),
                                        ragProperties.getRetrieval().getRecallThreshold(),
                                        ragProperties.getRetrieval().getRerankTopN(),
                                        ragProperties.getRetrieval().getRerankThreshold(),
                                        ragProperties.getRetrieval().getFinalTopK()
                                )
                        )
                ),
                source,
                version
        );
    }

    public AiRuntimeBundle build(AiOpsConfigPayload payload, String source, Long version) {
        Map<String, AiProviderRuntime> providerRuntimes = new LinkedHashMap<>();
        for (Map.Entry<String, AiOpsProviderDefinition> entry : payload.provider().providers().entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            providerRuntimes.put(entry.getKey(), buildProviderRuntime(entry.getKey(), entry.getValue(), payload.resilience()));
        }

        return new AiRuntimeBundle(
                payload,
                providerRuntimes,
                appServerRestClient(payload.rag().appServer()),
                source,
                version,
                OffsetDateTime.now()
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

        OpenAiApi chatApi = OpenAiApi.builder()
                .baseUrl(normalizeOpenAiBaseUrl(chatConfig.baseUrl()))
                .apiKey(defaultString(chatConfig.apiKey()))
                .restClientBuilder(providerRestClientBuilder(
                        chatConfig.baseUrl(),
                        chatConfig.apiKey(),
                        parseDuration(chatConfig.timeout())
                ))
                .build();
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(chatApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(chatConfig.model())
                        .temperature(chatConfig.temperature())
                        .maxTokens(chatConfig.maxTokens())
                        .build())
                .retryTemplate(RetryUtils.DEFAULT_RETRY_TEMPLATE)
                .build();

        OpenAiApi embeddingApi = OpenAiApi.builder()
                .baseUrl(normalizeOpenAiBaseUrl(embeddingConfig.baseUrl()))
                .apiKey(defaultString(embeddingConfig.apiKey()))
                .restClientBuilder(providerRestClientBuilder(
                        embeddingConfig.baseUrl(),
                        embeddingConfig.apiKey(),
                        parseDuration(embeddingConfig.timeout())
                ))
                .build();
        EmbeddingModel embeddingModel = new OpenAiEmbeddingModel(
                embeddingApi,
                MetadataMode.EMBED,
                OpenAiEmbeddingOptions.builder()
                        .model(embeddingConfig.model())
                        .dimensions(embeddingConfig.dimension())
                        .build(),
                RetryUtils.DEFAULT_RETRY_TEMPLATE
        );

        RestClient.Builder rerankBuilder = providerRestClientBuilder(
                rerankConfig.baseUrl(),
                rerankConfig.apiKey(),
                parseDuration(rerankConfig.timeout())
        );
        if (StringUtils.hasText(rerankConfig.baseUrl())) {
            rerankBuilder.baseUrl(rerankConfig.baseUrl());
        }

        RetryRegistry retryRegistry = RetryRegistry.of(RetryConfig.custom()
                .maxAttempts(resilience.maxAttempts())
                .waitDuration(parseDuration(resilience.waitDuration()))
                .retryOnException(providerErrorSupport::isRetryable)
                .failAfterMaxAttempts(true)
                .build());
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.of(CircuitBreakerConfig.custom()
                .failureRateThreshold(resilience.failureRateThreshold())
                .slidingWindowSize(resilience.slidingWindowSize())
                .minimumNumberOfCalls(resilience.slidingWindowSize())
                .waitDurationInOpenState(parseDuration(resilience.openStateDuration()))
                .recordException(providerErrorSupport::shouldRecordForCircuitBreaker)
                .build());

        return new AiProviderRuntime(
                providerName,
                definition,
                org.springframework.ai.chat.client.ChatClient.builder(chatModel).build(),
                chatModel,
                embeddingModel,
                rerankBuilder.build(),
                retryRegistry,
                circuitBreakerRegistry
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

    private RestClient.Builder providerRestClientBuilder(String baseUrl, String apiKey, Duration timeout) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(timeout);

        RestClient.Builder builder = restClientBuilder.clone()
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
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
                        providerProperties.getChat().getBaseUrl(),
                        providerProperties.getChat().getApiKey(),
                        providerProperties.getChat().getModel(),
                        formatDuration(providerProperties.getChat().getTimeout()),
                        providerProperties.getChat().getTemperature(),
                        providerProperties.getChat().getMaxTokens()
                ),
                new AiOpsEmbeddingConfig(
                        providerProperties.getEmbedding().getBaseUrl(),
                        providerProperties.getEmbedding().getApiKey(),
                        providerProperties.getEmbedding().getModel(),
                        formatDuration(providerProperties.getEmbedding().getTimeout()),
                        providerProperties.getEmbedding().getDimension()
                ),
                new AiOpsRerankConfig(
                        providerProperties.getRerank().getBaseUrl(),
                        providerProperties.getRerank().getApiKey(),
                        providerProperties.getRerank().getModel(),
                        formatDuration(providerProperties.getRerank().getTimeout())
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
}
