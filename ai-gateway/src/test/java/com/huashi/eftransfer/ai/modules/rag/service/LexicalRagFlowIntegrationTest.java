package com.huashi.eftransfer.ai.modules.rag.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huashi.eftransfer.ai.integration.provider.AiProviderFacade;
import com.huashi.eftransfer.ai.integration.provider.AiProviderRegistry;
import com.huashi.eftransfer.ai.modules.rag.config.RagProperties;
import com.huashi.eftransfer.ai.modules.rag.integration.AppServerKnowledgeClient;
import com.huashi.eftransfer.ai.modules.rag.repository.IngestionJobRepository;
import com.huashi.eftransfer.ai.modules.rag.repository.KnowledgeStoreRepository;
import com.huashi.eftransfer.ai.modules.rag.support.RagSearchFilter;
import com.huashi.eftransfer.shared.ai.ChatRequest;
import com.huashi.eftransfer.shared.ai.ChatResponse;
import com.huashi.eftransfer.shared.ai.EmbeddingBatchRequest;
import com.huashi.eftransfer.shared.ai.EmbeddingItem;
import com.huashi.eftransfer.shared.ai.EmbeddingRequest;
import com.huashi.eftransfer.shared.ai.EmbeddingResponse;
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
import com.huashi.eftransfer.shared.ai.StructuredChatRequest;
import com.huashi.eftransfer.shared.ai.StructuredChatResponse;
import com.huashi.eftransfer.shared.ai.TokenUsage;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
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

        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();

        jdbcTemplate = new JdbcTemplate(dataSource);
        ObjectMapper objectMapper = new ObjectMapper();
        KnowledgeStoreRepository knowledgeStoreRepository = new KnowledgeStoreRepository(jdbcTemplate, objectMapper);
        IngestionJobRepository ingestionJobRepository = new IngestionJobRepository(jdbcTemplate, objectMapper);
        appServerKnowledgeClient = Mockito.mock(AppServerKnowledgeClient.class);

        RagProperties ragProperties = ragProperties();
        StubAiProviderFacade stubProvider = new StubAiProviderFacade();
        com.huashi.eftransfer.ai.common.config.AiProviderProperties aiProviderProperties =
                new com.huashi.eftransfer.ai.common.config.AiProviderProperties();
        aiProviderProperties.setActiveProvider("qwen");
        AiProviderRegistry aiProviderRegistry = new AiProviderRegistry(aiProviderProperties, List.of(stubProvider));

        knowledgeIngestionService = new KnowledgeIngestionService(
                ingestionJobRepository,
                knowledgeStoreRepository,
                appServerKnowledgeClient,
                aiProviderRegistry,
                ragProperties,
                new ConcurrentTaskExecutor(Runnable::run),
                objectMapper
        );
        KnowledgeSearchService knowledgeSearchService = new KnowledgeSearchService(aiProviderRegistry, knowledgeStoreRepository, ragProperties);
        ragService = new RagService(
                Mockito.mock(ChatClient.class),
                new RagRetrievalCapture(),
                knowledgeSearchService,
                ragProperties,
                objectMapper
        );
    }

    @AfterAll
    static void afterAll() {
        POSTGRES.stop();
    }

    @BeforeEach
    void cleanTables() {
        jdbcTemplate.execute("TRUNCATE TABLE chunk_embedding, knowledge_chunk, knowledge_document, ingestion_job, rag_knowledge_document RESTART IDENTITY CASCADE");
    }

    @Test
    void shouldIngestLexicalKnowledgeIntoPgvectorAndRetrieveTopContexts() {
        when(appServerKnowledgeClient.exportLexicalPairs(any(), any(), anyInt(), any()))
                .thenReturn(new LexicalKnowledgeExportPageResponse(
                        List.of(coinLexicalItem()),
                        null,
                        OffsetDateTime.now(ZoneOffset.UTC)
                ));

        knowledgeIngestionService.submit(new RagReindexRequest(
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

    private static RagProperties ragProperties() {
        RagProperties properties = new RagProperties();
        properties.getIngestion().setEmbeddingBatchSize(8);
        properties.getRetrieval().setRecallTopK(8);
        properties.getRetrieval().setRecallThreshold(0.0d);
        properties.getRetrieval().setRerankTopN(3);
        properties.getRetrieval().setRerankThreshold(0.0d);
        properties.getRetrieval().setFinalTopK(3);
        return properties;
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

    private static class StubAiProviderFacade implements AiProviderFacade {

        @Override
        public String providerName() {
            return "qwen";
        }

        @Override
        public ChatResponse chat(ChatRequest request) {
            throw new UnsupportedOperationException("chat is not used in this test");
        }

        @Override
        public StructuredChatResponse structuredChat(StructuredChatRequest request) {
            throw new UnsupportedOperationException("structuredChat is not used in this test");
        }

        @Override
        public EmbeddingResponse embed(EmbeddingRequest request) {
            return embedBatch(new EmbeddingBatchRequest(List.of(request.text()), request.model(), request.dimension()));
        }

        @Override
        public EmbeddingResponse embedBatch(EmbeddingBatchRequest request) {
            List<EmbeddingItem> items = request.texts().stream()
                    .map(this::embeddingForText)
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

        @Override
        public RerankResponse rerank(RerankRequest request) {
            List<RerankItem> items = java.util.stream.IntStream.range(0, request.documents().size())
                    .mapToObj(index -> new RerankItem(index, rerankScore(request.documents().get(index)), request.documents().get(index)))
                    .sorted(Comparator.comparing(RerankItem::relevanceScore).reversed())
                    .limit(request.topN() == null ? request.documents().size() : request.topN())
                    .toList();
            return new RerankResponse("stub", "stub-rerank-model", "stub-rerank-request", 9, items);
        }

        private List<Double> embeddingForText(String text) {
            String normalized = text == null ? "" : text.toLowerCase(Locale.ROOT);
            if (normalized.contains("coin")) {
                return vector(1);
            }
            if (normalized.contains("table")) {
                return vector(2);
            }
            return vector(3);
        }

        private double rerankScore(String document) {
            if (document.contains("Example")) {
                return 0.95d;
            }
            if (document.contains("Sense")) {
                return 0.90d;
            }
            return 0.85d;
        }

        private List<Double> vector(int axis) {
            Double[] values = new Double[1024];
            java.util.Arrays.fill(values, 0.0d);
            values[Math.max(0, Math.min(1023, axis - 1))] = 1.0d;
            return List.of(values);
        }
    }
}
