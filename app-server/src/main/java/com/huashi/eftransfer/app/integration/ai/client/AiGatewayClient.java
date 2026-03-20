package com.huashi.eftransfer.app.integration.ai.client;

import com.huashi.eftransfer.app.integration.ai.dto.AiGatewayHealthResponse;
import com.huashi.eftransfer.shared.ai.ChatRequest;
import com.huashi.eftransfer.shared.ai.ChatResponse;
import com.huashi.eftransfer.shared.ai.EmbeddingBatchRequest;
import com.huashi.eftransfer.shared.ai.EmbeddingRequest;
import com.huashi.eftransfer.shared.ai.EmbeddingResponse;
import com.huashi.eftransfer.shared.ai.RerankRequest;
import com.huashi.eftransfer.shared.ai.RerankResponse;
import com.huashi.eftransfer.shared.ai.StructuredChatRequest;
import com.huashi.eftransfer.shared.ai.StructuredChatResponse;
import com.huashi.eftransfer.shared.api.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;

@Component
public class AiGatewayClient {

    private static final Logger log = LoggerFactory.getLogger(AiGatewayClient.class);
    private static final ParameterizedTypeReference<ApiResponse<AiGatewayHealthResponse>> HEALTH_TYPE =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<ApiResponse<ChatResponse>> CHAT_TYPE =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<ApiResponse<StructuredChatResponse>> STRUCTURED_CHAT_TYPE =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<ApiResponse<EmbeddingResponse>> EMBEDDING_TYPE =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<ApiResponse<RerankResponse>> RERANK_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient aiGatewayRestClient;

    public AiGatewayClient(RestClient aiGatewayRestClient) {
        this.aiGatewayRestClient = aiGatewayRestClient;
    }

    public Optional<AiGatewayHealthResponse> fetchHealth() {
        try {
            ApiResponse<AiGatewayHealthResponse> response = aiGatewayRestClient.get()
                    .uri("/internal/ai/health")
                    .retrieve()
                    .body(HEALTH_TYPE);

            if (response == null || !response.success()) {
                return Optional.empty();
            }
            return Optional.ofNullable(response.data());
        } catch (RestClientException ex) {
            log.warn("event=ai_gateway_unreachable reason={}", ex.getMessage());
            return Optional.empty();
        }
    }

    public ChatResponse chat(ChatRequest request) {
        return post("/internal/ai/chat", request, CHAT_TYPE);
    }

    public StructuredChatResponse structuredChat(StructuredChatRequest request) {
        return post("/internal/ai/chat/structured", request, STRUCTURED_CHAT_TYPE);
    }

    public EmbeddingResponse embed(EmbeddingRequest request) {
        return post("/internal/ai/embed", request, EMBEDDING_TYPE);
    }

    public EmbeddingResponse embedBatch(EmbeddingBatchRequest request) {
        return post("/internal/ai/embed/batch", request, EMBEDDING_TYPE);
    }

    public RerankResponse rerank(RerankRequest request) {
        return post("/internal/ai/rerank", request, RERANK_TYPE);
    }

    private <T, R> R post(String uri, T request, ParameterizedTypeReference<ApiResponse<R>> responseType) {
        ApiResponse<R> response = aiGatewayRestClient.post()
                .uri(uri)
                .body(request)
                .retrieve()
                .body(responseType);

        if (response == null || !response.success() || response.data() == null) {
            throw new RestClientException("Unexpected ai-gateway response for " + uri);
        }
        return response.data();
    }
}
