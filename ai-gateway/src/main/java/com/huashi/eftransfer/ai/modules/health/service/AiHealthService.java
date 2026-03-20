package com.huashi.eftransfer.ai.modules.health.service;

import com.huashi.eftransfer.ai.common.config.AiProviderProperties;
import com.huashi.eftransfer.ai.modules.rag.repository.IngestionJobRepository;
import com.huashi.eftransfer.ai.modules.rag.repository.KnowledgeStoreRepository;
import com.huashi.eftransfer.ai.modules.health.dto.AiHealthPayload;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class AiHealthService {

    private final JdbcTemplate jdbcTemplate;
    private final Environment environment;
    private final AiProviderProperties providerProperties;
    private final KnowledgeStoreRepository knowledgeStoreRepository;
    private final IngestionJobRepository ingestionJobRepository;
    private final ObjectProvider<ChatModel> chatModelProvider;
    private final ObjectProvider<EmbeddingModel> embeddingModelProvider;
    private final ObjectProvider<VectorStore> vectorStoreProvider;

    public AiHealthService(
            JdbcTemplate jdbcTemplate,
            Environment environment,
            AiProviderProperties providerProperties,
            KnowledgeStoreRepository knowledgeStoreRepository,
            IngestionJobRepository ingestionJobRepository,
            ObjectProvider<ChatModel> chatModelProvider,
            ObjectProvider<EmbeddingModel> embeddingModelProvider,
            ObjectProvider<VectorStore> vectorStoreProvider
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.environment = environment;
        this.providerProperties = providerProperties;
        this.knowledgeStoreRepository = knowledgeStoreRepository;
        this.ingestionJobRepository = ingestionJobRepository;
        this.chatModelProvider = chatModelProvider;
        this.embeddingModelProvider = embeddingModelProvider;
        this.vectorStoreProvider = vectorStoreProvider;
    }

    public AiHealthPayload getHealthPayload() {
        AiProviderProperties.ProviderProperties activeProvider = providerProperties.getActiveProviderProperties();
        boolean databaseReady = isDatabaseReady();
        String vectorVersion = fetchVectorExtensionVersion();
        boolean vectorStoreReady = vectorStoreProvider.getIfAvailable() != null
                && !"UNAVAILABLE".equals(vectorVersion)
                && knowledgeStoreRepository.hasKnowledgeDocuments()
                && ingestionJobRepository.findLatestSuccessfulJob("KNOWLEDGE_REINDEX") != null;
        boolean providerReady = chatModelProvider.getIfAvailable() != null
                && embeddingModelProvider.getIfAvailable() != null
                && activeProvider != null;
        boolean rerankReady = activeProvider != null
                && activeProvider.getRerank() != null
                && activeProvider.getRerank().getBaseUrl() != null
                && !activeProvider.getRerank().getBaseUrl().isBlank()
                && activeProvider.getRerank().getApiKey() != null
                && !activeProvider.getRerank().getApiKey().isBlank()
                && activeProvider.getRerank().getModel() != null
                && !activeProvider.getRerank().getModel().isBlank();
        List<String> profiles = Arrays.asList(environment.getActiveProfiles());

        return new AiHealthPayload(
                "ai-gateway",
                databaseReady && providerReady && rerankReady ? "UP" : "DEGRADED",
                providerProperties.getActiveProvider(),
                providerProperties.getFallbackProvider(),
                activeProvider != null ? activeProvider.getChat().getModel() : null,
                activeProvider != null ? activeProvider.getEmbedding().getModel() : null,
                activeProvider != null ? activeProvider.getRerank().getModel() : null,
                databaseReady,
                vectorStoreReady,
                providerReady,
                rerankReady,
                vectorVersion,
                profiles.isEmpty() ? List.of("default") : profiles,
                OffsetDateTime.now()
        );
    }

    private boolean isDatabaseReady() {
        try {
            Integer value = jdbcTemplate.queryForObject("select 1", Integer.class);
            return value != null && value == 1;
        } catch (Exception ex) {
            return false;
        }
    }

    private String fetchVectorExtensionVersion() {
        try {
            return jdbcTemplate.queryForObject(
                    "select extversion from pg_extension where extname = 'vector'",
                    String.class
            );
        } catch (Exception ex) {
            return "UNAVAILABLE";
        }
    }
}
