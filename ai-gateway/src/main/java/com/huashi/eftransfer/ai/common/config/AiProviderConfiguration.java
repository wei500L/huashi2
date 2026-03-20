package com.huashi.eftransfer.ai.common.config;

import com.huashi.eftransfer.ai.common.exception.ProviderErrorSupport;
import com.huashi.eftransfer.ai.common.observability.ProviderRequestCaptureInterceptor;
import com.huashi.eftransfer.ai.common.observability.ProviderRequestContextHolder;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
@Configuration
@EnableConfigurationProperties({AiProviderProperties.class, AiResilienceProperties.class})
public class AiProviderConfiguration {

    @Bean
    @ConditionalOnMissingBean(RestClient.Builder.class)
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    public ProviderRequestContextHolder providerRequestContextHolder() {
        return new ProviderRequestContextHolder();
    }

    @Bean
    public ClientHttpRequestInterceptor providerRequestCaptureInterceptor(ProviderRequestContextHolder contextHolder) {
        return new ProviderRequestCaptureInterceptor(contextHolder);
    }

    @Bean
    public OpenAiApi qwenChatOpenAiApi(
            RestClient.Builder builder,
            ClientHttpRequestInterceptor providerRequestCaptureInterceptor,
            AiProviderProperties properties
    ) {
        AiProviderProperties.ChatProperties chat = qwen(properties).getChat();
        return OpenAiApi.builder()
                .baseUrl(normalizeOpenAiBaseUrl(chat.getBaseUrl()))
                .apiKey(chat.getApiKey())
                .restClientBuilder(providerRestClientBuilder(builder, providerRequestCaptureInterceptor, chat))
                .build();
    }

    @Bean
    public OpenAiApi qwenEmbeddingOpenAiApi(
            RestClient.Builder builder,
            ClientHttpRequestInterceptor providerRequestCaptureInterceptor,
            AiProviderProperties properties
    ) {
        AiProviderProperties.EmbeddingProperties embedding = qwen(properties).getEmbedding();
        return OpenAiApi.builder()
                .baseUrl(normalizeOpenAiBaseUrl(embedding.getBaseUrl()))
                .apiKey(embedding.getApiKey())
                .restClientBuilder(providerRestClientBuilder(builder, providerRequestCaptureInterceptor, embedding))
                .build();
    }

    @Bean
    public OpenAiChatModel qwenChatModel(OpenAiApi qwenChatOpenAiApi, AiProviderProperties properties) {
        AiProviderProperties.ChatProperties chat = qwen(properties).getChat();
        return OpenAiChatModel.builder()
                .openAiApi(qwenChatOpenAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(chat.getModel())
                        .temperature(chat.getTemperature())
                        .maxTokens(chat.getMaxTokens())
                        .build())
                .retryTemplate(RetryUtils.DEFAULT_RETRY_TEMPLATE)
                .build();
    }

    @Bean
    public ChatClient qwenChatClient(OpenAiChatModel qwenChatModel) {
        return ChatClient.builder(qwenChatModel).build();
    }

    @Bean
    public EmbeddingModel qwenEmbeddingModel(OpenAiApi qwenEmbeddingOpenAiApi, AiProviderProperties properties) {
        AiProviderProperties.EmbeddingProperties embedding = qwen(properties).getEmbedding();
        return new OpenAiEmbeddingModel(
                qwenEmbeddingOpenAiApi,
                MetadataMode.EMBED,
                OpenAiEmbeddingOptions.builder()
                        .model(embedding.getModel())
                        .dimensions(embedding.getDimension())
                        .build(),
                RetryUtils.DEFAULT_RETRY_TEMPLATE
        );
    }

    @Bean
    public RestClient qwenRerankRestClient(
            RestClient.Builder builder,
            ClientHttpRequestInterceptor providerRequestCaptureInterceptor,
            AiProviderProperties properties
    ) {
        AiProviderProperties.RerankProperties rerank = qwen(properties).getRerank();
        RestClient.Builder clientBuilder = providerRestClientBuilder(builder, providerRequestCaptureInterceptor, rerank);
        if (StringUtils.hasText(rerank.getBaseUrl())) {
            clientBuilder.baseUrl(rerank.getBaseUrl());
        }
        return clientBuilder.build();
    }

    @Bean
    public RetryRegistry retryRegistry(AiResilienceProperties properties, ProviderErrorSupport providerErrorSupport) {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(properties.getMaxAttempts())
                .waitDuration(properties.getWaitDuration())
                .retryOnException(providerErrorSupport::isRetryable)
                .failAfterMaxAttempts(true)
                .build();
        return RetryRegistry.of(config);
    }

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry(
            AiResilienceProperties properties,
            ProviderErrorSupport providerErrorSupport
    ) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(properties.getFailureRateThreshold())
                .slidingWindowSize(properties.getSlidingWindowSize())
                .minimumNumberOfCalls(properties.getSlidingWindowSize())
                .waitDurationInOpenState(properties.getOpenStateDuration())
                .recordException(providerErrorSupport::shouldRecordForCircuitBreaker)
                .build();
        return CircuitBreakerRegistry.of(config);
    }

    private RestClient.Builder providerRestClientBuilder(
            RestClient.Builder builder,
            ClientHttpRequestInterceptor providerRequestCaptureInterceptor,
            AiProviderProperties.BaseModelProperties properties
    ) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getTimeout())
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.getTimeout());

        RestClient.Builder clientBuilder = builder.clone()
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .requestInterceptor(providerRequestCaptureInterceptor);

        if (StringUtils.hasText(properties.getApiKey())) {
            clientBuilder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey());
        }

        return clientBuilder;
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

    private AiProviderProperties.ProviderProperties qwen(AiProviderProperties properties) {
        AiProviderProperties.ProviderProperties providerProperties = properties.getProviderProperties("qwen");
        if (providerProperties == null) {
            throw new IllegalStateException("ai.provider.providers.qwen must be configured");
        }
        return providerProperties;
    }
}
