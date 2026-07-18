package com.huashi.eftransfer.ai.integration.provider;

import com.huashi.eftransfer.ai.common.observability.AiProviderObservationService;
import com.huashi.eftransfer.ai.common.observability.ProviderRequestContextHolder;
import com.huashi.eftransfer.ai.common.observability.ResilientAiExecutor;
import com.huashi.eftransfer.ai.common.runtime.AiProviderRuntime;
import com.huashi.eftransfer.ai.common.runtime.AiRuntimeBundle;
import com.huashi.eftransfer.ai.common.runtime.AiRuntimeConfigService;
import com.huashi.eftransfer.shared.ai.EmbeddingBatchRequest;
import com.huashi.eftransfer.shared.ai.EmbeddingItem;
import com.huashi.eftransfer.shared.ai.EmbeddingRequest;
import com.huashi.eftransfer.shared.ai.EmbeddingResponse;
import com.huashi.eftransfer.shared.ai.TokenUsage;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

@Component
public class QwenEmbeddingProviderClient {

    private final AiRuntimeConfigService runtimeConfigService;
    private final ResilientAiExecutor resilientAiExecutor;
    private final AiProviderObservationService observationService;
    private final ProviderRequestContextHolder requestContextHolder;

    public QwenEmbeddingProviderClient(
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

    public EmbeddingResponse embed(String providerName, EmbeddingRequest request) {
        return embed(providerRuntime(providerName), providerName, request);
    }

    public EmbeddingResponse embed(AiProviderRuntime runtime, String providerName, EmbeddingRequest request) {
        return embedInternal(
                runtime,
                providerName,
                List.of(request.text()),
                request.model(),
                request.modality(),
                request.dimension()
        );
    }

    public EmbeddingResponse embedBatch(String providerName, EmbeddingBatchRequest request) {
        return embedInternal(providerRuntime(providerName), providerName, request.texts(), request.model(), request.modality(), request.dimension());
    }

    public EmbeddingResponse embedBatch(AiProviderRuntime runtime, String providerName, EmbeddingBatchRequest request) {
        return embedInternal(runtime, providerName, request.texts(), request.model(), request.modality(), request.dimension());
    }

    private EmbeddingResponse embedInternal(
            AiProviderRuntime runtime,
            String providerName,
            List<String> texts,
            String requestModel,
            String modality,
            Integer requestDimension
    ) {
        String provider = providerName;
        String model = resolveModel(runtime, requestModel, modality);
        int dimension = requestDimension != null ? requestDimension : runtime.definition().embedding().dimension();
        long startNanos = System.nanoTime();
        requestContextHolder.clear();

        try {
            org.springframework.ai.embedding.EmbeddingResponse response = resilientAiExecutor.execute(runtime, "embedding", () -> runtime.embeddingModel().call(
                    new org.springframework.ai.embedding.EmbeddingRequest(
                            texts,
                            OpenAiEmbeddingOptions.builder()
                                    .model(model)
                                    .dimensions(dimension)
                                    .build()
                    )
            ));

            ValidatedEmbeddings validated = validateEmbeddings(texts, response.getResults(), dimension);
            List<Embedding> embeddings = validated.embeddings();

            EmbeddingResponse embeddingResponse = new EmbeddingResponse(
                    provider,
                    model,
                    validated.dimension(),
                    requestContextHolder.getRequestId(),
                    toUsage(response.getMetadata().getUsage()),
                    mapItems(texts, embeddings)
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

    private ValidatedEmbeddings validateEmbeddings(List<String> texts, List<Embedding> embeddings, int expectedDimension) {
        if (embeddings == null || embeddings.size() != texts.size()) {
            int actualSize = embeddings == null ? 0 : embeddings.size();
            throw new IllegalStateException(
                    "Embedding provider returned %d vectors for %d inputs".formatted(actualSize, texts.size())
            );
        }
        List<Embedding> ordered = new ArrayList<>(Collections.nCopies(texts.size(), null));
        int actualDimension = -1;
        for (int responsePosition = 0; responsePosition < embeddings.size(); responsePosition++) {
            Embedding embedding = embeddings.get(responsePosition);
            if (embedding == null || embedding.getIndex() == null) {
                throw new IllegalStateException("Embedding provider returned a vector without an input index");
            }
            int index = embedding.getIndex();
            if (index < 0 || index >= texts.size()) {
                throw new IllegalStateException("Embedding provider returned an out-of-range input index: " + index);
            }
            if (ordered.set(index, embedding) != null) {
                throw new IllegalStateException("Embedding provider returned a duplicate input index: " + index);
            }
            float[] output = embedding.getOutput();
            if (output == null || output.length == 0) {
                throw new IllegalStateException("Embedding provider returned an empty vector at index " + index);
            }
            if (actualDimension < 0) {
                actualDimension = output.length;
            } else if (actualDimension != output.length) {
                throw new IllegalStateException("Embedding provider returned inconsistent vector dimensions");
            }
            for (float value : output) {
                if (!Float.isFinite(value)) {
                    throw new IllegalStateException("Embedding provider returned a non-finite value at index " + index);
                }
            }
        }
        if (actualDimension != expectedDimension) {
            throw new IllegalStateException(
                    "Embedding provider returned dimension %d but %d was requested".formatted(actualDimension, expectedDimension)
            );
        }
        return new ValidatedEmbeddings(List.copyOf(ordered), actualDimension);
    }

    private List<Double> toDoubles(float[] vector) {
        return IntStream.range(0, vector.length)
                .mapToObj(index -> BigDecimal.valueOf(Double.parseDouble(Float.toString(vector[index]))).doubleValue())
                .toList();
    }

    private String resolveModel(AiProviderRuntime runtime, String requestModel, String modality) {
        if (isMultimodal(modality)) {
            throw new IllegalArgumentException(
                    "Multimodal embedding is not supported by the current text-only request contract"
            );
        }
        return requestModel != null && !requestModel.isBlank() ? requestModel : runtime.definition().embedding().model();
    }

    private boolean isMultimodal(String modality) {
        return modality != null && ("multimodal".equalsIgnoreCase(modality) || "vl".equalsIgnoreCase(modality));
    }

    private AiProviderRuntime providerRuntime(String providerName) {
        AiRuntimeBundle bundle = runtimeConfigService.current();
        AiProviderRuntime runtime = bundle.providerRuntime(providerName);
        if (runtime == null) {
            throw new IllegalStateException("No configured AI provider runtime for " + providerName);
        }
        return runtime;
    }

    private TokenUsage toUsage(org.springframework.ai.chat.metadata.Usage usage) {
        if (usage == null) {
            return null;
        }
        return new TokenUsage(usage.getPromptTokens(), usage.getCompletionTokens(), usage.getTotalTokens());
    }

    private record ValidatedEmbeddings(List<Embedding> embeddings, int dimension) {
    }
}
