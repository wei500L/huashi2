package com.huashi.eftransfer.ai.modules.rag.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huashi.eftransfer.ai.common.runtime.AiRuntimeBundle;
import com.huashi.eftransfer.ai.common.runtime.AiRuntimeConfigService;
import com.huashi.eftransfer.ai.integration.provider.AiProviderRegistry;
import com.huashi.eftransfer.ai.modules.rag.config.RagProperties;
import com.huashi.eftransfer.ai.modules.rag.integration.AppServerKnowledgeClient;
import com.huashi.eftransfer.ai.modules.rag.repository.IngestionJobRepository;
import com.huashi.eftransfer.ai.modules.rag.repository.KnowledgeStoreRepository;
import com.huashi.eftransfer.ai.modules.rag.support.RagSearchFilter;
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
import com.huashi.eftransfer.shared.ai.EmbeddingBatchRequest;
import com.huashi.eftransfer.shared.ai.EmbeddingItem;
import com.huashi.eftransfer.shared.ai.EmbeddingRequest;
import com.huashi.eftransfer.shared.ai.EmbeddingResponse;
import com.huashi.eftransfer.shared.ai.ChatMessage;
import com.huashi.eftransfer.shared.ai.LexicalKnowledgeExampleItem;
import com.huashi.eftransfer.shared.ai.LexicalKnowledgeExportItem;
import com.huashi.eftransfer.shared.ai.LexicalKnowledgeExportPageResponse;
import com.huashi.eftransfer.shared.ai.LexicalKnowledgeSenseItem;
import com.huashi.eftransfer.shared.ai.RagReindexRequest;
import com.huashi.eftransfer.shared.ai.RagRetrieveRequest;
import com.huashi.eftransfer.shared.ai.RagRetrieveResponse;
import com.huashi.eftransfer.shared.ai.RerankItem;
import com.huashi.eftransfer.shared.ai.RerankRequest;
import com.huashi.eftransfer.shared.ai.RerankResponse;
import com.huashi.eftransfer.shared.ai.TokenUsage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Testcontainers
class LexicalRagFlowIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("pgvector/pgvector:pg16")
            .withDatabaseName("ef_transfer_ai_test")
            .withUsername("ef_ai")
            .withPassword("ef_ai_password");

    private static JdbcTemplate jdbcTemplate;
    private static AppServerKnowledgeClient appServerKnowledgeClient;
    private static KnowledgeIngestionService knowledgeIngestionService;
    private static RagService ragService;

    @BeforeAll
    static void beforeAll() {
        POSTGRES.start();
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl(POSTGRES.getJdbcUrl());
        dataSource.setUsername(POSTGRES.getUsername());
        dataSource.setPassword(POSTGRES.getPassword());

        new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).execute(dataSource);

        jdbcTemplate = new JdbcTemplate(dataSource);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        KnowledgeStoreRepository knowledgeStoreRepository = new KnowledgeStoreRepository(jdbcTemplate, objectMapper);
        IngestionJobRepository ingestionJobRepository = new IngestionJobRepository(jdbcTemplate, objectMapper);
        appServerKnowledgeClient = Mockito.mock(AppServerKnowledgeClient.class);

        RagProperties ragProperties = ragProperties();
        AiRuntimeConfigService runtimeConfigService = mock(AiRuntimeConfigService.class);
        when(runtimeConfigService.current()).thenReturn(runtimeBundle(ragProperties));
        AiProviderRegistry aiProviderRegistry = mock(AiProviderRegistry.class);
        when(aiProviderRegistry.embed(any())).thenAnswer(invocation -> {
            EmbeddingRequest request = invocation.getArgument(0);
            return embeddingResponse(new EmbeddingBatchRequest(List.of(request.text()), request.model(), request.dimension()));
        });
        when(aiProviderRegistry.embedBatch(any())).thenAnswer(invocation -> embeddingResponse(invocation.getArgument(0)));
        when(aiProviderRegistry.rerank(any())).thenAnswer(invocation -> rerankResponse(invocation.getArgument(0)));

        knowledgeIngestionService = new KnowledgeIngestionService(
                ingestionJobRepository,
                knowledgeStoreRepository,
                appServerKnowledgeClient,
                aiProviderRegistry,
                runtimeConfigService,
                new ConcurrentTaskExecutor(Runnable::run),
                objectMapper
        );
        KnowledgeSearchService knowledgeSearchService = new KnowledgeSearchService(aiProviderRegistry, knowledgeStoreRepository, runtimeConfigService);
        ragService = new RagService(
                runtimeConfigService,
                new RagRetrievalCapture(),
                knowledgeSearchService,
                objectMapper
        );
    }

    @AfterAll
    static void afterAll() {
        POSTGRES.stop();
    }

    @BeforeEach
    void cleanTables() {
        jdbcTemplate.execute("TRUNCATE TABLE chunk_embedding, knowledge_chunk, knowledge_document, ingestion_job RESTART IDENTITY CASCADE");
    }

    @Test
    void shouldIngestLexicalKnowledgeIntoPgvectorAndRetrieveTopContexts() {
        when(appServerKnowledgeClient.exportLexicalPairs(any(), any(), anyInt(), any()))
                .thenReturn(new LexicalKnowledgeExportPageResponse(
                        List.of(coinLexicalItem()),
                        null,
                        OffsetDateTime.now(ZoneOffset.UTC)
                ));

        knowledgeIngestionService.submitAndAwait(new RagReindexRequest(
                "FULL",
                List.of("LEXICAL_PAIR", "LEXICAL_SENSE", "LEXICAL_EXAMPLE"),
                List.of(),
                true
        ));

        assertThat(count("SELECT COUNT(*) FROM knowledge_document")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM knowledge_chunk WHERE active = TRUE")).isEqualTo(3);
        assertThat(count("SELECT COUNT(*) FROM chunk_embedding WHERE is_current = TRUE")).isEqualTo(3);
        assertThat(single("SELECT status FROM ingestion_job ORDER BY id DESC LIMIT 1")).isEqualTo("SUCCEEDED");

        RagRetrieveResponse response = ragService.retrieve(new RagRetrieveRequest(
                "Why is coin and coin easy to confuse?",
                List.of("LEXICAL_PAIR", "LEXICAL_SENSE", "LEXICAL_EXAMPLE"),
                List.of(),
                null,
                List.of()
        ));

        assertThat(response.grounded()).isTrue();
        assertThat(response.citations()).hasSize(3);
        assertThat(response.contextChunks()).hasSize(3);
        assertThat(response.citations().get(0).sourceType()).isEqualTo("LEXICAL_EXAMPLE");
        assertThat(response.citations().get(1).sourceType()).isEqualTo("LEXICAL_SENSE");
        assertThat(response.citations().get(2).sourceType()).isEqualTo("LEXICAL_PAIR");
        assertThat(response.contextChunks().get(0).content()).contains("I found a coin on the floor");
    }

    @Test
    void shouldUseConversationHistoryToDisambiguateFollowUpRetrieval() {
        when(appServerKnowledgeClient.exportLexicalPairs(any(), any(), anyInt(), any()))
                .thenReturn(new LexicalKnowledgeExportPageResponse(
                        List.of(coinLexicalItem()),
                        null,
                        OffsetDateTime.now(ZoneOffset.UTC)
                ));

        knowledgeIngestionService.submitAndAwait(new RagReindexRequest(
                "FULL",
                List.of("LEXICAL_PAIR", "LEXICAL_SENSE", "LEXICAL_EXAMPLE"),
                List.of(),
                true
        ));

        RagRetrieveResponse response = ragService.retrieve(new RagRetrieveRequest(
                "why is it confusing?",
                List.of("LEXICAL_PAIR", "LEXICAL_SENSE", "LEXICAL_EXAMPLE"),
                List.of(),
                "conv-1",
                List.of(
                        new ChatMessage("user", "Tell me about coin / coin."),
                        new ChatMessage("assistant", "It is a false friend pair."),
                        new ChatMessage("user", "What is the difference between them?")
                )
        ));

        assertThat(response.grounded()).isTrue();
        assertThat(response.citations()).isNotEmpty();
    }

    @Test
    void shouldMarkJobFailedAndThrowWhenSynchronousReindexFails() {
        when(appServerKnowledgeClient.exportLexicalPairs(any(), any(), anyInt(), any()))
                .thenThrow(new IllegalStateException("app-server export failed"));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> knowledgeIngestionService.submitAndAwait(new RagReindexRequest(
                "FULL",
                List.of("LEXICAL_PAIR", "LEXICAL_SENSE", "LEXICAL_EXAMPLE"),
                List.of("1001"),
                false
        )))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app-server export failed");

        assertThat(single("SELECT status FROM ingestion_job ORDER BY id DESC LIMIT 1")).isEqualTo("FAILED");
    }

    private static RagProperties ragProperties() {
        RagProperties properties = new RagProperties();
        properties.getAppServer().setBaseUrl("http://localhost:8080");
        properties.getAppServer().setInternalToken("test-internal-token");
        properties.getIngestion().setEmbeddingBatchSize(8);
        properties.getRetrieval().setRecallTopK(8);
        properties.getRetrieval().setRecallThreshold(0.0d);
        properties.getRetrieval().setRerankTopN(3);
        properties.getRetrieval().setRerankThreshold(0.0d);
        properties.getRetrieval().setFinalTopK(3);
        return properties;
    }

    private static AiRuntimeBundle runtimeBundle(RagProperties ragProperties) {
        return new AiRuntimeBundle(
                new AiOpsConfigPayload(
                        new AiOpsProviderConfig(
                                "qwen",
                                "deepseek",
                                Map.of(
                                        "qwen",
                                        new AiOpsProviderDefinition(
                                                new AiOpsChatConfig(AiOpsProtocols.OPENAI_COMPAT, "https://example.com/v1", "test-api-key", "qwen-max", "PT3S", "PT30S", 0.2d, 1024),
                                                new AiOpsEmbeddingConfig(AiOpsProtocols.OPENAI_COMPAT, "https://example.com/v1", "test-api-key", "text-embedding-v4", "PT3S", "PT30S", 1024),
                                                new AiOpsRerankConfig(AiOpsProtocols.OPENAI_RERANK, "https://example.com", "test-api-key", "gte-rerank-v2", "PT3S", "PT30S")
                                        ),
                                        "deepseek",
                                        new AiOpsProviderDefinition(
                                                new AiOpsChatConfig(AiOpsProtocols.OPENAI_COMPAT, "https://example.com/v1", "backup-api-key", "deepseek-chat", "PT3S", "PT30S", 0.2d, 1024),
                                                new AiOpsEmbeddingConfig(AiOpsProtocols.OPENAI_COMPAT, "https://example.com/v1", "backup-api-key", "text-embedding-v4", "PT3S", "PT30S", 1024),
                                                new AiOpsRerankConfig(AiOpsProtocols.OPENAI_RERANK, "https://example.com", "backup-api-key", "gte-rerank-v2", "PT3S", "PT30S")
                                        )
                                )
                        ),
                        new AiOpsResilienceConfig(1, "PT0.1S", 50.0f, 10, "PT5S"),
                        new AiOpsRagConfig(
                                new AiOpsRagAppServerConfig(
                                        ragProperties.getAppServer().getBaseUrl(),
                                        ragProperties.getAppServer().getInternalToken(),
                                        "PT3S",
                                        "PT5S"
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
                                        ragProperties.getRetrieval().getFinalTopK(),
                                        ragProperties.getRetrieval().getHnswEfSearch()
                                )
                        )
                ),
                Map.of(),
                null,
                "TEST",
                1L,
                OffsetDateTime.now(ZoneOffset.UTC)
        );
    }

    private int count(String sql) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class);
        return value == null ? 0 : value;
    }

    private String single(String sql) {
        return jdbcTemplate.queryForObject(sql, String.class);
    }

    private static LexicalKnowledgeExportItem coinLexicalItem() {
        return new LexicalKnowledgeExportItem(
                1001L,
                OffsetDateTime.parse("2026-03-20T00:00:00Z"),
                true,
                "READY",
                "PENDING",
                "coin",
                "coin",
                "硬币；角落",
                "FALSE_FRIEND",
                0.12d,
                0.92d,
                "HIGH",
                4,
                "High confusion for beginners",
                "Teacher Curated",
                List.of("false-friend", "high-frequency"),
                List.of(new LexicalKnowledgeSenseItem(
                        2001L,
                        1,
                        "a piece of money",
                        "une pièce de monnaie",
                        "硬币",
                        List.of(new LexicalKnowledgeExampleItem(
                                3001L,
                                1,
                                "I found a coin on the floor.",
                                "J'ai trouvé une pièce par terre.",
                                "我在地上捡到一枚硬币。",
                                "HIGH",
                                "Teacher Curated"
                        ))
                ))
        );
    }

    private static EmbeddingResponse embeddingResponse(EmbeddingBatchRequest request) {
        List<EmbeddingItem> items = request.texts().stream()
                .map(LexicalRagFlowIntegrationTest::embeddingForText)
                .map(embedding -> new EmbeddingItem(0, null, embedding))
                .toList();
        List<EmbeddingItem> indexedItems = java.util.stream.IntStream.range(0, request.texts().size())
                .mapToObj(index -> new EmbeddingItem(index, request.texts().get(index), items.get(index).embedding()))
                .toList();
        int dimension = indexedItems.isEmpty() ? 1024 : indexedItems.get(0).embedding().size();
        return new EmbeddingResponse(
                "stub",
                "stub-embedding-model",
                dimension,
                "stub-embedding-request",
                new TokenUsage(request.texts().size(), 0, request.texts().size()),
                indexedItems
        );
    }

    private static RerankResponse rerankResponse(RerankRequest request) {
        List<RerankItem> items = java.util.stream.IntStream.range(0, request.documents().size())
                .mapToObj(index -> new RerankItem(index, rerankScore(request.documents().get(index)), request.documents().get(index)))
                .sorted(Comparator.comparing(RerankItem::relevanceScore).reversed())
                .limit(request.topN() == null ? request.documents().size() : request.topN())
                .toList();
        return new RerankResponse("stub", "stub-rerank-model", "stub-rerank-request", 9, items);
    }

    private static List<Double> embeddingForText(String text) {
        String normalized = text == null ? "" : text.toLowerCase(Locale.ROOT);
        if (normalized.contains("coin")) {
            return vector(1);
        }
        if (normalized.contains("table")) {
            return vector(2);
        }
        return vector(3);
    }

    private static double rerankScore(String document) {
        if (document.contains("Example")) {
            return 0.95d;
        }
        if (document.contains("Sense")) {
            return 0.90d;
        }
        return 0.85d;
    }

    private static List<Double> vector(int axis) {
        Double[] values = new Double[1024];
        java.util.Arrays.fill(values, 0.0d);
        values[Math.max(0, Math.min(1023, axis - 1))] = 1.0d;
        return List.of(values);
    }
}
