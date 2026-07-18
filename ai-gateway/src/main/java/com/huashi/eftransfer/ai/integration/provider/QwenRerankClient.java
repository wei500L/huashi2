package com.huashi.eftransfer.ai.integration.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huashi.eftransfer.ai.common.exception.InvalidProviderResponseException;
import com.huashi.eftransfer.ai.common.observability.AiProviderObservationService;
import com.huashi.eftransfer.ai.common.observability.ProviderRequestContextHolder;
import com.huashi.eftransfer.ai.common.observability.ResilientAiExecutor;
import com.huashi.eftransfer.ai.common.runtime.AiProviderRuntime;
import com.huashi.eftransfer.ai.common.runtime.AiRuntimeBundle;
import com.huashi.eftransfer.ai.common.runtime.AiRuntimeConfigService;
import com.huashi.eftransfer.shared.ai.RerankItem;
import com.huashi.eftransfer.shared.ai.RerankRequest;
import com.huashi.eftransfer.shared.ai.RerankResponse;
import com.huashi.eftransfer.shared.ai.config.AiOpsProtocols;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

@Component
public class QwenRerankClient implements RerankClient {

    private final AiRuntimeConfigService runtimeConfigService;
    private final ObjectMapper objectMapper;
    private final ResilientAiExecutor resilientAiExecutor;
    private final AiProviderObservationService observationService;
    private final ProviderRequestContextHolder requestContextHolder;

    public QwenRerankClient(
            AiRuntimeConfigService runtimeConfigService,
            ObjectMapper objectMapper,
            ResilientAiExecutor resilientAiExecutor,
            AiProviderObservationService observationService,
            ProviderRequestContextHolder requestContextHolder
    ) {
        this.runtimeConfigService = runtimeConfigService;
        this.objectMapper = objectMapper;
        this.resilientAiExecutor = resilientAiExecutor;
        this.observationService = observationService;
        this.requestContextHolder = requestContextHolder;
    }

    @Override
    public RerankResponse rerank(String providerName, RerankRequest request) {
        return rerank(providerRuntime(providerName), providerName, request);
    }

    public RerankResponse rerank(AiProviderRuntime runtime, String providerName, RerankRequest request) {
        String provider = providerName;
        String model = resolveModel(runtime, request.model(), request.modality());
        long startNanos = System.nanoTime();
        requestContextHolder.clear();

        try {
            boolean chatRerank = AiOpsProtocols.OPENAI_CHAT_RERANK.equals(runtime.definition().rerank().protocol());
            ProviderRerankResult providerResult = resilientAiExecutor.execute(runtime, "rerank", () -> {
                JsonNode response = runtime.rerankRestClient().post()
                        .uri(chatRerank ? "/v1/chat/completions" : "/v1/rerank")
                        .body(chatRerank ? buildChatPayload(request, model) : buildRerankPayload(request, model))
                        .retrieve()
                        .body(JsonNode.class);
                if (response == null) {
                    throw new InvalidProviderResponseException("Rerank provider returned no response");
                }
                JsonNode responseModel = response.path("model");
                if (responseModel.isTextual() && !responseModel.asText().isBlank()
                        && !model.equals(responseModel.asText())) {
                    throw new InvalidProviderResponseException(
                            "Rerank provider returned model %s but %s was requested"
                                    .formatted(responseModel.asText(), model)
                    );
                }
                return new ProviderRerankResult(
                        response,
                        chatRerank ? mapChatItems(request, response) : mapItems(request, response)
                );
            });

            JsonNode response = providerResult.response();
            List<RerankItem> items = providerResult.items();
            Integer totalTokens = resolveTotalTokens(response);
            String requestId = resolveRequestId(response);
            RerankResponse rerankResponse = new RerankResponse(
                    provider,
                    model,
                    requestId != null ? requestId : requestContextHolder.getRequestId(),
                    totalTokens,
                    items
            );
            observationService.recordSuccess(
                    "rerank",
                    provider,
                    model,
                    startNanos,
                    rerankResponse.providerRequestId(),
                    totalTokens == null ? null : new com.huashi.eftransfer.shared.ai.TokenUsage(totalTokens, null, totalTokens)
            );
            return rerankResponse;
        } catch (Exception ex) {
            throw observationService.recordFailure("rerank", provider, model, startNanos, ex);
        }
    }

    private Map<String, Object> buildRerankPayload(RerankRequest request, String model) {
        int topN = resolveTopN(request);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("query", request.query());
        payload.put("documents", request.documents());
        payload.put("top_n", topN);
        payload.put("return_documents", request.returnDocuments() == null || request.returnDocuments());
        if (StringUtils.hasText(request.instruct())) {
            payload.put("instruction", request.instruct());
        }
        return payload;
    }

    private Map<String, Object> buildChatPayload(RerankRequest request, String model) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("temperature", 0);
        payload.put("messages", List.of(
                Map.of(
                        "role", "system",
                        "content", "You are a reranking engine. Return only valid JSON with results sorted by relevance."
                ),
                Map.of(
                        "role", "user",
                        "content", buildChatPrompt(request)
                )
        ));
        payload.put("response_format", Map.of("type", "json_object"));
        return payload;
    }

    private String buildChatPrompt(RerankRequest request) {
        int topN = resolveTopN(request);
        StringBuilder prompt = new StringBuilder();
        prompt.append("Rank the documents by relevance to the query. ")
                .append("Return JSON only in this format: {\"results\":[{\"index\":0,\"relevance_score\":0.95}]}. ")
                .append("index must be the original zero-based document index. relevance_score must be between 0 and 1. ")
                .append("Return at most ").append(topN).append(" results.");
        if (StringUtils.hasText(request.instruct())) {
            prompt.append("\nInstruction: ").append(request.instruct());
        }
        prompt.append("\nQuery: ").append(request.query());
        prompt.append("\nDocuments:");
        for (int index = 0; index < request.documents().size(); index++) {
            prompt.append("\n[").append(index).append("] ").append(request.documents().get(index));
        }
        return prompt.toString();
    }

    private List<RerankItem> mapItems(RerankRequest request, JsonNode response) {
        JsonNode results = response.path("output").path("results");
        if (results.isMissingNode() || !results.isArray()) {
            results = response.path("results");
        }
        if (!results.isArray()) {
            throw new InvalidProviderResponseException("Rerank provider response does not contain a results array");
        }

        boolean returnDocuments = request.returnDocuments() == null || request.returnDocuments();
        List<RerankItem> items = new ArrayList<>();
        Set<Integer> seenIndexes = new HashSet<>();
        for (JsonNode item : results) {
            int index = item.path("index").asInt(-1);
            if (index < 0 || index >= request.documents().size()) {
                throw new InvalidProviderResponseException("Rerank provider returned an out-of-range document index: " + index);
            }
            if (!seenIndexes.add(index)) {
                throw new InvalidProviderResponseException("Rerank provider returned a duplicate document index: " + index);
            }
            JsonNode scoreNode = item.has("relevance_score") ? item.get("relevance_score") : item.get("score");
            if (scoreNode == null || !scoreNode.isNumber()) {
                throw new InvalidProviderResponseException("Rerank provider returned a missing or non-numeric relevance score");
            }
            double score = scoreNode.asDouble();
            if (!Double.isFinite(score) || score < 0.0D || score > 1.0D) {
                throw new InvalidProviderResponseException("Rerank provider returned an invalid relevance score: " + score);
            }
            String document = null;
            if (returnDocuments) {
                JsonNode documentNode = item.path("document");
                JsonNode documentTextNode = documentNode.path("text");
                if (documentNode.isTextual()) {
                    document = documentNode.asText();
                } else if (documentTextNode.isTextual()) {
                    document = documentTextNode.asText();
                } else if (index >= 0 && index < request.documents().size()) {
                    document = request.documents().get(index);
                }
            }
            items.add(new RerankItem(index, score, document));
        }

        items.sort(Comparator.comparing(RerankItem::relevanceScore).reversed());
        int topN = Math.min(resolveTopN(request), items.size());
        return List.copyOf(items.subList(0, topN));
    }

    private int resolveTopN(RerankRequest request) {
        return request.topN() == null
                ? request.documents().size()
                : Math.min(request.topN(), request.documents().size());
    }

    private List<RerankItem> mapChatItems(RerankRequest request, JsonNode response) {
        String content = response.path("choices").path(0).path("message").path("content").asText();
        try {
            JsonNode parsed = objectMapper.readTree(stripJsonFence(content));
            return mapItems(request, parsed);
        } catch (java.io.IOException exception) {
            throw new InvalidProviderResponseException("Rerank chat provider returned invalid JSON", exception);
        }
    }

    private String stripJsonFence(String content) {
        if (content == null) {
            return "{}";
        }
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            int firstLineEnd = trimmed.indexOf('\n');
            int lastFenceStart = trimmed.lastIndexOf("```");
            if (firstLineEnd >= 0 && lastFenceStart > firstLineEnd) {
                return trimmed.substring(firstLineEnd + 1, lastFenceStart).trim();
            }
        }
        return trimmed;
    }

    private Integer resolveTotalTokens(JsonNode response) {
        JsonNode usage = response.path("usage");
        if (!usage.isMissingNode()) {
            JsonNode totalTokens = usage.path("total_tokens");
            if (totalTokens.isInt()) {
                return totalTokens.asInt();
            }
        }
        return null;
    }

    private String resolveRequestId(JsonNode response) {
        JsonNode requestId = response.path("request_id");
        if (requestId.isMissingNode() || requestId.isNull()) {
            requestId = response.path("id");
        }
        if (!requestId.isMissingNode() && !requestId.isNull()) {
            return requestId.asText();
        }
        return null;
    }

    private String resolveModel(AiProviderRuntime runtime, String requestModel, String modality) {
        if (isMultimodal(modality)) {
            throw new IllegalArgumentException(
                    "Multimodal rerank is not supported by the current text-only request contract"
            );
        }
        return StringUtils.hasText(requestModel) ? requestModel : runtime.definition().rerank().model();
    }

    private boolean isMultimodal(String modality) {
        return StringUtils.hasText(modality) && ("multimodal".equalsIgnoreCase(modality) || "vl".equalsIgnoreCase(modality));
    }

    private AiProviderRuntime providerRuntime(String providerName) {
        AiRuntimeBundle bundle = runtimeConfigService.current();
        AiProviderRuntime runtime = bundle.providerRuntime(providerName);
        if (runtime == null) {
            throw new IllegalStateException("No configured AI provider runtime for " + providerName);
        }
        return runtime;
    }

    private record ProviderRerankResult(JsonNode response, List<RerankItem> items) {
    }
}
