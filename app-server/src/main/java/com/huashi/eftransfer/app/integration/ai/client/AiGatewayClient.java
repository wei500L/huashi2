package com.huashi.eftransfer.app.integration.ai.client;

import com.huashi.eftransfer.app.common.config.AiGatewayClientProperties;
import com.huashi.eftransfer.shared.ai.AiGatewayHealthResponse;
import com.huashi.eftransfer.shared.ai.AdminAiEmbeddingProbeVO;
import com.huashi.eftransfer.shared.ai.AdminAiRerankProbeVO;
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
import com.huashi.eftransfer.shared.ai.RagReindexJobResponse;
import com.huashi.eftransfer.shared.ai.RagReindexRequest;
import com.huashi.eftransfer.shared.ai.RagReindexResponse;
import com.huashi.eftransfer.shared.ai.RagRetrieveRequest;
import com.huashi.eftransfer.shared.ai.RagRetrieveResponse;
import com.huashi.eftransfer.shared.ai.StructuredChatRequest;
import com.huashi.eftransfer.shared.ai.StructuredChatResponse;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigApplyRequest;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigApplyResponse;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigCommitRequest;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigEffectiveResponse;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigPayload;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigStageRequest;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigStageResponse;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigValidationResponse;
import com.huashi.eftransfer.shared.api.ApiResponse;
import com.huashi.eftransfer.shared.security.InternalApiHeaders;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

@Component
public class AiGatewayClient {

    private static final Logger log = LoggerFactory.getLogger(AiGatewayClient.class);
    private static final ParameterizedTypeReference<ApiResponse<AiGatewayHealthResponse>> HEALTH_TYPE =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<ApiResponse<AdminAiEmbeddingProbeVO>> EMBEDDING_PROBE_TYPE =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<ApiResponse<AdminAiRerankProbeVO>> RERANK_PROBE_TYPE =
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
    private static final ParameterizedTypeReference<ApiResponse<RagRetrieveResponse>> RAG_RETRIEVE_TYPE =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<ApiResponse<RagExplainRiskResponse>> RAG_EXPLAIN_RISK_TYPE =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<ApiResponse<RagReindexResponse>> RAG_REINDEX_TYPE =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<ApiResponse<RagReindexJobResponse>> RAG_REINDEX_JOB_TYPE =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<ApiResponse<AiOpsConfigEffectiveResponse>> CONFIG_EFFECTIVE_TYPE =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<ApiResponse<AiOpsConfigValidationResponse>> CONFIG_VALIDATE_TYPE =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<ApiResponse<AiOpsConfigApplyResponse>> CONFIG_APPLY_TYPE =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<ApiResponse<AiOpsConfigStageResponse>> CONFIG_STAGE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient aiGatewayRestClient;
    private final AiGatewayClientProperties properties;
    private final RetryRegistry retryRegistry;

    public AiGatewayClient(RestClient aiGatewayRestClient, AiGatewayClientProperties properties) {
        this.aiGatewayRestClient = aiGatewayRestClient;
        this.properties = properties;
        this.retryRegistry = RetryRegistry.of(RetryConfig.custom()
                .maxAttempts(properties.getMaxAttempts())
                .waitDuration(normalizeWaitDuration(properties.getRetryBackoff()))
                .retryOnException(this::isRetryableFailure)
                .failAfterMaxAttempts(false)
                .build());
    }

    public Optional<AiGatewayHealthResponse> fetchHealth() {
        try {
            ApiResponse<AiGatewayHealthResponse> response = aiGatewayRestClient.get()
                    .uri("/internal/ai/health")
                    .headers(headers -> headers.set(InternalApiHeaders.INTERNAL_TOKEN, properties.getInternalToken()))
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

    public AiGatewayCallResult<AdminAiEmbeddingProbeVO> probeEmbeddingConfig(AiOpsConfigPayload payload) {
        return post("/internal/ai/config/probes/embedding", payload, EMBEDDING_PROBE_TYPE);
    }

    public AiGatewayCallResult<AdminAiRerankProbeVO> probeRerankConfig(AiOpsConfigPayload payload) {
        return post("/internal/ai/config/probes/rerank", payload, RERANK_PROBE_TYPE);
    }

    public AiGatewayCallResult<RagAnswerResponse> ragAnswer(RagAnswerRequest request) {
        return post("/internal/ai/rag/answer", request, RAG_ANSWER_TYPE);
    }

    public AiGatewayCallResult<RagRetrieveResponse> ragRetrieve(RagRetrieveRequest request) {
        return post("/internal/ai/rag/retrieve", request, RAG_RETRIEVE_TYPE);
    }

    public AiGatewayCallResult<RagExplainRiskResponse> explainRisk(RagExplainRiskRequest request) {
        return post("/internal/ai/rag/explain-risk", request, RAG_EXPLAIN_RISK_TYPE);
    }

    public AiGatewayCallResult<RagReindexResponse> reindex(RagReindexRequest request) {
        return post("/internal/ai/rag/reindex", request, RAG_REINDEX_TYPE);
    }

    public Optional<RagReindexJobResponse> fetchReindexJob(Long jobId) {
        return getOptional("/internal/ai/rag/reindex/jobs/" + jobId, RAG_REINDEX_JOB_TYPE);
    }

    public Optional<AiOpsConfigEffectiveResponse> fetchEffectiveConfig() {
        return getOptional("/internal/ai/config/effective", CONFIG_EFFECTIVE_TYPE);
    }

    public AiOpsConfigValidationResponse validateConfig(AiOpsConfigPayload payload) {
        AiGatewayCallResult<AiOpsConfigValidationResponse> result = post(
                "/internal/ai/config/validate",
                payload,
                CONFIG_VALIDATE_TYPE
        );
        if (!result.success() || result.data() == null) {
            throw new IllegalStateException(result.failureMessage());
        }
        return result.data();
    }

    public AiOpsConfigApplyResponse applyConfig(AiOpsConfigPayload payload, String source, Long version) {
        AiGatewayCallResult<AiOpsConfigApplyResponse> result = post(
                "/internal/ai/config/apply",
                new AiOpsConfigApplyRequest(payload, source, version),
                CONFIG_APPLY_TYPE
        );
        if (!result.success() || result.data() == null) {
            throw new IllegalStateException(result.failureMessage());
        }
        return result.data();
    }

    public AiOpsConfigStageResponse stageConfig(AiOpsConfigPayload payload, String source, Long version) {
        AiGatewayCallResult<AiOpsConfigStageResponse> result = post(
                "/internal/ai/config/stage",
                new AiOpsConfigStageRequest(payload, source, version),
                CONFIG_STAGE_TYPE
        );
        if (!result.success() || result.data() == null) {
            throw new IllegalStateException(result.failureMessage());
        }
        return result.data();
    }

    public AiOpsConfigApplyResponse commitConfig(String stageId) {
        AiGatewayCallResult<AiOpsConfigApplyResponse> result = post(
                "/internal/ai/config/commit",
                new AiOpsConfigCommitRequest(stageId),
                CONFIG_APPLY_TYPE
        );
        if (!result.success() || result.data() == null) {
            throw new IllegalStateException(result.failureMessage());
        }
        return result.data();
    }

    private <T, R> AiGatewayCallResult<R> post(String uri, T request, ParameterizedTypeReference<ApiResponse<R>> responseType) {
        long startedAt = System.nanoTime();
        AtomicInteger attempts = new AtomicInteger();
        Retry retry = retryRegistry.retry(uri);
        Supplier<R> supplier = Retry.decorateSupplier(retry, () -> {
            attempts.incrementAndGet();
            try {
                ApiResponse<R> response = aiGatewayRestClient.post()
                        .uri(uri)
                        .headers(headers -> headers.set(InternalApiHeaders.INTERNAL_TOKEN, properties.getInternalToken()))
                        .body(request)
                        .retrieve()
                        .body(responseType);

                if (response == null || !response.success() || response.data() == null) {
                    throw new AiGatewayRequestFailure(
                            AiGatewayFailureReason.INVALID_RESPONSE,
                            "Unexpected ai-gateway response for " + uri,
                            false
                    );
                }
                return response.data();
            } catch (RestClientResponseException ex) {
                throw new AiGatewayRequestFailure(
                        mapStatusReason(ex.getStatusCode().value(), uri),
                        ex.getMessage(),
                        isRetryableStatus(ex.getStatusCode().value())
                );
            } catch (RestClientException ex) {
                throw new AiGatewayRequestFailure(
                        mapClientReason(ex, uri),
                        ex.getMessage(),
                        isRetryableException(ex)
                );
            }
        });
        try {
            return AiGatewayCallResult.success(supplier.get(), attempts.get(), elapsedMillis(startedAt), uri);
        } catch (AiGatewayRequestFailure ex) {
            log.warn("event=ai_gateway_call_failed endpoint={} attempts={} reason={} message={}",
                    uri, attempts.get(), ex.reason(), ex.getMessage());
            return AiGatewayCallResult.failure(ex.reason(), ex.getMessage(), attempts.get(), elapsedMillis(startedAt), uri);
        }
    }

    private <R> Optional<R> getOptional(String uri, ParameterizedTypeReference<ApiResponse<R>> responseType) {
        try {
            ApiResponse<R> response = aiGatewayRestClient.get()
                    .uri(uri)
                    .headers(headers -> headers.set(InternalApiHeaders.INTERNAL_TOKEN, properties.getInternalToken()))
                    .retrieve()
                    .body(responseType);

            if (response == null || !response.success()) {
                return Optional.empty();
            }
            return Optional.ofNullable(response.data());
        } catch (RestClientException ex) {
            log.warn("event=ai_gateway_get_failed endpoint={} reason={}", uri, ex.getMessage());
            return Optional.empty();
        }
    }

    private Duration normalizeWaitDuration(Duration retryBackoff) {
        if (retryBackoff == null || retryBackoff.isZero() || retryBackoff.isNegative()) {
            return Duration.ofMillis(1);
        }
        return retryBackoff;
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

    private boolean isRetryableFailure(Throwable throwable) {
        return throwable instanceof AiGatewayRequestFailure failure && failure.retryable();
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }

    private static final class AiGatewayRequestFailure extends RuntimeException {

        private final AiGatewayFailureReason reason;
        private final boolean retryable;

        private AiGatewayRequestFailure(AiGatewayFailureReason reason, String message, boolean retryable) {
            super(message);
            this.reason = reason;
            this.retryable = retryable;
        }

        private AiGatewayFailureReason reason() {
            return reason;
        }

        private boolean retryable() {
            return retryable;
        }
    }
}
