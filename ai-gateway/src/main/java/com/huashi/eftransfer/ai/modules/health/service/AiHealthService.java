package com.huashi.eftransfer.ai.modules.health.service;

import com.huashi.eftransfer.ai.common.runtime.AiRuntimeBundle;
import com.huashi.eftransfer.ai.common.runtime.AiRuntimeConfigService;
import com.huashi.eftransfer.ai.modules.rag.repository.KnowledgeStoreRepository;
import com.huashi.eftransfer.ai.modules.internal.service.AiConfigProbeService;
import com.huashi.eftransfer.shared.ai.AiGatewayHealthResponse;
import com.huashi.eftransfer.shared.ai.config.AiOpsProviderDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class AiHealthService {

    private static final Logger log = LoggerFactory.getLogger(AiHealthService.class);

    private final JdbcTemplate jdbcTemplate;
    private final Environment environment;
    private final AiRuntimeConfigService runtimeConfigService;
    private final KnowledgeStoreRepository knowledgeStoreRepository;
    private final AiConfigProbeService aiConfigProbeService;

    public AiHealthService(
            JdbcTemplate jdbcTemplate,
            Environment environment,
            AiRuntimeConfigService runtimeConfigService,
            KnowledgeStoreRepository knowledgeStoreRepository,
            AiConfigProbeService aiConfigProbeService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.environment = environment;
        this.runtimeConfigService = runtimeConfigService;
        this.knowledgeStoreRepository = knowledgeStoreRepository;
        this.aiConfigProbeService = aiConfigProbeService;
    }

    public AiGatewayHealthResponse getHealthPayload() {
        AiRuntimeBundle bundle = runtimeConfigService.current();
        var provider = bundle.config().provider();
        AiOpsProviderDefinition activeProvider = provider.providers().get(provider.activeProvider());
        AiOpsProviderDefinition fallbackProvider = provider.providers().get(provider.fallbackProvider());
        String storedSyncStatus = runtimeConfigService.storedSyncStatus();
        boolean databaseReady = isDatabaseReady();
        String vectorVersion = fetchVectorExtensionVersion();
        KnowledgeStoreRepository.KnowledgeIndexCoverage indexCoverage =
                new KnowledgeStoreRepository.KnowledgeIndexCoverage(0, 0);
        boolean hasKnowledgeDocuments = false;
        if (databaseReady && activeProvider != null && activeProvider.embedding() != null) {
            try {
                indexCoverage = knowledgeStoreRepository.getIndexCoverage(
                        activeProvider.embedding().model(),
                        activeProvider.embedding().dimension()
                );
                hasKnowledgeDocuments = knowledgeStoreRepository.hasKnowledgeDocuments();
            } catch (RuntimeException exception) {
                log.warn("event=ai_health_vector_store_probe_failed message={}", exception.getMessage());
            }
        }
        boolean vectorStoreReady = databaseReady
                && !"UNAVAILABLE".equals(vectorVersion)
                && hasKnowledgeDocuments
                && indexCoverage.isComplete();
        boolean providerConfigured = activeProvider != null
                && activeProvider.chat() != null
                && activeProvider.embedding() != null
                && configured(activeProvider.chat().baseUrl())
                && configured(activeProvider.chat().apiKey())
                && configured(activeProvider.chat().model())
                && configured(activeProvider.embedding().baseUrl())
                && configured(activeProvider.embedding().apiKey())
                && configured(activeProvider.embedding().model());
        boolean rerankConfigured = activeProvider != null
                && activeProvider.rerank() != null
                && configured(activeProvider.rerank().baseUrl())
                && configured(activeProvider.rerank().apiKey())
                && configured(activeProvider.rerank().model());
        boolean fallbackEmbeddingConfigured = fallbackProvider != null
                && fallbackProvider.embedding() != null
                && configured(fallbackProvider.embedding().baseUrl())
                && configured(fallbackProvider.embedding().apiKey())
                && configured(fallbackProvider.embedding().model());
        boolean fallbackRerankConfigured = fallbackProvider != null
                && fallbackProvider.rerank() != null
                && configured(fallbackProvider.rerank().baseUrl())
                && configured(fallbackProvider.rerank().apiKey())
                && configured(fallbackProvider.rerank().model());
        boolean providerReady = providerConfigured
                && fallbackEmbeddingConfigured
                && aiConfigProbeService.isEmbeddingReady(provider.activeProvider(), activeProvider.embedding())
                && aiConfigProbeService.isEmbeddingReady(provider.fallbackProvider(), fallbackProvider.embedding());
        boolean rerankReady = rerankConfigured
                && fallbackRerankConfigured
                && aiConfigProbeService.isRerankReady(provider.activeProvider(), activeProvider.rerank())
                && aiConfigProbeService.isRerankReady(provider.fallbackProvider(), fallbackProvider.rerank());
        AppServerProbe appServerProbe = probeAppServer(bundle);
        List<String> profiles = Arrays.asList(environment.getActiveProfiles());
        boolean storedSyncHealthy = !AiRuntimeConfigService.STORED_SYNC_STATUS_SYNC_FAILED.equals(storedSyncStatus);
        boolean overallHealthy = databaseReady
                && vectorStoreReady
                && providerReady
                && rerankReady
                && appServerProbe.ready()
                && storedSyncHealthy;

        return new AiGatewayHealthResponse(
                "ai-gateway",
                overallHealthy ? "UP" : "DEGRADED",
                storedSyncStatus,
                provider.activeProvider(),
                provider.fallbackProvider(),
                activeProvider == null || activeProvider.chat() == null ? null : activeProvider.chat().model(),
                activeProvider == null || activeProvider.embedding() == null ? null : activeProvider.embedding().model(),
                activeProvider == null || activeProvider.rerank() == null ? null : activeProvider.rerank().model(),
                databaseReady,
                vectorStoreReady,
                providerReady,
                rerankReady,
                appServerProbe.ready(),
                vectorVersion,
                profiles.isEmpty() ? List.of("default") : profiles,
                OffsetDateTime.now(),
                appServerProbe.error()
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

    private AppServerProbe probeAppServer(AiRuntimeBundle bundle) {
        try {
            bundle.appServerRestClient().get()
                    .uri("/internal/ops/ai-config")
                    .retrieve()
                    .toBodilessEntity();
            return new AppServerProbe(true, null);
        } catch (RestClientResponseException ex) {
            return new AppServerProbe(false, "HTTP " + ex.getStatusCode().value() + " " + ex.getStatusText());
        } catch (RestClientException ex) {
            return new AppServerProbe(false, ex.getMessage());
        } catch (Exception ex) {
            return new AppServerProbe(false, ex.getMessage());
        }
    }

    private record AppServerProbe(
            boolean ready,
            String error
    ) {
    }
}
