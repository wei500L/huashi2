package com.huashi.eftransfer.ai.integration.provider;

import com.huashi.eftransfer.shared.ai.ChatRequest;
import com.huashi.eftransfer.shared.ai.ChatResponse;
import com.huashi.eftransfer.shared.ai.EmbeddingBatchRequest;
import com.huashi.eftransfer.shared.ai.EmbeddingRequest;
import com.huashi.eftransfer.shared.ai.EmbeddingResponse;
import com.huashi.eftransfer.shared.ai.RerankRequest;
import com.huashi.eftransfer.shared.ai.RerankResponse;
import com.huashi.eftransfer.shared.ai.StructuredChatRequest;
import com.huashi.eftransfer.shared.ai.StructuredChatResponse;

public interface AiProviderFacade {

    String providerName();

    ChatResponse chat(ChatRequest request);

    StructuredChatResponse structuredChat(StructuredChatRequest request);

    EmbeddingResponse embed(EmbeddingRequest request);

    EmbeddingResponse embedBatch(EmbeddingBatchRequest request);

    RerankResponse rerank(RerankRequest request);
}
