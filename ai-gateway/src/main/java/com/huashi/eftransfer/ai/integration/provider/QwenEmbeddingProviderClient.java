package com.huashi.eftransfer.ai.integration.provider;

import com.huashi.eftransfer.ai.common.config.AiProviderProperties;
import com.huashi.eftransfer.ai.common.observability.AiProviderObservationService;
import com.huashi.eftransfer.ai.common.observability.ProviderRequestContextHolder;
import com.huashi.eftransfer.ai.common.observability.ResilientAiExecutor;
import com.huashi.eftransfer.shared.ai.EmbeddingBatchRequest;
import com.huashi.eftransfer.shared.ai.EmbeddingItem;
import com.huashi.eftransfer.shared.ai.EmbeddingRequest;
import com.huashi.eftransfer.shared.ai.EmbeddingResponse;
import com.huashi.eftransfer.shared.ai.TokenUsage;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;

@Component
public class QwenEmbeddingProviderClient {

    private final AiProviderProperties providerProperties;
    private final EmbeddingModel embeddingModel;
    private final ResilientAiExecutor resilientAiExecutor;
    private final AiProviderObservationService observationService;
    private final ProviderRequestContextHolder requestContextHolder;

    public QwenEmbeddingProviderClient(
            AiProviderProperties providerProperties,
            EmbeddingModel qwenEmbeddingModel,
            ResilientAiExecutor resilientAiExecutor,
            AiProviderObservationService observationService,
            ProviderRequestContextHolder requestContextHolder
    ) {
        this.providerProperties = providerProperties;
        this.embeddingModel = qwenEmbeddingModel;
        this.resilientAiExecutor = resilientAiExecutor;
        this.observationService = observationService;
        this.requestContextHolder = requestContextHolder;
    }

    public EmbeddingResponse embed(EmbeddingRequest request) {
        return embedInternal(
                List.of(request.text()),
                request.model(),
                request.dimension()
        );
    }

    public EmbeddingResponse embedBatch(EmbeddingBatchRequest request) {
        return embedInternal(request.texts(), request.model(), request.dimension());
    }

    private EmbeddingResponse embedInternal(List<String> texts, String requestModel, Integer requestDimension) {
        String provider = "qwen";
        String model = resolveModel(requestModel);
        int dimension = requestDimension != null ? requestDimension : defaultEmbedding().getDimension();
        long startNanos = System.nanoTime();
        requestContextHolder.clear();

        try {
            org.springframework.ai.embedding.EmbeddingResponse response = resilientAiExecutor.execute("embedding", () -> embeddingModel.call(
                    new org.springframework.ai.embedding.EmbeddingRequest(
                            texts,
                            OpenAiEmbeddingOptions.builder()
                                    .model(model)
                                    .dimensions(dimension)
                                    .build()
                    )
            ));

            EmbeddingResponse embeddingResponse = new EmbeddingResponse(
                    provider,
                    model,
                    dimension,
                    requestContextHolder.getRequestId(),
                    toUsage(response.getMetadata().getUsage()),
                    mapItems(texts, response.getResults())
            );
            observationService.recordSuccess(
                    "embedding",
                    provider,
                    model,
                    startNanos,
                    embeddingResponse.providerRequestId(),
                    embeddingResponse.usage()
            );
            return embeddingResponse;
        } catch (Exception ex) {
            throw observationService.recordFailure("embedding", provider, model, startNanos, ex);
        }
    }

    private List<EmbeddingItem> mapItems(List<String> texts, List<Embedding> embeddings) {
        return IntStream.range(0, embeddings.size())
                .mapToObj(index -> new EmbeddingItem(
                        index,
                        texts.get(index),
                        toDoubles(embeddings.get(index).getOutput())
                ))
                .toList();
    }

    private List<Double> toDoubles(float[] vector) {
        return IntStream.range(0, vector.length)
                .mapToObj(index -> BigDecimal.valueOf(Double.parseDouble(Float.toString(vector[index]))).doubleValue())
                .toList();
    }

    private String resolveModel(String requestModel) {
        return requestModel != null && !requestModel.isBlank() ? requestModel : defaultEmbedding().getModel();
    }

    private AiProviderProperties.EmbeddingProperties defaultEmbedding() {
        return providerProperties.getProviderProperties("qwen").getEmbedding();
    }

    private TokenUsage toUsage(org.springframework.ai.chat.metadata.Usage usage) {
        if (usage == null) {
            return null;
        }
        return new TokenUsage(usage.getPromptTokens(), usage.getCompletionTokens(), usage.getTotalTokens());
    }
}
