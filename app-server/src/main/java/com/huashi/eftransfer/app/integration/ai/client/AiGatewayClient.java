package com.huashi.eftransfer.app.integration.ai.client;

import com.huashi.eftransfer.app.common.config.AiGatewayClientProperties;
import com.huashi.eftransfer.app.integration.ai.dto.AiGatewayHealthResponse;
import com.huashi.eftransfer.shared.ai.ChatRequest;
import com.huashi.eftransfer.shared.ai.ChatResponse;
import com.huashi.eftransfer.shared.ai.EmbeddingBatchRequest;
import com.huashi.eftransfer.shared.ai.EmbeddingRequest;
import com.huashi.eftransfer.shared.ai.EmbeddingResponse;
import com.huashi.eftransfer.shared.ai.RerankRequest;
import com.huashi.eftransfer.shared.ai.RerankResponse;
import com.huashi.eftransfer.shared.ai.RagAnswerRequest;
import com.huashi.eftransfer.shared.ai.RagAnswerResponse;
import com.huashi.eftransfer.shared.ai.RagExplainRiskRequest;
import com.huashi.eftransfer.shared.ai.RagExplainRiskResponse;
import com.huashi.eftransfer.shared.ai.StructuredChatRequest;
import com.huashi.eftransfer.shared.ai.StructuredChatResponse;
import com.huashi.eftransfer.shared.api.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpTimeoutException;
import java.time.Duration;
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
    private static final ParameterizedTypeReference<ApiResponse<RagAnswerResponse>> RAG_ANSWER_TYPE =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<ApiResponse<RagExplainRiskResponse>> RAG_EXPLAIN_RISK_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient aiGatewayRestClient;
    private final AiGatewayClientProperties properties;

    public AiGatewayClient(RestClient aiGatewayRestClient, AiGatewayClientProperties properties) {
        this.aiGatewayRestClient = aiGatewayRestClient;
        this.properties = properties;
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

    public AiGatewayCallResult<ChatResponse> chat(ChatRequest request) {
        return post("/internal/ai/chat", request, CHAT_TYPE);
    }

    public AiGatewayCallResult<StructuredChatResponse> structuredChat(StructuredChatRequest request) {
        return post("/internal/ai/chat/structured", request, STRUCTURED_CHAT_TYPE);
    }

    public AiGatewayCallResult<EmbeddingResponse> embed(EmbeddingRequest request) {
        return post("/internal/ai/embed", request, EMBEDDING_TYPE);
    }

    public AiGatewayCallResult<EmbeddingResponse> embedBatch(EmbeddingBatchRequest request) {
        return post("/internal/ai/embed/batch", request, EMBEDDING_TYPE);
    }

    public AiGatewayCallResult<RerankResponse> rerank(RerankRequest request) {
        return post("/internal/ai/rerank", request, RERANK_TYPE);
    }

    public AiGatewayCallResult<RagAnswerResponse> ragAnswer(RagAnswerRequest request) {
        return post("/internal/ai/rag/answer", request, RAG_ANSWER_TYPE);
    }

    public AiGatewayCallResult<RagExplainRiskResponse> explainRisk(RagExplainRiskRequest request) {
        return post("/internal/ai/rag/explain-risk", request, RAG_EXPLAIN_RISK_TYPE);
    }

    private <T, R> AiGatewayCallResult<R> post(String uri, T request, ParameterizedTypeReference<ApiResponse<R>> responseType) {
        long startedAt = System.nanoTime();
        int attempts = 0;
        AiGatewayFailureReason lastReason = AiGatewayFailureReason.UNKNOWN;
        String lastMessage = "Unknown ai-gateway failure";

        while (attempts < properties.getMaxAttempts()) {
            attempts++;
            try {
                ApiResponse<R> response = aiGatewayRestClient.post()
                        .uri(uri)
                        .body(request)
                        .retrieve()
                        .body(responseType);

                if (response == null || !response.success() || response.data() == null) {
                    throw new IllegalStateException("Unexpected ai-gateway response for " + uri);
                }
                return AiGatewayCallResult.success(response.data(), attempts, elapsedMillis(startedAt), uri);
            } catch (IllegalStateException ex) {
                lastReason = AiGatewayFailureReason.INVALID_RESPONSE;
                lastMessage = ex.getMessage();
                break;
            } catch (RestClientResponseException ex) {
                lastReason = mapStatusReason(ex.getStatusCode().value(), uri);
                lastMessage = ex.getMessage();
                if (!isRetryableStatus(ex.getStatusCode().value()) || attempts >= properties.getMaxAttempts()) {
                    break;
                }
                backoff();
            } catch (RestClientException ex) {
                lastReason = mapClientReason(ex, uri);
                lastMessage = ex.getMessage();
                if (!isRetryableException(ex) || attempts >= properties.getMaxAttempts()) {
                    break;
                }
                backoff();
            }
        }
        log.warn("event=ai_gateway_call_failed endpoint={} attempts={} reason={} message={}",
                uri, attempts, lastReason, lastMessage);
        return AiGatewayCallResult.failure(lastReason, lastMessage, attempts, elapsedMillis(startedAt), uri);
    }

    private void backoff() {
        Duration retryBackoff = properties.getRetryBackoff();
        if (retryBackoff == null || retryBackoff.isZero() || retryBackoff.isNegative()) {
            return;
        }
        try {
            Thread.sleep(retryBackoff.toMillis());
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean isRetryableStatus(int statusCode) {
        return statusCode == 429 || statusCode == 502 || statusCode == 503 || statusCode == 504 || statusCode >= 500;
    }

    private boolean isRetryableException(RestClientException ex) {
        Throwable cause = ex.getCause();
        if (cause instanceof HttpTimeoutException) {
            return true;
        }
        String message = ex.getMessage();
        return message != null && (message.contains("timed out") || message.contains("Connection refused"));
    }

    private AiGatewayFailureReason mapStatusReason(int statusCode, String uri) {
        if (statusCode == 503 && uri.contains("/rag/")) {
            return AiGatewayFailureReason.RAG_UNAVAILABLE;
        }
        if (statusCode == 502 || statusCode == 503 || statusCode == 504) {
            return AiGatewayFailureReason.PROVIDER_UNAVAILABLE;
        }
        return AiGatewayFailureReason.HTTP_ERROR;
    }

    private AiGatewayFailureReason mapClientReason(RestClientException ex, String uri) {
        Throwable cause = ex.getCause();
        if (cause instanceof HttpTimeoutException) {
            return AiGatewayFailureReason.TIMEOUT;
        }
        String message = ex.getMessage();
        if (message != null && message.contains("timed out")) {
            return AiGatewayFailureReason.TIMEOUT;
        }
        if (uri.contains("/rag/")) {
            return AiGatewayFailureReason.RAG_UNAVAILABLE;
        }
        return AiGatewayFailureReason.UNKNOWN;
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
