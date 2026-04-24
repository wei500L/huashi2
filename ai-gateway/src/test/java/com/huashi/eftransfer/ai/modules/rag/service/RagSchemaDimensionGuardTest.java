package com.huashi.eftransfer.ai.modules.rag.service;

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
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RagSchemaDimensionGuardTest {

    @Test
    void shouldAllowFixed1024Dimensions() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        mockSchema(jdbcTemplate, 1024, 16, 128);
        when(jdbcTemplate.queryForObject(anyString(), org.mockito.ArgumentMatchers.eq(String.class))).thenReturn("vector(1024)");

        RagSchemaDimensionGuard guard = new RagSchemaDimensionGuard(jdbcTemplate);

        assertThatCode(() -> guard.verifyConfig(payload(1024))).doesNotThrowAnyException();
    }

    @Test
    void shouldAllowSchemaDimensionWithoutStaticVectorStoreDimensionGate() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        mockSchema(jdbcTemplate, 1536, 16, 128);
        when(jdbcTemplate.queryForObject(anyString(), org.mockito.ArgumentMatchers.eq(String.class))).thenReturn("vector(1536)");

        RagSchemaDimensionGuard guard = new RagSchemaDimensionGuard(jdbcTemplate);

        assertThatCode(() -> guard.verifyConfig(payload(1536))).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectSchemaDimensionMismatch() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        mockSchema(jdbcTemplate, 1024, 16, 128);
        when(jdbcTemplate.queryForObject(anyString(), org.mockito.ArgumentMatchers.eq(String.class))).thenReturn("vector(1536)");

        RagSchemaDimensionGuard guard = new RagSchemaDimensionGuard(jdbcTemplate);

        assertThatThrownBy(() -> guard.verifyConfig(payload(1024)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("chunk_embedding.embedding");
    }

    @Test
    void shouldRejectProviderDimensionMismatch() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        mockSchema(jdbcTemplate, 1024, 16, 128);
        when(jdbcTemplate.queryForObject(anyString(), org.mockito.ArgumentMatchers.eq(String.class))).thenReturn("vector(1024)");

        RagSchemaDimensionGuard guard = new RagSchemaDimensionGuard(jdbcTemplate);

        assertThatThrownBy(() -> guard.verifyConfig(payload(1536)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Provider embedding dimensions");
    }

    @SuppressWarnings("unchecked")
    private void mockSchema(JdbcTemplate jdbcTemplate, int dimension, int hnswM, int hnswEfConstruction) {
        when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class))).thenAnswer(invocation -> {
            ResultSetExtractor<Object> extractor = invocation.getArgument(1);
            java.sql.ResultSet resultSet = mock(java.sql.ResultSet.class);
            when(resultSet.next()).thenReturn(true, false);
            when(resultSet.getInt("embedding_dimension")).thenReturn(dimension);
            when(resultSet.getInt("hnsw_m")).thenReturn(hnswM);
            when(resultSet.getInt("hnsw_ef_construction")).thenReturn(hnswEfConstruction);
            return extractor.extractData(resultSet);
        });
    }

    private AiOpsConfigPayload payload(int embeddingDimension) {
        AiOpsProviderDefinition provider = new AiOpsProviderDefinition(
                new AiOpsChatConfig(AiOpsProtocols.OPENAI_COMPAT, "https://example.com/v1", "chat-key", "qwen-max", "PT3S", "PT30S", 0.2d, 1024),
                new AiOpsEmbeddingConfig(AiOpsProtocols.OPENAI_COMPAT, "https://example.com/v1", "embed-key", "text-embedding-v4", "PT3S", "PT30S", embeddingDimension),
                new AiOpsRerankConfig(AiOpsProtocols.OPENAI_RERANK, "https://example.com/v1", "rerank-key", "gte-rerank-v2", "PT3S", "PT30S")
        );
        return new AiOpsConfigPayload(
                new AiOpsProviderConfig("qwen", "deepseek", Map.of("qwen", provider, "deepseek", provider)),
                new AiOpsResilienceConfig(1, "PT0.1S", 50.0f, 10, "PT5S"),
                new AiOpsRagConfig(
                        new AiOpsRagAppServerConfig("http://localhost:8080", "test-internal-token", "PT3S", "PT5S"),
                        new AiOpsRagIngestionConfig(100, 32),
                        new AiOpsRagRetrievalConfig(20, 0.55d, 8, 0.2d, 6, 64)
                )
        );
    }
}
