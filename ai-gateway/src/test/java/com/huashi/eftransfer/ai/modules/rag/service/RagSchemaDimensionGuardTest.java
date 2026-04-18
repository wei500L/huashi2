package com.huashi.eftransfer.ai.modules.rag.service;

import com.huashi.eftransfer.ai.common.runtime.AiRuntimeBundle;
import com.huashi.eftransfer.ai.common.runtime.AiRuntimeConfigService;
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

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RagSchemaDimensionGuardTest {

    @Test
    void shouldAllowFixed1024Dimensions() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        AiRuntimeConfigService runtimeConfigService = mock(AiRuntimeConfigService.class);
        when(jdbcTemplate.queryForObject(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq(String.class)))
                .thenReturn("vector(1024)");
        when(runtimeConfigService.current()).thenReturn(runtimeBundle(1024));

        RagSchemaDimensionGuard guard = new RagSchemaDimensionGuard(jdbcTemplate, runtimeConfigService, 1024);

        assertThatCode(guard::verifyDimensions).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectSchemaDimensionMismatch() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        AiRuntimeConfigService runtimeConfigService = mock(AiRuntimeConfigService.class);
        when(jdbcTemplate.queryForObject(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq(String.class)))
                .thenReturn("vector(1536)");
        when(runtimeConfigService.current()).thenReturn(runtimeBundle(1024));

        RagSchemaDimensionGuard guard = new RagSchemaDimensionGuard(jdbcTemplate, runtimeConfigService, 1024);

        assertThatThrownBy(guard::verifyDimensions)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("chunk_embedding.embedding");
    }

    @Test
    void shouldRejectProviderDimensionMismatch() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        AiRuntimeConfigService runtimeConfigService = mock(AiRuntimeConfigService.class);
        when(jdbcTemplate.queryForObject(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq(String.class)))
                .thenReturn("vector(1024)");
        when(runtimeConfigService.current()).thenReturn(runtimeBundle(1536));

        RagSchemaDimensionGuard guard = new RagSchemaDimensionGuard(jdbcTemplate, runtimeConfigService, 1024);

        assertThatThrownBy(guard::verifyDimensions)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Provider embedding dimensions");
    }

    private AiRuntimeBundle runtimeBundle(int embeddingDimension) {
        AiOpsProviderDefinition provider = new AiOpsProviderDefinition(
                new AiOpsChatConfig(AiOpsProtocols.OPENAI_COMPAT, "https://example.com/v1", "chat-key", "qwen-max", "PT30S", 0.2d, 1024),
                new AiOpsEmbeddingConfig(AiOpsProtocols.OPENAI_COMPAT, "https://example.com/v1", "embed-key", "text-embedding-v4", "PT30S", embeddingDimension),
                new AiOpsRerankConfig(AiOpsProtocols.QWEN_RERANK, "https://example.com/rerank", "rerank-key", "gte-rerank-v2", "PT30S")
        );
        return new AiRuntimeBundle(
                new AiOpsConfigPayload(
                        new AiOpsProviderConfig("qwen", "deepseek", Map.of("qwen", provider, "deepseek", provider)),
                        new AiOpsResilienceConfig(1, "PT0.1S", 50.0f, 10, "PT5S"),
                        new AiOpsRagConfig(
                                new AiOpsRagAppServerConfig("http://localhost:8080", "test-internal-token", "PT3S", "PT5S"),
                                new AiOpsRagIngestionConfig(100, 32),
                                new AiOpsRagRetrievalConfig(20, 0.55d, 8, 0.2d, 6)
                        )
                ),
                Map.of(),
                null,
                "TEST",
                1L,
                OffsetDateTime.now(ZoneOffset.UTC)
        );
    }
}
