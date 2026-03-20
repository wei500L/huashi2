package com.huashi.eftransfer.ai.modules.health.service;

import com.huashi.eftransfer.ai.common.runtime.AiRuntimeBundle;
import com.huashi.eftransfer.ai.common.runtime.AiRuntimeConfigService;
import com.huashi.eftransfer.ai.modules.rag.repository.IngestionJobRepository;
import com.huashi.eftransfer.ai.modules.rag.repository.KnowledgeStoreRepository;
import com.huashi.eftransfer.ai.modules.health.dto.AiHealthPayload;
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
    private final AiRuntimeConfigService runtimeConfigService;
    private final KnowledgeStoreRepository knowledgeStoreRepository;
    private final IngestionJobRepository ingestionJobRepository;

    public AiHealthService(
            JdbcTemplate jdbcTemplate,
            Environment environment,
            AiRuntimeConfigService runtimeConfigService,
            KnowledgeStoreRepository knowledgeStoreRepository,
            IngestionJobRepository ingestionJobRepository
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.environment = environment;
        this.runtimeConfigService = runtimeConfigService;
        this.knowledgeStoreRepository = knowledgeStoreRepository;
        this.ingestionJobRepository = ingestionJobRepository;
    }

    public AiHealthPayload getHealthPayload() {
        AiRuntimeBundle bundle = runtimeConfigService.current();
        var provider = bundle.config().provider();
        boolean databaseReady = isDatabaseReady();
        String vectorVersion = fetchVectorExtensionVersion();
        boolean vectorStoreReady = !"UNAVAILABLE".equals(vectorVersion)
                && knowledgeStoreRepository.hasKnowledgeDocuments()
                && ingestionJobRepository.findLatestSuccessfulJob("KNOWLEDGE_REINDEX") != null;
        boolean providerReady = configured(provider.chat().baseUrl())
                && configured(provider.chat().apiKey())
                && configured(provider.chat().model())
                && configured(provider.embedding().baseUrl())
                && configured(provider.embedding().apiKey())
                && configured(provider.embedding().model());
        boolean rerankReady = configured(provider.rerank().baseUrl())
                && configured(provider.rerank().apiKey())
                && configured(provider.rerank().model());
        List<String> profiles = Arrays.asList(environment.getActiveProfiles());

        return new AiHealthPayload(
                "ai-gateway",
                databaseReady && providerReady && rerankReady ? "UP" : "DEGRADED",
                provider.activeProvider(),
                provider.fallbackProvider(),
                provider.chat().model(),
                provider.embedding().model(),
                provider.rerank().model(),
                databaseReady,
                vectorStoreReady,
                providerReady,
                rerankReady,
                vectorVersion,
                profiles.isEmpty() ? List.of("default") : profiles,
                OffsetDateTime.now()
        );
    }

    private boolean configured(String value) {
        return value != null && !value.isBlank();
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
