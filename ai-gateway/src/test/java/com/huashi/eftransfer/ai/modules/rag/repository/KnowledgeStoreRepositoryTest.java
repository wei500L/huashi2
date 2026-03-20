package com.huashi.eftransfer.ai.modules.rag.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huashi.eftransfer.ai.integration.provider.AiProviderFacade;
import com.huashi.eftransfer.ai.integration.provider.AiProviderRegistry;
import com.huashi.eftransfer.ai.modules.rag.config.RagProperties;
import com.huashi.eftransfer.ai.modules.rag.service.KnowledgeSearchService;
import com.huashi.eftransfer.ai.modules.rag.support.KnowledgeChunkPayload;
import com.huashi.eftransfer.ai.modules.rag.support.KnowledgeDocumentPayload;
import com.huashi.eftransfer.ai.modules.rag.support.KnowledgeSourceTypes;
import com.huashi.eftransfer.ai.modules.rag.support.RagRetrievalResult;
import com.huashi.eftransfer.ai.modules.rag.support.RagSearchFilter;
import com.huashi.eftransfer.shared.ai.EmbeddingItem;
import com.huashi.eftransfer.shared.ai.EmbeddingResponse;
import com.huashi.eftransfer.shared.ai.RerankItem;
import com.huashi.eftransfer.shared.ai.RerankResponse;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
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

        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();

        jdbcTemplate = new JdbcTemplate(dataSource);
        knowledgeStoreRepository = new KnowledgeStoreRepository(jdbcTemplate, new ObjectMapper());
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

        AiProviderFacade provider = mock(AiProviderFacade.class);
        AiProviderRegistry providerRegistry = mock(AiProviderRegistry.class);
        when(providerRegistry.resolveActiveProvider()).thenReturn(provider);
        when(provider.embed(any())).thenReturn(new EmbeddingResponse(
                "qwen",
                "text-embedding-v4",
                1024,
                "embed-test",
                null,
                List.of(new EmbeddingItem(0, "query", vector(1)))
        ));
        when(provider.rerank(any())).thenReturn(new RerankResponse(
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

        KnowledgeSearchService knowledgeSearchService = new KnowledgeSearchService(providerRegistry, knowledgeStoreRepository, ragProperties);
        RagRetrievalResult result = knowledgeSearchService.search("Why is coin/coin risky?", RagSearchFilter.empty());

        assertThat(result.chunks()).hasSize(2);
        assertThat(result.chunks().get(0).sourceType()).isEqualTo(KnowledgeSourceTypes.ERROR_TYPE);
        assertThat(result.chunks().get(1).sourceType()).isEqualTo(KnowledgeSourceTypes.LEXICAL_PAIR);
        assertThat(result.chunks().get(0).citationId()).isEqualTo("C1");
        assertThat(result.documents()).hasSize(2);
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
