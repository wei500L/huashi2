package com.huashi.eftransfer.ai.integration.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.huashi.eftransfer.ai.common.observability.AiProviderObservationService;
import com.huashi.eftransfer.ai.common.observability.ProviderRequestContextHolder;
import com.huashi.eftransfer.ai.common.observability.ResilientAiExecutor;
import com.huashi.eftransfer.ai.common.runtime.AiProviderRuntime;
import com.huashi.eftransfer.ai.common.runtime.AiRuntimeBundle;
import com.huashi.eftransfer.ai.common.runtime.AiRuntimeConfigService;
import com.huashi.eftransfer.shared.ai.RerankItem;
import com.huashi.eftransfer.shared.ai.RerankRequest;
import com.huashi.eftransfer.shared.ai.RerankResponse;
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
    private final ResilientAiExecutor resilientAiExecutor;
    private final AiProviderObservationService observationService;
    private final ProviderRequestContextHolder requestContextHolder;

    public QwenRerankClient(
            AiRuntimeConfigService runtimeConfigService,
            ResilientAiExecutor resilientAiExecutor,
            AiProviderObservationService observationService,
            ProviderRequestContextHolder requestContextHolder
    ) {
        this.runtimeConfigService = runtimeConfigService;
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
            JsonNode response = resilientAiExecutor.execute(runtime, "rerank", () -> runtime.rerankRestClient().post()
                    .uri("")
                    .body(buildPayload(request, model))
                    .retrieve()
                    .body(JsonNode.class));

            List<RerankItem> items = mapItems(request, response);
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

    private Map<String, Object> buildPayload(RerankRequest request, String model) {
        Integer topN = request.topN() != null ? request.topN() : request.documents().size();

        if (model.startsWith("qwen3-rerank")) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("model", model);
            payload.put("query", request.query());
            payload.put("documents", request.documents());
            payload.put("top_n", topN);
            if (StringUtils.hasText(request.instruct())) {
                payload.put("instruct", request.instruct());
            }
            return payload;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("input", Map.of(
                "query", request.query(),
                "documents", request.documents()
        ));
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("top_n", topN);
        parameters.put("return_documents", request.returnDocuments() == null || request.returnDocuments());
        if (StringUtils.hasText(request.instruct())) {
            parameters.put("instruct", request.instruct());
        }
        payload.put("parameters", parameters);
        return payload;
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
                document = documentNode.isTextual()
                        ? documentNode.asText()
                        : index >= 0 && index < request.documents().size() ? request.documents().get(index) : null;
            }
            items.add(new RerankItem(index, score, document));
        }

        items.sort(Comparator.comparing(RerankItem::relevanceScore).reversed());
        return items;
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
