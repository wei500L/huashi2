package com.huashi.eftransfer.ai.modules.internal.service;

import com.huashi.eftransfer.ai.integration.provider.AiProviderRegistry;
import com.huashi.eftransfer.shared.ai.ChatRequest;
import com.huashi.eftransfer.shared.ai.ChatResponse;
import com.huashi.eftransfer.shared.ai.EmbeddingBatchRequest;
import com.huashi.eftransfer.shared.ai.EmbeddingRequest;
import com.huashi.eftransfer.shared.ai.EmbeddingResponse;
import com.huashi.eftransfer.shared.ai.RerankRequest;
import com.huashi.eftransfer.shared.ai.RerankResponse;
import com.huashi.eftransfer.shared.ai.StructuredChatRequest;
import com.huashi.eftransfer.shared.ai.StructuredChatResponse;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.exception.BusinessException;
import org.springframework.stereotype.Service;

@Service
public class InternalAiService {

    private final AiProviderRegistry providerRegistry;

    public InternalAiService(AiProviderRegistry providerRegistry) {
        this.providerRegistry = providerRegistry;
    }

    public ChatResponse chat(ChatRequest request) {
        return providerRegistry.chat(request);
    }

    public StructuredChatResponse structuredChat(StructuredChatRequest request) {
        return providerRegistry.structuredChat(request);
    }

    public EmbeddingResponse embed(EmbeddingRequest request) {
        return providerRegistry.embed(request);
    }

    public EmbeddingResponse embedBatch(EmbeddingBatchRequest request) {
        return providerRegistry.embedBatch(request);
    }

    public RerankResponse rerank(RerankRequest request) {
        if (request.topN() != null && request.topN() > request.documents().size()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "topN must be less than or equal to documents size", 400);
        }
        return providerRegistry.rerank(request);
    }
}
