package com.huashi.eftransfer.ai.modules.rag.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huashi.eftransfer.ai.common.runtime.AiRuntimeConfigService;
import com.huashi.eftransfer.ai.integration.provider.AiProviderRegistry;
import com.huashi.eftransfer.ai.modules.rag.integration.AppServerKnowledgeClient;
import com.huashi.eftransfer.ai.modules.rag.repository.IngestionJobRepository;
import com.huashi.eftransfer.ai.modules.rag.repository.KnowledgeStoreRepository;
import com.huashi.eftransfer.ai.modules.rag.support.KnowledgeChunkPayload;
import com.huashi.eftransfer.ai.modules.rag.support.KnowledgeDocumentPayload;
import com.huashi.eftransfer.ai.modules.rag.support.KnowledgeSourceTypes;
import com.huashi.eftransfer.ai.modules.rag.support.PendingChunkEmbedding;
import com.huashi.eftransfer.ai.modules.rag.support.ReindexMode;
import com.huashi.eftransfer.shared.ai.EmbeddingBatchRequest;
import com.huashi.eftransfer.shared.ai.EmbeddingResponse;
import com.huashi.eftransfer.shared.ai.LexicalKnowledgeExampleItem;
import com.huashi.eftransfer.shared.ai.LexicalKnowledgeExportItem;
import com.huashi.eftransfer.shared.ai.LexicalKnowledgeExportPageResponse;
import com.huashi.eftransfer.shared.ai.LexicalKnowledgeSenseItem;
import com.huashi.eftransfer.shared.ai.RagReindexJobResponse;
import com.huashi.eftransfer.shared.ai.RagReindexRequest;
import com.huashi.eftransfer.shared.ai.RagReindexResponse;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.exception.BusinessException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class KnowledgeIngestionService {

    private static final String JOB_TYPE = "KNOWLEDGE_REINDEX";
    private static final TypeReference<List<SeedKnowledgeEntry>> SEED_TYPE = new TypeReference<>() {
    };

    private final IngestionJobRepository ingestionJobRepository;
    private final KnowledgeStoreRepository knowledgeStoreRepository;
    private final AppServerKnowledgeClient appServerKnowledgeClient;
    private final AiProviderRegistry aiProviderRegistry;
    private final AiRuntimeConfigService runtimeConfigService;
    private final TaskExecutor ragTaskExecutor;
    private final ObjectMapper objectMapper;

    public KnowledgeIngestionService(
            IngestionJobRepository ingestionJobRepository,
            KnowledgeStoreRepository knowledgeStoreRepository,
            AppServerKnowledgeClient appServerKnowledgeClient,
            AiProviderRegistry aiProviderRegistry,
            AiRuntimeConfigService runtimeConfigService,
            TaskExecutor ragTaskExecutor,
            ObjectMapper objectMapper
    ) {
        this.ingestionJobRepository = ingestionJobRepository;
        this.knowledgeStoreRepository = knowledgeStoreRepository;
        this.appServerKnowledgeClient = appServerKnowledgeClient;
        this.aiProviderRegistry = aiProviderRegistry;
        this.runtimeConfigService = runtimeConfigService;
        this.ragTaskExecutor = ragTaskExecutor;
        this.objectMapper = objectMapper;
    }

    public RagReindexResponse submit(RagReindexRequest request) {
        PreparedJob job = prepareJob(request);
        ragTaskExecutor.execute(() -> runJob(job, false));
        return new RagReindexResponse(job.jobId(), "PENDING");
    }

    public RagReindexJobResponse submitAndAwait(RagReindexRequest request) {
        PreparedJob job = prepareJob(request);
        runJob(job, true);
        return getJob(job.jobId());
    }

    public RagReindexJobResponse getJob(Long jobId) {
        var job = ingestionJobRepository.findById(jobId);
        if (job == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "RAG reindex job was not found", 404);
        }
        return new RagReindexJobResponse(
                job.id(),
                job.jobType(),
                job.mode(),
                job.status(),
                job.sourceTypes(),
                job.sourceIds(),
                job.lastCursor(),
                job.lastSourceUpdatedAt(),
                job.finishedAt(),
                job.stats(),
                job.errorMessage()
        );
    }

    private PreparedJob prepareJob(RagReindexRequest request) {
        ReindexMode mode = parseMode(request.mode());
        Set<String> requestedSourceTypes = parseSourceTypes(request.sourceTypes());
        Set<String> requestedSourceIds = normalizeIds(request.sourceIds());
        Long jobId = ingestionJobRepository.createPendingJob(JOB_TYPE, mode.name(), requestedSourceTypes, requestedSourceIds);
        return new PreparedJob(
                jobId,
                mode,
                requestedSourceTypes,
                requestedSourceIds,
                Boolean.TRUE.equals(request.forceReembed())
        );
    }

    private void runJob(PreparedJob job, boolean rethrowFailure) {
        StatsAccumulator stats = new StatsAccumulator();
        OffsetDateTime watermark = null;
        try {
            ingestionJobRepository.markRunning(job.jobId());

            if (!java.util.Collections.disjoint(job.requestedSourceTypes(), KnowledgeSourceTypes.APP_SERVER_SOURCE_TYPES)) {
                watermark = syncLexicalKnowledge(
                        job.jobId(),
                        job.mode(),
                        job.requestedSourceTypes(),
                        job.requestedSourceIds(),
                        job.forceReembed(),
                        stats
                );
            }
            if (!java.util.Collections.disjoint(job.requestedSourceTypes(), KnowledgeSourceTypes.SEED_SOURCE_TYPES)) {
                syncSeedKnowledge(
                        job.jobId(),
                        job.mode(),
                        job.requestedSourceTypes(),
                        job.requestedSourceIds(),
                        job.forceReembed(),
                        stats
                );
            }

            ingestionJobRepository.markSucceeded(job.jobId(), watermark, stats.toMap());
        } catch (Exception ex) {
            ingestionJobRepository.markFailed(job.jobId(), ex.getMessage(), watermark, stats.toMap());
            if (rethrowFailure) {
                if (ex instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }
                throw new IllegalStateException(ex.getMessage(), ex);
            }
        }
    }

    private OffsetDateTime syncLexicalKnowledge(
            Long jobId,
            ReindexMode mode,
            Set<String> requestedSourceTypes,
            Set<String> requestedSourceIds,
            boolean forceReembed,
            StatsAccumulator stats
    ) {
        OffsetDateTime updatedSince = mode == ReindexMode.FULL
                ? null
                : ingestionJobRepository.findLatestSuccessfulWatermark(JOB_TYPE, KnowledgeSourceTypes.LEXICAL_PAIR);

        List<String> lexicalSourceIds = toLexicalSourceIds(requestedSourceIds);
        Set<String> seenDocumentIds = new LinkedHashSet<>();
        String cursor = null;
        OffsetDateTime watermark = updatedSince;

        do {
            LexicalKnowledgeExportPageResponse response = appServerKnowledgeClient.exportLexicalPairs(
                    updatedSince,
                    cursor,
                    runtimeConfigService.current().config().rag().ingestion().exportPageSize(),
                    lexicalSourceIds
            );

            List<PendingChunkEmbedding> pendingChunkEmbeddings = new ArrayList<>();
            for (LexicalKnowledgeExportItem item : response.items()) {
                seenDocumentIds.add(String.valueOf(item.lexicalPairId()));
                KnowledgeDocumentPayload documentPayload = toLexicalDocument(item, requestedSourceTypes);
                KnowledgeStoreRepository.UpsertDocumentResult result = knowledgeStoreRepository.upsertDocument(
                        documentPayload,
                        forceReembed,
                        documentHash(documentPayload)
                );
                pendingChunkEmbeddings.addAll(result.pendingChunkEmbeddings());
                stats.documentsProcessed++;
                stats.chunksProcessed += documentPayload.chunks().size();
                if (item.sourceUpdatedAt() != null && (watermark == null || item.sourceUpdatedAt().isAfter(watermark))) {
                    watermark = item.sourceUpdatedAt();
                }
            }

            embedPendingChunks(pendingChunkEmbeddings, stats);
            cursor = response.nextCursor();
            ingestionJobRepository.updateProgress(jobId, cursor, watermark, stats.toMap());
        } while (cursor != null);

        if (mode == ReindexMode.FULL) {
            if (lexicalSourceIds.isEmpty()) {
                knowledgeStoreRepository.deactivateDocumentsNotIn(KnowledgeSourceTypes.LEXICAL_PAIR, seenDocumentIds);
            } else {
                knowledgeStoreRepository.deactivateDocumentsBySourceIds(
                        KnowledgeSourceTypes.LEXICAL_PAIR,
                        new LinkedHashSet<>(lexicalSourceIds),
                        seenDocumentIds
                );
            }
        }

        return watermark;
    }

    private void syncSeedKnowledge(
            Long jobId,
            ReindexMode mode,
            Set<String> requestedSourceTypes,
            Set<String> requestedSourceIds,
            boolean forceReembed,
            StatsAccumulator stats
    ) throws IOException {
        List<SeedKnowledgeEntry> seedEntries = objectMapper.readValue(
                new ClassPathResource("rag/seed-knowledge.json").getInputStream(),
                SEED_TYPE
        );

        Map<String, Set<String>> seenBySourceType = new LinkedHashMap<>();
        List<PendingChunkEmbedding> pendingChunkEmbeddings = new ArrayList<>();

        for (SeedKnowledgeEntry seedEntry : seedEntries) {
            String sourceType = normalizeSourceType(seedEntry.sourceType());
            if (!requestedSourceTypes.contains(sourceType)) {
                continue;
            }
            if (!requestedSourceIds.isEmpty() && !requestedSourceIds.contains(seedEntry.sourceId())) {
                continue;
            }
            seenBySourceType.computeIfAbsent(sourceType, key -> new LinkedHashSet<>()).add(seedEntry.sourceId());

            KnowledgeChunkPayload chunkPayload = new KnowledgeChunkPayload(
                    "explanation:%s:%s".formatted(sourceType.toLowerCase(Locale.ROOT), seedEntry.sourceId()),
                    0,
                    sourceType,
                    seedEntry.sourceId(),
                    seedEntry.title(),
                    seedEntry.content(),
                    seedEntry.metadata() == null ? Map.of() : seedEntry.metadata(),
                    true
            );
            KnowledgeDocumentPayload documentPayload = new KnowledgeDocumentPayload(
                    sourceType,
                    seedEntry.sourceId(),
                    seedEntry.title(),
                    OffsetDateTime.now(ZoneOffset.UTC),
                    true,
                    seedEntry.metadata() == null ? Map.of() : seedEntry.metadata(),
                    List.of(chunkPayload)
            );
            KnowledgeStoreRepository.UpsertDocumentResult result = knowledgeStoreRepository.upsertDocument(
                    documentPayload,
                    forceReembed,
                    documentHash(documentPayload)
            );
            pendingChunkEmbeddings.addAll(result.pendingChunkEmbeddings());
            stats.documentsProcessed++;
            stats.chunksProcessed++;
        }

        embedPendingChunks(pendingChunkEmbeddings, stats);
        ingestionJobRepository.updateProgress(jobId, null, null, stats.toMap());

        if (mode == ReindexMode.FULL) {
            for (String sourceType : requestedSourceTypes) {
                if (!KnowledgeSourceTypes.SEED_SOURCE_TYPES.contains(sourceType)) {
                    continue;
                }
                Set<String> seenIds = seenBySourceType.getOrDefault(sourceType, Set.of());
                if (requestedSourceIds.isEmpty()) {
                    knowledgeStoreRepository.deactivateDocumentsNotIn(sourceType, seenIds);
                } else {
                    Set<String> targetIds = requestedSourceIds.stream()
                            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
                    knowledgeStoreRepository.deactivateDocumentsBySourceIds(sourceType, targetIds, seenIds);
                }
            }
        }
    }

    private void embedPendingChunks(List<PendingChunkEmbedding> pendingChunkEmbeddings, StatsAccumulator stats) {
        if (pendingChunkEmbeddings.isEmpty()) {
            return;
        }
        int batchSize = Math.max(1, runtimeConfigService.current().config().rag().ingestion().embeddingBatchSize());
        for (int start = 0; start < pendingChunkEmbeddings.size(); start += batchSize) {
            List<PendingChunkEmbedding> batch = pendingChunkEmbeddings.subList(start, Math.min(start + batchSize, pendingChunkEmbeddings.size()));
            try {
                writeEmbeddingBatch(batch, stats);
            } catch (Exception ex) {
                if (batch.size() == 1) {
                    knowledgeStoreRepository.markChunkEmbeddingFailed(batch.getFirst().chunkId());
                    stats.embeddingFailures++;
                } else {
                    for (PendingChunkEmbedding chunk : batch) {
                        try {
                            writeEmbeddingBatch(List.of(chunk), stats);
                        } catch (Exception nestedEx) {
                            knowledgeStoreRepository.markChunkEmbeddingFailed(chunk.chunkId());
                            stats.embeddingFailures++;
                        }
                    }
                }
            }
        }
    }

    private void writeEmbeddingBatch(List<PendingChunkEmbedding> batch, StatsAccumulator stats) {
        EmbeddingResponse response = aiProviderRegistry.embedBatch(new EmbeddingBatchRequest(
                batch.stream().map(PendingChunkEmbedding::content).toList(),
                null,
                null
        ));
        if (response.items() == null || response.items().size() != batch.size()) {
            throw new IllegalStateException("Unexpected embedding batch size");
        }
        for (int index = 0; index < batch.size(); index++) {
            PendingChunkEmbedding chunk = batch.get(index);
            knowledgeStoreRepository.replaceChunkEmbedding(
                    chunk.chunkId(),
                    response.model(),
                    response.dimension(),
                    chunk.contentHash(),
                    response.items().get(index).embedding()
            );
            stats.embeddedChunks++;
        }
    }

    private KnowledgeDocumentPayload toLexicalDocument(LexicalKnowledgeExportItem item, Set<String> requestedSourceTypes) {
        boolean active = Boolean.TRUE.equals(item.active()) && "READY".equalsIgnoreCase(item.knowledgeStatus());
        String documentSourceId = String.valueOf(item.lexicalPairId());
        List<KnowledgeChunkPayload> chunks = new ArrayList<>();

        if (requestedSourceTypes.contains(KnowledgeSourceTypes.LEXICAL_PAIR)) {
            Map<String, Object> pairMetadata = new LinkedHashMap<>();
            pairMetadata.put("chunkKind", "LEXICAL_PAIR");
            pairMetadata.put("lexicalPairId", item.lexicalPairId());
            pairMetadata.put("lexicalPairType", item.lexicalPairType());
            pairMetadata.put("semanticOverlapScore", item.semanticOverlapScore());
            pairMetadata.put("falseFriendRisk", item.falseFriendRisk());
            pairMetadata.put("defaultContextSupport", item.defaultContextSupport());
            pairMetadata.put("difficultyLevel", item.difficultyLevel());
            pairMetadata.put("tags", item.tags() == null ? List.of() : item.tags());
            chunks.add(new KnowledgeChunkPayload(
                    "pair:%s".formatted(documentSourceId),
                    0,
                    KnowledgeSourceTypes.LEXICAL_PAIR,
                    documentSourceId,
                    "%s / %s".formatted(item.englishWord(), item.frenchWord()),
                    buildPairContent(item),
                    pairMetadata,
                    active
            ));
        }

        int order = 1;
        if (item.senses() != null) {
            for (LexicalKnowledgeSenseItem sense : item.senses()) {
                if (requestedSourceTypes.contains(KnowledgeSourceTypes.LEXICAL_SENSE)) {
                    Map<String, Object> senseMetadata = new LinkedHashMap<>();
                    senseMetadata.put("chunkKind", "SENSE");
                    senseMetadata.put("lexicalPairId", item.lexicalPairId());
                    senseMetadata.put("senseId", sense.senseId());
                    senseMetadata.put("lexicalPairType", item.lexicalPairType());
                    chunks.add(new KnowledgeChunkPayload(
                            "sense:%s".formatted(sense.senseId()),
                            order++,
                            KnowledgeSourceTypes.LEXICAL_SENSE,
                            String.valueOf(sense.senseId()),
                            "%s / %s - Sense %s".formatted(item.englishWord(), item.frenchWord(), sense.sortOrder()),
                            buildSenseContent(item, sense),
                            senseMetadata,
                            active
                    ));
                }
                if (sense.examples() == null) {
                    continue;
                }
                for (LexicalKnowledgeExampleItem example : sense.examples()) {
                    if (!requestedSourceTypes.contains(KnowledgeSourceTypes.LEXICAL_EXAMPLE)) {
                        continue;
                    }
                    Map<String, Object> exampleMetadata = new LinkedHashMap<>();
                    exampleMetadata.put("chunkKind", "EXAMPLE");
                    exampleMetadata.put("lexicalPairId", item.lexicalPairId());
                    exampleMetadata.put("senseId", sense.senseId());
                    exampleMetadata.put("exampleId", example.exampleId());
                    exampleMetadata.put("contextSupportLevel", example.contextSupportLevel());
                    exampleMetadata.put("lexicalPairType", item.lexicalPairType());
                    chunks.add(new KnowledgeChunkPayload(
                            "example:%s".formatted(example.exampleId()),
                            order++,
                            KnowledgeSourceTypes.LEXICAL_EXAMPLE,
                            String.valueOf(example.exampleId()),
                            "%s / %s - Example %s".formatted(item.englishWord(), item.frenchWord(), example.sortOrder()),
                            buildExampleContent(item, sense, example),
                            exampleMetadata,
                            active
                    ));
                }
            }
        }

        Map<String, Object> documentMetadata = new LinkedHashMap<>();
        documentMetadata.put("lexicalPairId", item.lexicalPairId());
        documentMetadata.put("englishWord", item.englishWord());
        documentMetadata.put("frenchWord", item.frenchWord());
        documentMetadata.put("lexicalPairType", item.lexicalPairType());
        documentMetadata.put("knowledgeStatus", item.knowledgeStatus());
        documentMetadata.put("tags", item.tags() == null ? List.of() : item.tags());

        return new KnowledgeDocumentPayload(
                KnowledgeSourceTypes.LEXICAL_PAIR,
                documentSourceId,
                "%s / %s".formatted(item.englishWord(), item.frenchWord()),
                item.sourceUpdatedAt(),
                active,
                documentMetadata,
                chunks
        );
    }

    private String buildPairContent(LexicalKnowledgeExportItem item) {
        return """
                English word: %s
                French word: %s
                Chinese gloss: %s
                Lexical pair type: %s
                Semantic overlap score: %s
                False friend risk: %s
                Default context support: %s
                Difficulty level: %s
                Teacher notes: %s
                Source: %s
                Tags: %s
                """.formatted(
                item.englishWord(),
                item.frenchWord(),
                item.chineseGloss(),
                item.lexicalPairType(),
                item.semanticOverlapScore(),
                item.falseFriendRisk(),
                item.defaultContextSupport(),
                item.difficultyLevel(),
                item.notes() == null ? "" : item.notes(),
                item.source() == null ? "" : item.source(),
                item.tags() == null ? "" : String.join(", ", item.tags())
        ).trim();
    }

    private String buildSenseContent(LexicalKnowledgeExportItem item, LexicalKnowledgeSenseItem sense) {
        return """
                Lexical pair: %s / %s
                Sense order: %s
                English definition: %s
                French definition: %s
                Chinese definition: %s
                """.formatted(
                item.englishWord(),
                item.frenchWord(),
                sense.sortOrder(),
                sense.englishDefinition(),
                sense.frenchDefinition(),
                sense.chineseDefinition()
        ).trim();
    }

    private String buildExampleContent(
            LexicalKnowledgeExportItem item,
            LexicalKnowledgeSenseItem sense,
            LexicalKnowledgeExampleItem example
    ) {
        return """
                Lexical pair: %s / %s
                Sense: %s
                English example: %s
                French example: %s
                Chinese translation: %s
                Context support level: %s
                Example source: %s
                """.formatted(
                item.englishWord(),
                item.frenchWord(),
                sense.englishDefinition(),
                example.englishExample(),
                example.frenchExample(),
                example.chineseTranslation(),
                example.contextSupportLevel(),
                example.source() == null ? "" : example.source()
        ).trim();
    }

    private ReindexMode parseMode(String value) {
        try {
            return ReindexMode.fromValue(value);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "Unsupported reindex mode: " + value, 400);
        }
    }

    private Set<String> parseSourceTypes(List<String> sourceTypes) {
        Set<String> normalized = KnowledgeSourceTypes.normalizeRequestedTypes(sourceTypes == null ? Set.of() : new LinkedHashSet<>(sourceTypes));
        if (!KnowledgeSourceTypes.ALL_SOURCE_TYPES.containsAll(normalized)) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "Unsupported sourceTypes in reindex request", 400);
        }
        return normalized;
    }

    private Set<String> normalizeIds(List<String> sourceIds) {
        if (sourceIds == null || sourceIds.isEmpty()) {
            return Set.of();
        }
        return sourceIds.stream()
                .filter(id -> id != null && !id.isBlank())
                .map(String::trim)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private List<String> toLexicalSourceIds(Set<String> sourceIds) {
        if (sourceIds == null || sourceIds.isEmpty()) {
            return List.of();
        }
        List<String> lexicalIds = new ArrayList<>();
        for (String sourceId : sourceIds) {
            try {
                Long.parseLong(sourceId);
                lexicalIds.add(sourceId);
            } catch (NumberFormatException ignored) {
                // Non-numeric ids are for seed knowledge sources and should be ignored here.
            }
        }
        return lexicalIds;
    }

    private String normalizeSourceType(String sourceType) {
        return sourceType == null ? null : sourceType.trim().toUpperCase(Locale.ROOT);
    }

    private String documentHash(KnowledgeDocumentPayload payload) {
        Map<String, Object> hashPayload = new LinkedHashMap<>();
        hashPayload.put("sourceType", payload.sourceType());
        hashPayload.put("sourceId", payload.sourceId());
        hashPayload.put("title", payload.title());
        hashPayload.put("sourceUpdatedAt", payload.sourceUpdatedAt());
        hashPayload.put("active", payload.active());
        hashPayload.put("metadata", payload.metadata());
        hashPayload.put("chunkKeys", payload.chunks().stream().map(KnowledgeChunkPayload::chunkKey).toList());
        return sha256(writeJson(hashPayload));
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize knowledge payload", ex);
        }
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                builder.append(String.format(Locale.ROOT, "%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", ex);
        }
    }

    private record SeedKnowledgeEntry(
            String sourceType,
            String sourceId,
            String title,
            String content,
            Map<String, Object> metadata
    ) {
    }

    private record PreparedJob(
            Long jobId,
            ReindexMode mode,
            Set<String> requestedSourceTypes,
            Set<String> requestedSourceIds,
            boolean forceReembed
    ) {
    }

    private static final class StatsAccumulator {
        private int documentsProcessed;
        private int chunksProcessed;
        private int embeddedChunks;
        private int embeddingFailures;

        private Map<String, Object> toMap() {
            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("documentsProcessed", documentsProcessed);
            stats.put("chunksProcessed", chunksProcessed);
            stats.put("embeddedChunks", embeddedChunks);
            stats.put("embeddingFailures", embeddingFailures);
            return stats;
        }
    }
}
