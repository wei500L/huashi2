package com.huashi.eftransfer.ai.common.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "ai.provider")
public class AiProviderProperties {

    @NotBlank
    private String provider = "qwen";

    @NotBlank
    private String fallbackProvider = "deepseek";

    @NotBlank
    private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode";

    @NotBlank
    private String apiKey = "no-key-configured";

    @NotBlank
    private String chatModel = "qwen-max";

    @NotBlank
    private String embeddingModel = "text-embedding-v4";

    @NotBlank
    private String rerankModel = "gte-rerank-v2";

    private String rerankUrl = "https://dashscope.aliyuncs.com/api/v1/services/rerank/text-rerank/text-rerank";

    private Duration connectTimeout = Duration.ofSeconds(5);

    private Duration readTimeout = Duration.ofSeconds(15);

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getFallbackProvider() {
        return fallbackProvider;
    }

    public void setFallbackProvider(String fallbackProvider) {
        this.fallbackProvider = fallbackProvider;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getChatModel() {
        return chatModel;
    }

    public void setChatModel(String chatModel) {
        this.chatModel = chatModel;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public void setEmbeddingModel(String embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public String getRerankModel() {
        return rerankModel;
    }

    public void setRerankModel(String rerankModel) {
        this.rerankModel = rerankModel;
    }

    public String getRerankUrl() {
        return rerankUrl;
    }

    public void setRerankUrl(String rerankUrl) {
        this.rerankUrl = rerankUrl;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }
}
