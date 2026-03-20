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
import org.springframework.stereotype.Component;

@Component
public class QwenAiProviderFacade implements AiProviderFacade {

    private final QwenChatProviderClient chatProviderClient;
    private final QwenEmbeddingProviderClient embeddingProviderClient;
    private final RerankClient rerankClient;

    public QwenAiProviderFacade(
            QwenChatProviderClient chatProviderClient,
            QwenEmbeddingProviderClient embeddingProviderClient,
            RerankClient rerankClient
    ) {
        this.chatProviderClient = chatProviderClient;
        this.embeddingProviderClient = embeddingProviderClient;
        this.rerankClient = rerankClient;
    }

    @Override
    public String providerName() {
        return "qwen";
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        return chatProviderClient.chat(request);
    }

    @Override
    public StructuredChatResponse structuredChat(StructuredChatRequest request) {
        return chatProviderClient.structuredChat(request);
    }

    @Override
    public EmbeddingResponse embed(EmbeddingRequest request) {
        return embeddingProviderClient.embed(request);
    }

    @Override
    public EmbeddingResponse embedBatch(EmbeddingBatchRequest request) {
        return embeddingProviderClient.embedBatch(request);
    }

    @Override
    public RerankResponse rerank(RerankRequest request) {
        return rerankClient.rerank(request);
    }
}
