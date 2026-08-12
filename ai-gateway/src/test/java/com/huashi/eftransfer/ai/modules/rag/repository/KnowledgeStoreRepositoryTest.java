package com.huashi.eftransfer.ai.modules.rag.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huashi.eftransfer.ai.common.runtime.AiRuntimeBundle;
import com.huashi.eftransfer.ai.common.runtime.AiRuntimeConfigService;
import com.huashi.eftransfer.ai.integration.provider.AiProviderRegistry;
import com.huashi.eftransfer.ai.modules.rag.config.RagProperties;
import com.huashi.eftransfer.ai.modules.rag.service.KnowledgeSearchService;
import com.huashi.eftransfer.ai.modules.rag.service.RetrievalQueryPlanner;
import com.huashi.eftransfer.ai.modules.rag.support.KnowledgeChunkPayload;
import com.huashi.eftransfer.ai.modules.rag.support.KnowledgeDocumentPayload;
import com.huashi.eftransfer.ai.modules.rag.support.KnowledgeSearchCandidate;
import com.huashi.eftransfer.ai.modules.rag.support.KnowledgeSourceTypes;
import com.huashi.eftransfer.ai.modules.rag.support.RagRetrievalResult;
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
import com.huashi.eftransfer.shared.ai.EmbeddingItem;
import com.huashi.eftransfer.shared.ai.EmbeddingResponse;
import com.huashi.eftransfer.shared.ai.RerankItem;
import com.huashi.eftransfer.shared.ai.RerankResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Testcontainers
class KnowledgeStoreRepositoryTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("pgvector/pgvector:pg16")
            .withDatabaseName("ef_transfer_ai_test")
            .withUsername("ef_ai")
            .withPassword("ef_ai_password");

    private static JdbcTemplate jdbcTemplate;
    private static KnowledgeStoreRepository knowledgeStoreRepository;

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
        knowledgeStoreRepository = new KnowledgeStoreRepository(
                jdbcTemplate,
                new ObjectMapper().findAndRegisterModules()
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
    void shouldTuneChunkEmbeddingHnswIndex() {
        String reloptions = jdbcTemplate.queryForObject("""
                SELECT array_to_string(reloptions, ',')
                FROM pg_class
                WHERE relname = 'idx_chunk_embedding_vector_hnsw'
                """, String.class);

        assertThat(reloptions).contains("m=16");
        assertThat(reloptions).contains("ef_construction=128");
    }

    @Test
    void shouldWriteEmbeddingsAndSkipUnchangedChunks() {
        KnowledgeDocumentPayload payload = sampleDocument("1001", true);

        KnowledgeStoreRepository.UpsertDocumentResult firstUpsert = knowledgeStoreRepository.upsertDocument(payload, false, "doc-hash-1");
        assertThat(firstUpsert.pendingChunkEmbeddings()).hasSize(2);

        for (int index = 0; index < firstUpsert.pendingChunkEmbeddings().size(); index++) {
            knowledgeStoreRepository.replaceChunkEmbedding(
                    firstUpsert.pendingChunkEmbeddings().get(index).chunkId(),
                    "test-embedding-model",
                    1024,
                    firstUpsert.pendingChunkEmbeddings().get(index).contentHash(),
                    vector(index + 1)
            );
        }

        KnowledgeStoreRepository.UpsertDocumentResult secondUpsert = knowledgeStoreRepository.upsertDocument(payload, false, "doc-hash-1");
        assertThat(secondUpsert.pendingChunkEmbeddings()).isEmpty();

        List<?> rows = jdbcTemplate.queryForList("SELECT id FROM chunk_embedding WHERE is_current = TRUE");
        assertThat(rows).hasSize(2);
    }

    @Test
    void shouldRetrieveAndRerankKnowledgeChunks() {
        seedChunk("pair:1001", "1001", KnowledgeSourceTypes.LEXICAL_PAIR, "coin / coin", "False friend pair guidance", vector(1));
        seedChunk("sense:2001", "2001", KnowledgeSourceTypes.LEXICAL_SENSE, "coin / coin - Sense 1", "Money sense definition", vector(2));
        seedChunk("error:false_friend_confusion", "false_friend_confusion", KnowledgeSourceTypes.ERROR_TYPE, "False Friend Confusion", "This explains false friend confusion in detail.", vector(3));

        AiProviderRegistry providerRegistry = mock(AiProviderRegistry.class);
        when(providerRegistry.embed(any())).thenReturn(new EmbeddingResponse(
                "qwen",
                "test-embedding-model",
                1024,
                "embed-test",
                null,
                List.of(new EmbeddingItem(0, "query", vector(1)))
        ));
        when(providerRegistry.rerank(any())).thenReturn(new RerankResponse(
                "qwen",
                "gte-rerank-v2",
                "rerank-test",
                12,
                List.of(
                        new RerankItem(2, 0.92, "error-doc"),
                        new RerankItem(0, 0.83, "pair-doc"),
                        new RerankItem(1, 0.10, "sense-doc")
                )
        ));

        RagProperties ragProperties = new RagProperties();
        ragProperties.getRetrieval().setRecallTopK(10);
        ragProperties.getRetrieval().setRecallThreshold(0.0d);
        ragProperties.getRetrieval().setRerankTopN(5);
        ragProperties.getRetrieval().setRerankThreshold(0.2d);
        ragProperties.getRetrieval().setFinalTopK(3);
        ragProperties.getAppServer().setBaseUrl("http://localhost:8080");
        ragProperties.getAppServer().setInternalToken("test-internal-token");

        AiRuntimeConfigService runtimeConfigService = mock(AiRuntimeConfigService.class);
        when(runtimeConfigService.current()).thenReturn(runtimeBundle(ragProperties));

        KnowledgeSearchService knowledgeSearchService = new KnowledgeSearchService(
                providerRegistry,
                knowledgeStoreRepository,
                runtimeConfigService,
                new RetrievalQueryPlanner(providerRegistry)
        );
        RagRetrievalResult result = knowledgeSearchService.search("Why is coin/coin risky?", RagSearchFilter.empty());

        assertThat(result.chunks()).hasSize(2);
        assertThat(result.chunks().get(0).sourceType()).isEqualTo(KnowledgeSourceTypes.ERROR_TYPE);
        assertThat(result.chunks().get(1).sourceType()).isEqualTo(KnowledgeSourceTypes.LEXICAL_PAIR);
        assertThat(result.chunks().get(0).citationId()).isEqualTo("C1");
        assertThat(result.documents()).hasSize(2);
    }

    @Test
    void shouldFallbackToSimilarityOrderWhenRerankFails() {
        seedChunk("pair:1001", "1001", KnowledgeSourceTypes.LEXICAL_PAIR, "coin / coin", "False friend pair guidance", vector(1));
        seedChunk("sense:2001", "2001", KnowledgeSourceTypes.LEXICAL_SENSE, "coin / coin - Sense 1", "Money sense definition", vector(2));
        seedChunk("error:false_friend_confusion", "false_friend_confusion", KnowledgeSourceTypes.ERROR_TYPE, "False Friend Confusion", "This explains false friend confusion in detail.", vector(3));

        AiProviderRegistry providerRegistry = mock(AiProviderRegistry.class);
        when(providerRegistry.embed(any())).thenReturn(new EmbeddingResponse(
                "qwen",
                "test-embedding-model",
                1024,
                "embed-test",
                null,
                List.of(new EmbeddingItem(0, "query", vector(1)))
        ));
        when(providerRegistry.rerank(any())).thenThrow(new IllegalStateException("rerank unavailable"));

        RagProperties ragProperties = new RagProperties();
        ragProperties.getRetrieval().setRecallTopK(10);
        ragProperties.getRetrieval().setRecallThreshold(0.0d);
        ragProperties.getRetrieval().setRerankTopN(5);
        ragProperties.getRetrieval().setRerankThreshold(0.2d);
        ragProperties.getRetrieval().setFinalTopK(2);
        ragProperties.getAppServer().setBaseUrl("http://localhost:8080");
        ragProperties.getAppServer().setInternalToken("test-internal-token");

        AiRuntimeConfigService runtimeConfigService = mock(AiRuntimeConfigService.class);
        when(runtimeConfigService.current()).thenReturn(runtimeBundle(ragProperties));

        KnowledgeSearchService knowledgeSearchService = new KnowledgeSearchService(
                providerRegistry,
                knowledgeStoreRepository,
                runtimeConfigService,
                new RetrievalQueryPlanner(providerRegistry)
        );
        RagRetrievalResult result = knowledgeSearchService.search("Why is coin/coin risky?", RagSearchFilter.empty());

        assertThat(result.chunks()).hasSize(2);
        assertThat(result.chunks().get(0).sourceType()).isEqualTo(KnowledgeSourceTypes.LEXICAL_PAIR);
        assertThat(result.chunks().get(0).score()).isGreaterThan(result.chunks().get(1).score());
    }

    @Test
    void shouldNotCompareVectorsFromDifferentEmbeddingModels() {
        seedChunk("pair:1001", "1001", KnowledgeSourceTypes.LEXICAL_PAIR, "coin / coin", "False friend pair guidance", vector(1));

        List<KnowledgeSearchCandidate> candidates = knowledgeStoreRepository.similaritySearch(
                vector(1),
                "different-embedding-model",
                RagSearchFilter.empty(),
                10,
                64
        );

        assertThat(candidates).isEmpty();
        assertThat(knowledgeStoreRepository.getIndexCoverage("different-embedding-model", 1024))
                .satisfies(coverage -> {
                    assertThat(coverage.activeChunkCount()).isEqualTo(1);
                    assertThat(coverage.searchableChunkCount()).isZero();
                    assertThat(coverage.isComplete()).isFalse();
                });
    }

    @Test
    void shouldNotServeUpdatedContentWithStaleEmbedding() {
        seedChunk("pair:1001", "1001", KnowledgeSourceTypes.LEXICAL_PAIR, "coin / coin", "Old content", vector(1));
        KnowledgeDocumentPayload updated = new KnowledgeDocumentPayload(
                KnowledgeSourceTypes.LEXICAL_PAIR,
                "1001",
                "coin / coin",
                OffsetDateTime.now(ZoneOffset.UTC),
                true,
                Map.of(),
                List.of(new KnowledgeChunkPayload(
                        "pair:1001",
                        0,
                        KnowledgeSourceTypes.LEXICAL_PAIR,
                        "1001",
                        "coin / coin",
                        "Updated content",
                        Map.of(),
                        true
                ))
        );
        KnowledgeStoreRepository.UpsertDocumentResult result = knowledgeStoreRepository.upsertDocument(updated, false, "updated-doc-hash");
        knowledgeStoreRepository.markChunkEmbeddingFailed(
                result.pendingChunkEmbeddings().getFirst().chunkId(),
                result.pendingChunkEmbeddings().getFirst().contentHash()
        );

        List<KnowledgeSearchCandidate> candidates = knowledgeStoreRepository.similaritySearch(
                vector(1),
                "test-embedding-model",
                RagSearchFilter.empty(),
                10,
                64
        );

        assertThat(candidates).isEmpty();
    }

    @Test
    void shouldValidateReplacementBeforeInvalidatingCurrentEmbedding() {
        seedChunk("pair:1001", "1001", KnowledgeSourceTypes.LEXICAL_PAIR, "coin / coin", "Content", vector(1));
        Long chunkId = jdbcTemplate.queryForObject("SELECT id FROM knowledge_chunk WHERE chunk_key = 'pair:1001'", Long.class);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> knowledgeStoreRepository.replaceChunkEmbedding(
                chunkId,
                "test-embedding-model",
                1024,
                "new-content-hash",
                List.of(0.1d)
        )).isInstanceOf(IllegalArgumentException.class);

        assertThat(countCurrentEmbeddings()).isEqualTo(1);
    }

    @Test
    void shouldRejectStaleEmbeddingWriteAfterChunkContentChanges() {
        KnowledgeStoreRepository.UpsertDocumentResult original = knowledgeStoreRepository.upsertDocument(
                sampleDocument("1001", true),
                false,
                "doc-hash-1"
        );
        var originalPair = original.pendingChunkEmbeddings().getFirst();

        KnowledgeDocumentPayload updated = sampleDocument("1001", true);
        KnowledgeChunkPayload oldPair = updated.chunks().getFirst();
        KnowledgeDocumentPayload changed = new KnowledgeDocumentPayload(
                updated.sourceType(),
                updated.sourceId(),
                updated.title(),
                updated.sourceUpdatedAt(),
                updated.active(),
                updated.metadata(),
                List.of(
                        new KnowledgeChunkPayload(
                                oldPair.chunkKey(),
                                oldPair.chunkOrder(),
                                oldPair.sourceType(),
                                oldPair.sourceId(),
                                oldPair.title(),
                                oldPair.content() + " Updated evidence.",
                                oldPair.metadata(),
                                oldPair.active()
                        ),
                        updated.chunks().get(1)
                )
        );
        KnowledgeStoreRepository.UpsertDocumentResult latest = knowledgeStoreRepository.upsertDocument(
                changed,
                false,
                "doc-hash-2"
        );
        var latestPair = latest.pendingChunkEmbeddings().stream()
                .filter(chunk -> chunk.chunkId().equals(originalPair.chunkId()))
                .findFirst()
                .orElseThrow();

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> knowledgeStoreRepository.replaceChunkEmbedding(
                originalPair.chunkId(),
                "test-embedding-model",
                1024,
                originalPair.contentHash(),
                vector(1)
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("stale");

        knowledgeStoreRepository.markChunkEmbeddingFailed(originalPair.chunkId(), originalPair.contentHash());
        assertThat(singleString("SELECT embedding_status FROM knowledge_chunk WHERE id = " + originalPair.chunkId()))
                .isEqualTo("PENDING");
        assertThat(countCurrentEmbeddings()).isZero();

        knowledgeStoreRepository.replaceChunkEmbedding(
                latestPair.chunkId(),
                "test-embedding-model",
                1024,
                latestPair.contentHash(),
                vector(1)
        );
        assertThat(singleString("SELECT embedding_status FROM knowledge_chunk WHERE id = " + originalPair.chunkId()))
                .isEqualTo("EMBEDDED");
        assertThat(countCurrentEmbeddings()).isEqualTo(1);
    }

    private AiRuntimeBundle runtimeBundle(RagProperties ragProperties) {
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

    private void seedChunk(
            String chunkKey,
            String sourceId,
            String sourceType,
            String title,
            String content,
            List<Double> embedding
    ) {
        KnowledgeDocumentPayload payload = new KnowledgeDocumentPayload(
                sourceType.equals(KnowledgeSourceTypes.LEXICAL_PAIR) ? KnowledgeSourceTypes.LEXICAL_PAIR : sourceType,
                sourceId,
                title,
                OffsetDateTime.now(ZoneOffset.UTC),
                true,
                Map.of("sourceType", sourceType),
                List.of(new KnowledgeChunkPayload(
                        chunkKey,
                        0,
                        sourceType,
                        sourceId,
                        title,
                        content,
                        Map.of("sourceType", sourceType),
                        true
                ))
        );
        KnowledgeStoreRepository.UpsertDocumentResult result = knowledgeStoreRepository.upsertDocument(payload, false, "doc-" + chunkKey);
        knowledgeStoreRepository.replaceChunkEmbedding(
                result.pendingChunkEmbeddings().getFirst().chunkId(),
                "test-embedding-model",
                1024,
                result.pendingChunkEmbeddings().getFirst().contentHash(),
                embedding
        );
    }

    private int countCurrentEmbeddings() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM chunk_embedding WHERE is_current = TRUE", Integer.class);
        return count == null ? 0 : count;
    }

    private String singleString(String sql) {
        return jdbcTemplate.queryForObject(sql, String.class);
    }

    private KnowledgeDocumentPayload sampleDocument(String sourceId, boolean active) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("lexicalPairId", sourceId);
        metadata.put("englishWord", "coin");
        metadata.put("frenchWord", "coin");

        return new KnowledgeDocumentPayload(
                KnowledgeSourceTypes.LEXICAL_PAIR,
                sourceId,
                "coin / coin",
                OffsetDateTime.now(ZoneOffset.UTC),
                active,
                metadata,
                List.of(
                        new KnowledgeChunkPayload(
                                "pair:" + sourceId,
                                0,
                                KnowledgeSourceTypes.LEXICAL_PAIR,
                                sourceId,
                                "coin / coin",
                                "English word: coin\nFrench word: coin\nChinese gloss: 硬币；角落",
                                Map.of("chunkKind", "LEXICAL_PAIR"),
                                active
                        ),
                        new KnowledgeChunkPayload(
                                "sense:2001",
                                1,
                                KnowledgeSourceTypes.LEXICAL_SENSE,
                                "2001",
                                "coin / coin - Sense 1",
                                "English definition: a piece of money",
                                Map.of("chunkKind", "SENSE"),
                                active
                        )
                )
        );
    }

    private List<Double> vector(int hotIndex) {
        List<Double> values = new java.util.ArrayList<>(1024);
        for (int index = 0; index < 1024; index++) {
            values.add(index == hotIndex ? 0.99d : 0.0d);
        }
        return values;
    }
}
