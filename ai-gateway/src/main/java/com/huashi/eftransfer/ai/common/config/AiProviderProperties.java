package com.huashi.eftransfer.ai.common.config;

import jakarta.validation.Valid;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "ai.provider")
public class AiProviderProperties {

    private String activeProvider = "qwen";

    private String fallbackProvider = "deepseek";

    @Valid
    private Map<String, ProviderProperties> providers = new LinkedHashMap<>();

    public String getActiveProvider() {
        return activeProvider;
    }

    public void setActiveProvider(String activeProvider) {
        this.activeProvider = activeProvider;
    }

    public String getFallbackProvider() {
        return fallbackProvider;
    }

    public void setFallbackProvider(String fallbackProvider) {
        this.fallbackProvider = fallbackProvider;
    }

    public Map<String, ProviderProperties> getProviders() {
        return providers;
    }

    public void setProviders(Map<String, ProviderProperties> providers) {
        this.providers = providers;
    }

    public ProviderProperties getProviderProperties(String providerName) {
        return providers.get(providerName);
    }

    public ProviderProperties getActiveProviderProperties() {
        return getProviderProperties(activeProvider);
    }

    public static class ProviderProperties {

        @Valid
        private ChatProperties chat = new ChatProperties();

        @Valid
        private EmbeddingProperties embedding = new EmbeddingProperties();

        @Valid
        private RerankProperties rerank = new RerankProperties();

        public ChatProperties getChat() {
            return chat;
        }

        public void setChat(ChatProperties chat) {
            this.chat = chat;
        }

        public EmbeddingProperties getEmbedding() {
            return embedding;
        }

        public void setEmbedding(EmbeddingProperties embedding) {
            this.embedding = embedding;
        }

        public RerankProperties getRerank() {
            return rerank;
        }

        public void setRerank(RerankProperties rerank) {
            this.rerank = rerank;
        }
    }

    public static class BaseModelProperties {

        private String protocol = "openai-compat";

        private String baseUrl;

        private String apiKey;

        private String model;

        private Duration connectTimeout = Duration.ofSeconds(3);

        private Duration readTimeout = Duration.ofSeconds(30);

        public String getProtocol() {
            return protocol;
        }

        public void setProtocol(String protocol) {
            this.protocol = protocol;
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

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
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

    public static class ChatProperties extends BaseModelProperties {

        private Double temperature = 0.2D;

        private Integer maxTokens = 2048;

        public Double getTemperature() {
            return temperature;
        }

        public void setTemperature(Double temperature) {
            this.temperature = temperature;
        }

        public Integer getMaxTokens() {
            return maxTokens;
        }

        public void setMaxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
        }
    }

    public static class EmbeddingProperties extends BaseModelProperties {

        private Integer dimension = 1024;

        public Integer getDimension() {
            return dimension;
        }

        public void setDimension(Integer dimension) {
            this.dimension = dimension;
        }
    }

    public static class RerankProperties extends BaseModelProperties {
        public RerankProperties() {
            setProtocol("qwen-rerank");
        }
    }
}
