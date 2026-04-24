package com.huashi.eftransfer.ai.integration.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
        String model = resolveModel(runtime, request.model());
        long startNanos = System.nanoTime();
        requestContextHolder.clear();

        try {
            boolean chatRerank = AiOpsProtocols.OPENAI_CHAT_RERANK.equals(runtime.definition().rerank().protocol());
            JsonNode response = resilientAiExecutor.execute(runtime, "rerank", () -> runtime.rerankRestClient().post()
                    .uri(chatRerank ? "/v1/chat/completions" : "/v1/rerank")
                    .body(chatRerank ? buildChatPayload(request, model) : buildRerankPayload(request, model))
                    .retrieve()
                    .body(JsonNode.class));

            List<RerankItem> items = chatRerank ? mapChatItems(request, response) : mapItems(request, response);
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
        Integer topN = request.topN() != null ? request.topN() : request.documents().size();
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
        Integer topN = request.topN() != null ? request.topN() : request.documents().size();
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

        boolean returnDocuments = request.returnDocuments() == null || request.returnDocuments();
        List<RerankItem> items = new ArrayList<>();
        for (JsonNode item : results) {
            int index = item.path("index").asInt(-1);
            double score = item.path("relevance_score").asDouble(item.path("score").asDouble(0.0D));
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
        return items;
    }

    private List<RerankItem> mapChatItems(RerankRequest request, JsonNode response) throws java.io.IOException {
        String content = response.path("choices").path(0).path("message").path("content").asText();
        JsonNode parsed = objectMapper.readTree(stripJsonFence(content));
        return mapItems(request, parsed);
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

    private String resolveModel(AiProviderRuntime runtime, String requestModel) {
        return StringUtils.hasText(requestModel) ? requestModel : runtime.definition().rerank().model();
    }

    private AiProviderRuntime providerRuntime(String providerName) {
        AiRuntimeBundle bundle = runtimeConfigService.current();
        AiProviderRuntime runtime = bundle.providerRuntime(providerName);
        if (runtime == null) {
            throw new IllegalStateException("No configured AI provider runtime for " + providerName);
        }
        return runtime;
    }
}
