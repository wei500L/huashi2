package com.huashi.eftransfer.ai.modules.internal.service;

import com.huashi.eftransfer.ai.common.runtime.AiProviderRuntime;
import com.huashi.eftransfer.ai.common.runtime.AiRuntimeBundle;
import com.huashi.eftransfer.ai.common.runtime.AiRuntimeBundleFactory;
import com.huashi.eftransfer.ai.common.runtime.AiRuntimeConfigService;
import com.huashi.eftransfer.ai.integration.provider.QwenEmbeddingProviderClient;
import com.huashi.eftransfer.ai.integration.provider.QwenRerankClient;
import com.huashi.eftransfer.ai.modules.rag.support.EmbeddingTextSupport;
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
import com.huashi.eftransfer.shared.ai.config.AiOpsEmbeddingConfig;
import com.huashi.eftransfer.shared.ai.config.AiOpsRerankConfig;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class AiConfigProbeService {

    private static final String EMBEDDING_PROBE_QUERY = "What are false friends in English-French vocabulary learning?";
    private static final String EMBEDDING_PROBE_RELEVANT_DOCUMENT =
            "False friends are English-French word pairs that look similar but have different meanings.";
    private static final String EMBEDDING_PROBE_UNRELATED_DOCUMENT =
            "A classroom roster lists student names and assigned seat numbers.";
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
    private final AtomicReference<EmbeddingProbeState> embeddingProbeState = new AtomicReference<>();
    private final AtomicReference<RerankProbeState> rerankProbeState = new AtomicReference<>();

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
                    new EmbeddingBatchRequest(List.of(
                            EmbeddingTextSupport.toRetrievalQuery(EMBEDDING_PROBE_QUERY),
                            EMBEDDING_PROBE_RELEVANT_DOCUMENT,
                            EMBEDDING_PROBE_UNRELATED_DOCUMENT
                    ), null, null, expectedDimension)
            );
            int itemCount = response.items() == null ? 0 : response.items().size();
            Double relatedSimilarity = itemCount == 3
                    ? cosineSimilarity(response.items().get(0).embedding(), response.items().get(1).embedding())
                    : null;
            Double unrelatedSimilarity = itemCount == 3
                    ? cosineSimilarity(response.items().get(0).embedding(), response.items().get(2).embedding())
                    : null;
            Double similarityMargin = relatedSimilarity == null || unrelatedSimilarity == null
                    ? null
                    : relatedSimilarity - unrelatedSimilarity;
            boolean semanticOrdering = similarityMargin != null
                    && Double.isFinite(similarityMargin)
                    && similarityMargin > 0.0D;
            boolean ok = itemCount == 3
                    && Objects.equals(response.dimension(), expectedDimension)
                    && semanticOrdering;
            AdminAiEmbeddingProbeVO result = new AdminAiEmbeddingProbeVO(
                    ok,
                    ok ? "Embedding probe succeeded" : "Embedding probe did not separate relevant and unrelated passages",
                    response.provider(),
                    response.model(),
                    elapsedMillis(startedAt),
                    response.providerRequestId(),
                    OffsetDateTime.now(),
                    response.dimension(),
                    expectedDimension,
                    itemCount,
                    relatedSimilarity,
                    unrelatedSimilarity,
                    similarityMargin
            );
            embeddingProbeState.set(new EmbeddingProbeState(
                    providerName,
                    runtime.definition().embedding(),
                    ok,
                    result.testedAt()
            ));
            return result;
        } catch (RuntimeException exception) {
            AdminAiEmbeddingProbeVO result = new AdminAiEmbeddingProbeVO(
                    false,
                    summarizeFailure(exception),
                    providerName,
                    runtime.definition().embedding().model(),
                    elapsedMillis(startedAt),
                    null,
                    OffsetDateTime.now(),
                    null,
                    expectedDimension,
                    null,
                    null,
                    null,
                    null
            );
            embeddingProbeState.set(new EmbeddingProbeState(
                    providerName,
                    runtime.definition().embedding(),
                    false,
                    result.testedAt()
            ));
            return result;
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
                    new RerankRequest(null, RERANK_PROBE_QUERY, RERANK_PROBE_DOCUMENTS, RERANK_PROBE_DOCUMENTS.size(), true, null, null)
            );
            List<RerankItem> items = response.items() == null ? List.of() : response.items();
            boolean ordered = isOrdered(items);
            Integer topDocumentIndex = items.isEmpty() ? null : items.getFirst().index();
            Double topScore = items.isEmpty() ? null : items.getFirst().relevanceScore();
            boolean validScores = items.stream().allMatch(item -> Double.isFinite(item.relevanceScore())
                    && item.relevanceScore() >= 0.0D
                    && item.relevanceScore() <= 1.0D);
            boolean uniqueIndexes = items.stream().map(RerankItem::index).distinct().count() == items.size();
            boolean validIndexes = items.stream().allMatch(item -> item.index() >= 0 && item.index() < RERANK_PROBE_DOCUMENTS.size());
            boolean ok = !items.isEmpty()
                    && ordered
                    && validScores
                    && uniqueIndexes
                    && validIndexes
                    && Objects.equals(topDocumentIndex, 0);
            AdminAiRerankProbeVO result = new AdminAiRerankProbeVO(
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
            rerankProbeState.set(new RerankProbeState(
                    providerName,
                    runtime.definition().rerank(),
                    ok,
                    result.testedAt()
            ));
            return result;
        } catch (RuntimeException exception) {
            AdminAiRerankProbeVO result = new AdminAiRerankProbeVO(
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
            rerankProbeState.set(new RerankProbeState(
                    providerName,
                    runtime.definition().rerank(),
                    false,
                    result.testedAt()
            ));
            return result;
        }
    }

    public boolean isEmbeddingReady(String providerName, AiOpsEmbeddingConfig config) {
        EmbeddingProbeState state = embeddingProbeState.get();
        return state != null && state.matches(providerName, config) && state.isRecentAndSuccessful();
    }

    public boolean isRerankReady(String providerName, AiOpsRerankConfig config) {
        RerankProbeState state = rerankProbeState.get();
        return state != null && state.matches(providerName, config) && state.isRecentAndSuccessful();
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

    private Double cosineSimilarity(List<Double> left, List<Double> right) {
        if (left == null || right == null || left.isEmpty() || left.size() != right.size()) {
            return null;
        }
        double dot = 0.0D;
        double leftNorm = 0.0D;
        double rightNorm = 0.0D;
        for (int index = 0; index < left.size(); index++) {
            Double leftValue = left.get(index);
            Double rightValue = right.get(index);
            if (leftValue == null || rightValue == null
                    || !Double.isFinite(leftValue) || !Double.isFinite(rightValue)) {
                return null;
            }
            dot += leftValue * rightValue;
            leftNorm += leftValue * leftValue;
            rightNorm += rightValue * rightValue;
        }
        if (leftNorm == 0.0D || rightNorm == 0.0D) {
            return null;
        }
        double similarity = dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
        return Double.isFinite(similarity) ? similarity : null;
    }

    private String summarizeFailure(RuntimeException exception) {
        String message = exception.getMessage();
        return StringUtils.hasText(message) ? message : "Probe failed";
    }

    private long elapsedMillis(long startedAt) {
        return Math.max(1L, (System.nanoTime() - startedAt) / 1_000_000L);
    }

    private record EmbeddingProbeState(
            String providerName,
            AiOpsEmbeddingConfig config,
            boolean successful,
            OffsetDateTime checkedAt
    ) {
        private boolean matches(String currentProvider, AiOpsEmbeddingConfig currentConfig) {
            return Objects.equals(providerName, currentProvider)
                    && Objects.equals(config, currentConfig);
        }

        private boolean isRecentAndSuccessful() {
            return successful && checkedAt != null && checkedAt.isAfter(OffsetDateTime.now().minusMinutes(15));
        }
    }

    private record RerankProbeState(
            String providerName,
            AiOpsRerankConfig config,
            boolean successful,
            OffsetDateTime checkedAt
    ) {
        private boolean matches(String currentProvider, AiOpsRerankConfig currentConfig) {
            return Objects.equals(providerName, currentProvider)
                    && Objects.equals(config, currentConfig);
        }

        private boolean isRecentAndSuccessful() {
            return successful && checkedAt != null && checkedAt.isAfter(OffsetDateTime.now().minusMinutes(15));
        }
    }
}
