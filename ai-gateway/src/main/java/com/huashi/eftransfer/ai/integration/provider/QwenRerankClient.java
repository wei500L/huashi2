package com.huashi.eftransfer.ai.integration.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.huashi.eftransfer.ai.common.config.AiProviderProperties;
import com.huashi.eftransfer.ai.integration.provider.dto.RerankRequest;
import com.huashi.eftransfer.ai.integration.provider.dto.RerankResponse;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.exception.BusinessException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Component
public class QwenRerankClient implements RerankClient {

    private final RestClient rerankRestClient;
    private final AiProviderProperties providerProperties;

    public QwenRerankClient(RestClient rerankRestClient, AiProviderProperties providerProperties) {
        this.rerankRestClient = rerankRestClient;
        this.providerProperties = providerProperties;
    }

    @Override
    public RerankResponse rerank(RerankRequest request) {
        if (!StringUtils.hasText(providerProperties.getRerankUrl())) {
            throw new BusinessException(ResultCode.AI_PROVIDER_UNAVAILABLE, "Rerank URL is not configured");
        }

        try {
            JsonNode response = rerankRestClient.post()
                    .uri(providerProperties.getRerankUrl())
                    .body(Map.of(
                            "model", StringUtils.hasText(request.model()) ? request.model() : providerProperties.getRerankModel(),
                            "query", request.query(),
                            "documents", request.documents(),
                            "top_n", request.topN()
                    ))
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null) {
                throw new BusinessException(ResultCode.AI_PROVIDER_UNAVAILABLE, "Empty rerank response");
            }

            JsonNode items = response.path("results");
            if (items.isMissingNode() || !items.isArray()) {
                items = response.path("data");
            }

            List<RerankResponse.RerankItem> reranked = new ArrayList<>();
            for (JsonNode item : items) {
                int index = item.path("index").asInt(-1);
                double score = item.path("relevance_score").asDouble(item.path("score").asDouble(0.0D));
                String document = index >= 0 && index < request.documents().size() ? request.documents().get(index) : "";
                reranked.add(new RerankResponse.RerankItem(index, score, document));
            }

            reranked.sort(Comparator.comparing(RerankResponse.RerankItem::score).reversed());
            return new RerankResponse(
                    providerProperties.getProvider(),
                    StringUtils.hasText(request.model()) ? request.model() : providerProperties.getRerankModel(),
                    reranked
            );
        } catch (RestClientException ex) {
            throw new BusinessException(ResultCode.AI_PROVIDER_UNAVAILABLE, "Failed to call rerank provider: " + ex.getMessage());
        }
    }
}
