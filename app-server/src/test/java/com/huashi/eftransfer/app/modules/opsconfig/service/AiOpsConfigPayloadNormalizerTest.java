package com.huashi.eftransfer.app.modules.opsconfig.service;

import com.huashi.eftransfer.shared.ai.config.AiOpsChatConfig;
import com.huashi.eftransfer.shared.ai.config.AiOpsEmbeddingConfig;
import com.huashi.eftransfer.shared.ai.config.AiOpsProviderDefinition;
import com.huashi.eftransfer.shared.ai.config.AiOpsProtocols;
import com.huashi.eftransfer.shared.ai.config.AiOpsRerankConfig;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiOpsConfigPayloadNormalizerTest {

    private final AiOpsConfigPayloadNormalizer normalizer = new AiOpsConfigPayloadNormalizer();

    @Test
    void shouldBackfillMissingSplitTimeoutsFromSiblingFields() {
        AiOpsProviderDefinition normalized = normalizer.normalizeProviderDefinition(new AiOpsProviderDefinition(
                new AiOpsChatConfig(AiOpsProtocols.OPENAI_COMPAT, "https://example.com/v1", "chat-key", "qwen-max", null, "PT30S", 0.2d, 1024),
                new AiOpsEmbeddingConfig(AiOpsProtocols.OPENAI_COMPAT, "https://example.com/v1", "embed-key", "text-embedding-v4", "PT3S", null, 1024),
                new AiOpsRerankConfig(AiOpsProtocols.QWEN_RERANK, "https://example.com/rerank", "rerank-key", "gte-rerank-v2", null, "PT15S")
        ));

        assertThat(normalized.chat().connectTimeout()).isEqualTo("PT30S");
        assertThat(normalized.chat().readTimeout()).isEqualTo("PT30S");
        assertThat(normalized.embedding().connectTimeout()).isEqualTo("PT3S");
        assertThat(normalized.embedding().readTimeout()).isEqualTo("PT3S");
        assertThat(normalized.rerank().connectTimeout()).isEqualTo("PT15S");
        assertThat(normalized.rerank().readTimeout()).isEqualTo("PT15S");
    }
}
