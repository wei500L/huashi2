package com.huashi.eftransfer.ai.modules.internal.service;

import com.huashi.eftransfer.ai.common.runtime.AiProviderRuntime;
import com.huashi.eftransfer.ai.common.runtime.AiRuntimeBundle;
import com.huashi.eftransfer.ai.common.runtime.AiRuntimeBundleFactory;
import com.huashi.eftransfer.ai.common.runtime.AiRuntimeConfigService;
import com.huashi.eftransfer.ai.integration.provider.QwenEmbeddingProviderClient;
import com.huashi.eftransfer.ai.integration.provider.QwenRerankClient;
import com.huashi.eftransfer.shared.ai.AdminAiEmbeddingProbeVO;
import com.huashi.eftransfer.shared.ai.AdminAiRerankProbeVO;
import com.huashi.eftransfer.shared.ai.EmbeddingBatchRequest;
import com.huashi.eftransfer.shared.ai.EmbeddingResponse;
import com.huashi.eftransfer.shared.ai.RerankItem;
import com.huashi.eftransfer.shared.ai.RerankRequest;
import com.huashi.eftransfer.shared.ai.RerankResponse;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigIssue;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigPayload;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigValidationResponse;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class AiConfigProbeService {

    private static final String EMBEDDING_PROBE_TEXT = "semantic probe: false friend classroom guidance";
    private static final String RERANK_PROBE_QUERY = "Which passage best explains false friends in vocabulary learning?";
    private static final List<String> RERANK_PROBE_DOCUMENTS = List.of(
            "False friends are word pairs that look similar across languages but differ in meaning.",
            "A class roster lists students and seat numbers.",
            "Embedding vectors transform text into numeric representations for retrieval."
    );

    private final AiRuntimeConfigService runtimeConfigService;
    private final AiRuntimeBundleFactory bundleFactory;
    private final QwenEmbeddingProviderClient embeddingProviderClient;
    private final QwenRerankClient rerankClient;

    public AiConfigProbeService(
            AiRuntimeConfigService runtimeConfigService,
            AiRuntimeBundleFactory bundleFactory,
            QwenEmbeddingProviderClient embeddingProviderClient,
            QwenRerankClient rerankClient
    ) {
        this.runtimeConfigService = runtimeConfigService;
        this.bundleFactory = bundleFactory;
        this.embeddingProviderClient = embeddingProviderClient;
        this.rerankClient = rerankClient;
    }

    public AdminAiEmbeddingProbeVO probeEmbedding(AiOpsConfigPayload payload) {
        AiRuntimeBundle bundle = buildProbeBundle(payload);
        String providerName = payload.provider().activeProvider();
        AiProviderRuntime runtime = requireActiveRuntime(bundle, providerName);
        Integer expectedDimension = runtime.definition().embedding().dimension();
        long startedAt = System.nanoTime();
        try {
            EmbeddingResponse response = embeddingProviderClient.embedBatch(
                    runtime,
                    providerName,
                    new EmbeddingBatchRequest(List.of(EMBEDDING_PROBE_TEXT), null, expectedDimension)
            );
            int itemCount = response.items() == null ? 0 : response.items().size();
            boolean ok = itemCount == 1 && Objects.equals(response.dimension(), expectedDimension);
            return new AdminAiEmbeddingProbeVO(
                    ok,
                    ok ? "Embedding probe succeeded" : "Embedding probe returned unexpected shape",
                    response.provider(),
                    response.model(),
                    elapsedMillis(startedAt),
                    response.providerRequestId(),
                    OffsetDateTime.now(),
                    response.dimension(),
                    expectedDimension,
                    itemCount
            );
        } catch (RuntimeException exception) {
            return new AdminAiEmbeddingProbeVO(
                    false,
                    summarizeFailure(exception),
                    providerName,
                    runtime.definition().embedding().model(),
                    elapsedMillis(startedAt),
                    null,
                    OffsetDateTime.now(),
                    null,
                    expectedDimension,
                    null
            );
        }
    }

    public AdminAiRerankProbeVO probeRerank(AiOpsConfigPayload payload) {
        AiRuntimeBundle bundle = buildProbeBundle(payload);
        String providerName = payload.provider().activeProvider();
        AiProviderRuntime runtime = requireActiveRuntime(bundle, providerName);
        long startedAt = System.nanoTime();
        try {
            RerankResponse response = rerankClient.rerank(
                    runtime,
                    providerName,
                    new RerankRequest(null, RERANK_PROBE_QUERY, RERANK_PROBE_DOCUMENTS, RERANK_PROBE_DOCUMENTS.size(), true, null)
            );
            List<RerankItem> items = response.items() == null ? List.of() : response.items();
            boolean ordered = isOrdered(items);
            Integer topDocumentIndex = items.isEmpty() ? null : items.getFirst().index();
            Double topScore = items.isEmpty() ? null : items.getFirst().relevanceScore();
            boolean ok = !items.isEmpty() && ordered && Objects.equals(topDocumentIndex, 0);
            return new AdminAiRerankProbeVO(
                    ok,
                    ok ? "Rerank probe succeeded" : "Rerank probe returned unexpected ranking",
                    response.provider(),
                    response.model(),
                    elapsedMillis(startedAt),
                    response.providerRequestId(),
                    OffsetDateTime.now(),
                    RERANK_PROBE_DOCUMENTS.size(),
                    items.size(),
                    ordered,
                    topDocumentIndex,
                    topScore
            );
        } catch (RuntimeException exception) {
            return new AdminAiRerankProbeVO(
                    false,
                    summarizeFailure(exception),
                    providerName,
                    runtime.definition().rerank().model(),
                    elapsedMillis(startedAt),
                    null,
                    OffsetDateTime.now(),
                    RERANK_PROBE_DOCUMENTS.size(),
                    null,
                    false,
                    null,
                    null
            );
        }
    }

    private AiRuntimeBundle buildProbeBundle(AiOpsConfigPayload payload) {
        AiOpsConfigValidationResponse validation = runtimeConfigService.validate(payload);
        if (!validation.valid()) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, firstIssueMessage(validation), 400);
        }
        return bundleFactory.build(payload, "PROBE", runtimeConfigService.current().version());
    }

    private AiProviderRuntime requireActiveRuntime(AiRuntimeBundle bundle, String providerName) {
        AiProviderRuntime runtime = bundle.providerRuntime(providerName);
        if (runtime == null) {
            throw new BusinessException(ResultCode.AI_PROVIDER_UNAVAILABLE, "No configured AI provider runtime found for " + providerName, 503);
        }
        return runtime;
    }

    private String firstIssueMessage(AiOpsConfigValidationResponse validation) {
        if (validation.issues() == null || validation.issues().isEmpty()) {
            return "AI ops config validation failed";
        }
        AiOpsConfigIssue issue = validation.issues().getFirst();
        return issue.field() + ": " + issue.message();
    }

    private boolean isOrdered(List<RerankItem> items) {
        if (items.isEmpty()) {
            return false;
        }
        List<RerankItem> sorted = items.stream()
                .sorted(Comparator.comparing(RerankItem::relevanceScore).reversed())
                .toList();
        return items.equals(sorted);
    }

    private String summarizeFailure(RuntimeException exception) {
        String message = exception.getMessage();
        return StringUtils.hasText(message) ? message : "Probe failed";
    }

    private long elapsedMillis(long startedAt) {
        return Math.max(1L, (System.nanoTime() - startedAt) / 1_000_000L);
    }
}
