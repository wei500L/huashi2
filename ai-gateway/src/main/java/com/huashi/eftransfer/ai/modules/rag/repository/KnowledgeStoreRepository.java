package com.huashi.eftransfer.ai.modules.rag.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huashi.eftransfer.ai.modules.rag.support.KnowledgeChunkPayload;
import com.huashi.eftransfer.ai.modules.rag.support.KnowledgeDocumentPayload;
import com.huashi.eftransfer.ai.modules.rag.support.KnowledgeSearchCandidate;
import com.huashi.eftransfer.ai.modules.rag.support.PendingChunkEmbedding;
import com.huashi.eftransfer.ai.modules.rag.support.RagSearchFilter;
import com.huashi.eftransfer.shared.enums.EmbeddingStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class KnowledgeStoreRepository {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public KnowledgeStoreRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UpsertDocumentResult upsertDocument(KnowledgeDocumentPayload documentPayload, boolean forceReembed, String contentHash) {
        return upsertDocument(documentPayload, forceReembed, contentHash, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UpsertDocumentResult upsertDocument(
            KnowledgeDocumentPayload documentPayload,
            boolean forceReembed,
            String contentHash,
            Set<String> reconcileSourceTypes
    ) {
        Long documentId = jdbcTemplate.queryForObject(
                """
                        INSERT INTO knowledge_document (
                            source_type,
                            source_id,
                            title,
                            source_updated_at,
                            active,
                            content_hash,
                            metadata,
                            created_at,
                            updated_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, CAST(? AS jsonb), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        ON CONFLICT (source_type, source_id)
                        DO UPDATE SET
                            title = EXCLUDED.title,
                            source_updated_at = EXCLUDED.source_updated_at,
                            active = EXCLUDED.active,
                            content_hash = EXCLUDED.content_hash,
                            metadata = EXCLUDED.metadata,
                            updated_at = CURRENT_TIMESTAMP
                        RETURNING id
                        """,
                Long.class,
                documentPayload.sourceType(),
                documentPayload.sourceId(),
                documentPayload.title(),
                toTimestamp(documentPayload.sourceUpdatedAt()),
                documentPayload.active(),
                contentHash,
                writeJson(documentPayload.metadata())
        );

        if (documentId == null) {
            throw new IllegalStateException("Failed to upsert knowledge document");
        }

        List<PendingChunkEmbedding> pendingChunkEmbeddings = new ArrayList<>();
        Set<String> activeChunkKeys = new java.util.LinkedHashSet<>();

        for (KnowledgeChunkPayload chunkPayload : documentPayload.chunks()) {
            activeChunkKeys.add(chunkPayload.chunkKey());
            ChunkRecord existing = findChunk(documentId, chunkPayload.chunkKey());
            Map<String, Object> hashPayload = new LinkedHashMap<>();
            hashPayload.put("title", chunkPayload.title());
            hashPayload.put("content", chunkPayload.content());
            hashPayload.put("metadata", chunkPayload.metadata() == null ? Map.of() : chunkPayload.metadata());
            hashPayload.put("active", chunkPayload.active());
            String chunkContentHash = sha256(writeJson(hashPayload));
            boolean needsEmbedding = chunkPayload.active()
                    && (forceReembed
                    || existing == null
                    || !Objects.equals(chunkContentHash, existing.contentHash())
                    || !EmbeddingStatus.EMBEDDED.name().equals(existing.embeddingStatus()));
            String embeddingStatus = chunkPayload.active()
                    ? (needsEmbedding ? EmbeddingStatus.PENDING.name() : existing.embeddingStatus())
                    : (existing == null ? EmbeddingStatus.PENDING.name() : existing.embeddingStatus());
            OffsetDateTime embeddedAt = chunkPayload.active() && !needsEmbedding && existing != null ? existing.embeddedAt() : null;

            Long chunkId = jdbcTemplate.queryForObject(
                    """
                            INSERT INTO knowledge_chunk (
                                document_id,
                                chunk_key,
                                chunk_order,
                                source_type,
                                source_id,
                                title,
                                content,
                                metadata,
                                embedding_status,
                                embedded_at,
                                content_hash,
                                active,
                                created_at,
                                updated_at
                            )
                            VALUES (?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                            ON CONFLICT (document_id, chunk_key)
                            DO UPDATE SET
                                chunk_order = EXCLUDED.chunk_order,
                                source_type = EXCLUDED.source_type,
                                source_id = EXCLUDED.source_id,
                                title = EXCLUDED.title,
                                content = EXCLUDED.content,
                                metadata = EXCLUDED.metadata,
                                embedding_status = EXCLUDED.embedding_status,
                                embedded_at = EXCLUDED.embedded_at,
                                content_hash = EXCLUDED.content_hash,
                                active = EXCLUDED.active,
                                updated_at = CURRENT_TIMESTAMP
                            RETURNING id
                            """,
                    Long.class,
                    documentId,
                    chunkPayload.chunkKey(),
                    chunkPayload.chunkOrder(),
                    chunkPayload.sourceType(),
                    chunkPayload.sourceId(),
                    chunkPayload.title(),
                    chunkPayload.content(),
                    writeJson(chunkPayload.metadata()),
                    embeddingStatus,
                    toTimestamp(embeddedAt),
                    chunkContentHash,
                    chunkPayload.active()
            );

            if (chunkId == null) {
                throw new IllegalStateException("Failed to upsert knowledge chunk");
            }

            if (needsEmbedding) {
                pendingChunkEmbeddings.add(new PendingChunkEmbedding(chunkId, chunkPayload.content(), chunkContentHash));
            }
        }

        if (reconcileSourceTypes == null || !reconcileSourceTypes.isEmpty()) {
            deactivateMissingChunks(documentId, activeChunkKeys, reconcileSourceTypes);
        }
        if (!documentPayload.active()) {
            deactivateAllChunks(documentId);
        }

        return new UpsertDocumentResult(documentId, pendingChunkEmbeddings);
    }

    @Transactional
    public void replaceChunkEmbedding(Long chunkId, String model, int dimension, String contentHash, List<Double> embedding) {
        replaceChunkEmbeddings(List.of(new ChunkEmbeddingWrite(chunkId, model, dimension, contentHash, embedding)));
    }

    @Transactional
    public void replaceChunkEmbeddings(List<ChunkEmbeddingWrite> writes) {
        if (writes == null || writes.isEmpty()) {
            return;
        }

        validateEmbeddingWrites(writes);
        lockAndValidateCurrentChunks(writes);

        List<Long> chunkIds = writes.stream()
                .map(ChunkEmbeddingWrite::chunkId)
                .distinct()
                .toList();
        String placeholders = String.join(", ", java.util.Collections.nCopies(chunkIds.size(), "?"));
        jdbcTemplate.update(
                "UPDATE chunk_embedding SET is_current = FALSE WHERE is_current = TRUE AND chunk_id IN (" + placeholders + ")",
                chunkIds.toArray()
        );

        jdbcTemplate.batchUpdate(
                """
                        INSERT INTO chunk_embedding (
                            chunk_id,
                            embedding_model,
                            embedding_dimension,
                            embedding,
                            content_hash,
                            is_current,
                            embedded_at
                        )
                        VALUES (?, ?, ?, CAST(? AS vector), ?, TRUE, CURRENT_TIMESTAMP)
                        """,
                writes,
                writes.size(),
                (ps, write) -> {
                    ps.setLong(1, write.chunkId());
                    ps.setString(2, write.model());
                    ps.setInt(3, write.dimension());
                    ps.setString(4, toVectorParameter(write.embedding()));
                    ps.setString(5, write.contentHash());
                }
        );

        int[][] updatedChunks = jdbcTemplate.batchUpdate(
                "UPDATE knowledge_chunk SET embedding_status = ?, embedded_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                writes,
                writes.size(),
                (ps, write) -> {
                    ps.setString(1, EmbeddingStatus.EMBEDDED.name());
                    ps.setLong(2, write.chunkId());
                }
        );
        for (int[] batch : updatedChunks) {
            for (int updated : batch) {
                if (updated == 0) {
                    throw new IllegalStateException("Embedding write became stale before chunk status update");
                }
            }
        }
    }

    public void markChunkEmbeddingFailed(Long chunkId, String contentHash) {
        jdbcTemplate.update(
                "UPDATE knowledge_chunk SET embedding_status = ?, embedded_at = NULL, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND content_hash = ?",
                EmbeddingStatus.FAILED.name(),
                chunkId,
                contentHash
        );
    }

    @Transactional
    public List<PendingChunkEmbedding> claimFailedChunkEmbeddings(int limit) {
        int resolvedLimit = Math.max(1, Math.min(limit, 256));
        List<PendingChunkEmbedding> chunks = jdbcTemplate.query(
                """
                        SELECT kc.id, kc.content, kc.content_hash
                        FROM knowledge_chunk kc
                        JOIN knowledge_document kd ON kd.id = kc.document_id
                        WHERE kc.active = TRUE
                          AND kd.active = TRUE
                          AND (
                              kc.embedding_status = ?
                              OR (kc.embedding_status = ? AND kc.updated_at < CURRENT_TIMESTAMP - INTERVAL '10 minutes')
                          )
                        ORDER BY kc.updated_at, kc.id
                        LIMIT ?
                        FOR UPDATE OF kc SKIP LOCKED
                        """,
                (rs, rowNum) -> new PendingChunkEmbedding(
                        rs.getLong("id"),
                        rs.getString("content"),
                        rs.getString("content_hash")
                ),
                EmbeddingStatus.FAILED.name(),
                EmbeddingStatus.PENDING.name(),
                resolvedLimit
        );
        if (chunks.isEmpty()) {
            return chunks;
        }
        String placeholders = String.join(", ", java.util.Collections.nCopies(chunks.size(), "?"));
        jdbcTemplate.update(
                "UPDATE knowledge_chunk SET embedding_status = ?, updated_at = CURRENT_TIMESTAMP WHERE id IN (" + placeholders + ")",
                prepend(EmbeddingStatus.PENDING.name(), chunks.stream().map(PendingChunkEmbedding::chunkId).toList())
        );
        return chunks;
    }

    public Set<String> findLexicalPairSourceIdsForChunks(Collection<Long> chunkIds) {
        if (chunkIds == null || chunkIds.isEmpty()) {
            return Set.of();
        }
        String placeholders = String.join(", ", java.util.Collections.nCopies(chunkIds.size(), "?"));
        List<String> sourceIds = jdbcTemplate.queryForList(
                """
                        SELECT DISTINCT CASE
                            WHEN metadata ->> 'lexicalPairId' IS NOT NULL THEN metadata ->> 'lexicalPairId'
                            WHEN source_type = 'LEXICAL_PAIR' THEN source_id
                            ELSE NULL
                        END AS lexical_pair_source_id
                        FROM knowledge_chunk
                        WHERE id IN (""" + placeholders + ")",
                String.class,
                chunkIds.toArray()
        );
        return sourceIds.stream()
                .filter(Objects::nonNull)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    public List<LexicalPairEmbeddingState> listLexicalPairEmbeddingStates(Collection<String> lexicalPairSourceIds) {
        if (lexicalPairSourceIds == null || lexicalPairSourceIds.isEmpty()) {
            return List.of();
        }
        List<String> sourceIds = lexicalPairSourceIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (sourceIds.isEmpty()) {
            return List.of();
        }
        String placeholders = sourceIds.stream().map(ignored -> "?").collect(Collectors.joining(", "));
        return jdbcTemplate.query(
                """
                        SELECT kd.source_id,
                               CASE
                                   WHEN COALESCE(SUM(CASE WHEN kc.active THEN 1 ELSE 0 END), 0) = 0 THEN ?
                                   WHEN COALESCE(SUM(CASE WHEN kc.active AND kc.embedding_status = ? THEN 1 ELSE 0 END), 0) > 0 THEN ?
                                   WHEN COALESCE(SUM(CASE WHEN kc.active AND kc.embedding_status = ? THEN 1 ELSE 0 END), 0) > 0 THEN ?
                                   ELSE ?
                               END AS aggregate_embedding_status,
                               MAX(CASE WHEN kc.active AND kc.embedding_status = ? THEN kc.embedded_at END) AS last_embedded_at
                        FROM knowledge_document kd
                        LEFT JOIN knowledge_chunk kc ON kc.document_id = kd.id
                        WHERE kd.source_type = ?
                          AND kd.source_id IN (""" + placeholders + ") " +
                        """
                        GROUP BY kd.source_id
                        ORDER BY kd.source_id
                        """,
                ps -> {
                    int index = 1;
                    ps.setString(index++, EmbeddingStatus.PENDING.name());
                    ps.setString(index++, EmbeddingStatus.FAILED.name());
                    ps.setString(index++, EmbeddingStatus.FAILED.name());
                    ps.setString(index++, EmbeddingStatus.PENDING.name());
                    ps.setString(index++, EmbeddingStatus.PENDING.name());
                    ps.setString(index++, EmbeddingStatus.EMBEDDED.name());
                    ps.setString(index++, EmbeddingStatus.EMBEDDED.name());
                    ps.setString(index++, "LEXICAL_PAIR");
                    for (String sourceId : sourceIds) {
                        ps.setString(index++, sourceId);
                    }
                },
                (rs, rowNum) -> new LexicalPairEmbeddingState(
                        rs.getString("source_id"),
                        rs.getString("aggregate_embedding_status"),
                        toOffsetDateTime(rs.getTimestamp("last_embedded_at"))
                )
        );
    }

    public List<KnowledgeSearchCandidate> similaritySearch(
            List<Double> embedding,
            String embeddingModel,
            RagSearchFilter filter,
            int limit,
            int hnswEfSearch
    ) {
        StringBuilder sql = new StringBuilder("""
                SELECT kc.id,
                       kc.source_type,
                       kc.source_id,
                       kc.title,
                       kc.content,
                       kc.metadata::text AS metadata_json,
                       (1 - (ce.embedding <=> CAST(? AS vector))) AS similarity_score
                FROM knowledge_chunk kc
                JOIN knowledge_document kd ON kd.id = kc.document_id
                JOIN chunk_embedding ce ON ce.chunk_id = kc.id AND ce.is_current = TRUE
                WHERE kc.active = TRUE
                  AND kd.active = TRUE
                  AND kc.embedding_status = 'EMBEDDED'
                  AND ce.content_hash = kc.content_hash
                  AND ce.embedding_model = ?
                  AND ce.embedding_dimension = ?
                """);
        List<Object> params = new ArrayList<>();
        params.add(toVectorParameter(embedding));
        params.add(embeddingModel);
        params.add(embedding.size());

        appendInClause(sql, params, "kc.source_type", filter.sourceTypes());
        appendInClause(sql, params, "kc.source_id", filter.sourceIds());

        sql.append("""
                 ORDER BY ce.embedding <=> CAST(? AS vector), kc.id
                 LIMIT ?
                """);
        params.add(toVectorParameter(embedding));
        params.add(limit);

        return jdbcTemplate.execute((Connection connection) -> executeSimilaritySearch(connection, sql.toString(), params, hnswEfSearch));
    }

    public void deactivateDocumentsNotIn(String documentSourceType, Set<String> seenSourceIds) {
        if (seenSourceIds == null || seenSourceIds.isEmpty()) {
            jdbcTemplate.update("UPDATE knowledge_document SET active = FALSE, updated_at = CURRENT_TIMESTAMP WHERE source_type = ?", documentSourceType);
        } else {
            String placeholders = String.join(", ", java.util.Collections.nCopies(seenSourceIds.size(), "?"));
            List<Object> params = new ArrayList<>();
            params.add(documentSourceType);
            params.addAll(seenSourceIds);
            jdbcTemplate.update(
                    "UPDATE knowledge_document SET active = FALSE, updated_at = CURRENT_TIMESTAMP WHERE source_type = ? AND source_id NOT IN (" + placeholders + ")",
                    params.toArray()
            );
        }
        deactivateChildrenForInactiveDocuments(documentSourceType);
    }

    public void deactivateDocumentsBySourceIds(String documentSourceType, Set<String> targetSourceIds, Set<String> seenSourceIds) {
        if (targetSourceIds == null || targetSourceIds.isEmpty()) {
            return;
        }
        StringBuilder sql = new StringBuilder("UPDATE knowledge_document SET active = FALSE, updated_at = CURRENT_TIMESTAMP WHERE source_type = ? AND source_id IN (");
        sql.append(String.join(", ", java.util.Collections.nCopies(targetSourceIds.size(), "?"))).append(")");
        List<Object> params = new ArrayList<>();
        params.add(documentSourceType);
        params.addAll(targetSourceIds);
        if (seenSourceIds != null && !seenSourceIds.isEmpty()) {
            sql.append(" AND source_id NOT IN (")
                    .append(String.join(", ", java.util.Collections.nCopies(seenSourceIds.size(), "?")))
                    .append(")");
            params.addAll(seenSourceIds);
        }
        jdbcTemplate.update(sql.toString(), params.toArray());
        deactivateChildrenForInactiveDocuments(documentSourceType);
    }

    public boolean hasKnowledgeDocuments() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM knowledge_document WHERE active = TRUE",
                Integer.class
        );
        return count != null && count > 0;
    }

    public KnowledgeIndexCoverage getIndexCoverage(String embeddingModel, int embeddingDimension) {
        KnowledgeIndexCoverage coverage = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*) AS active_chunk_count,
                               COUNT(*) FILTER (
                                   WHERE kc.embedding_status = 'EMBEDDED'
                                     AND ce.content_hash = kc.content_hash
                                     AND ce.embedding_model = ?
                                     AND ce.embedding_dimension = ?
                               ) AS searchable_chunk_count
                        FROM knowledge_chunk kc
                        JOIN knowledge_document kd ON kd.id = kc.document_id
                        LEFT JOIN chunk_embedding ce ON ce.chunk_id = kc.id AND ce.is_current = TRUE
                        WHERE kc.active = TRUE
                          AND kd.active = TRUE
                        """,
                (rs, rowNum) -> new KnowledgeIndexCoverage(
                        rs.getInt("active_chunk_count"),
                        rs.getInt("searchable_chunk_count")
                ),
                embeddingModel,
                embeddingDimension
        );
        return coverage == null ? new KnowledgeIndexCoverage(0, 0) : coverage;
    }

    private ChunkRecord findChunk(Long documentId, String chunkKey) {
        List<ChunkRecord> chunks = jdbcTemplate.query(
                """
                        SELECT id, content_hash, embedding_status, embedded_at
                        FROM knowledge_chunk
                        WHERE document_id = ? AND chunk_key = ?
                        LIMIT 1
                        """,
                (rs, rowNum) -> new ChunkRecord(
                        rs.getLong("id"),
                        rs.getString("content_hash"),
                        rs.getString("embedding_status"),
                        toOffsetDateTime(rs.getTimestamp("embedded_at"))
                ),
                documentId,
                chunkKey
        );
        return chunks.isEmpty() ? null : chunks.getFirst();
    }

    private void deactivateMissingChunks(Long documentId, Set<String> activeChunkKeys, Set<String> sourceTypes) {
        StringBuilder scope = new StringBuilder("document_id = ?");
        List<Object> scopeParams = new ArrayList<>();
        scopeParams.add(documentId);
        if (sourceTypes != null && !sourceTypes.isEmpty()) {
            scope.append(" AND source_type IN (")
                    .append(String.join(", ", java.util.Collections.nCopies(sourceTypes.size(), "?")))
                    .append(")");
            scopeParams.addAll(sourceTypes);
        }
        StringBuilder missingScope = new StringBuilder(scope);
        List<Object> missingParams = new ArrayList<>(scopeParams);
        if (activeChunkKeys != null && !activeChunkKeys.isEmpty()) {
            missingScope.append(" AND chunk_key NOT IN (")
                    .append(String.join(", ", java.util.Collections.nCopies(activeChunkKeys.size(), "?")))
                    .append(")");
            missingParams.addAll(activeChunkKeys);
        }
        jdbcTemplate.update(
                "UPDATE knowledge_chunk SET active = FALSE, updated_at = CURRENT_TIMESTAMP WHERE " + missingScope,
                missingParams.toArray()
        );
        jdbcTemplate.update(
                "UPDATE chunk_embedding SET is_current = FALSE WHERE chunk_id IN ("
                        + "SELECT id FROM knowledge_chunk WHERE " + scope + " AND active = FALSE"
                        + ") AND is_current = TRUE",
                scopeParams.toArray()
        );
    }

    private void deactivateAllChunks(Long documentId) {
        jdbcTemplate.update(
                "UPDATE knowledge_chunk SET active = FALSE, updated_at = CURRENT_TIMESTAMP WHERE document_id = ?",
                documentId
        );
        jdbcTemplate.update(
                "UPDATE chunk_embedding SET is_current = FALSE WHERE chunk_id IN (SELECT id FROM knowledge_chunk WHERE document_id = ?) AND is_current = TRUE",
                documentId
        );
    }

    private void deactivateChildrenForInactiveDocuments(String documentSourceType) {
        jdbcTemplate.update(
                """
                        UPDATE knowledge_chunk kc
                        SET active = FALSE, updated_at = CURRENT_TIMESTAMP
                        WHERE kc.document_id IN (
                            SELECT id FROM knowledge_document WHERE source_type = ? AND active = FALSE
                        )
                        """,
                documentSourceType
        );
        jdbcTemplate.update(
                """
                        UPDATE chunk_embedding
                        SET is_current = FALSE
                        WHERE chunk_id IN (
                            SELECT kc.id
                            FROM knowledge_chunk kc
                            JOIN knowledge_document kd ON kd.id = kc.document_id
                            WHERE kd.source_type = ? AND kd.active = FALSE
                        ) AND is_current = TRUE
                        """,
                documentSourceType
        );
    }

    private KnowledgeSearchCandidate mapSearchCandidate(ResultSet rs, int rowNum) throws SQLException {
        return new KnowledgeSearchCandidate(
                rs.getLong("id"),
                rs.getString("source_type"),
                rs.getString("source_id"),
                rs.getString("title"),
                rs.getString("content"),
                readJsonMap(rs.getString("metadata_json")),
                rs.getDouble("similarity_score")
        );
    }

    private void appendInClause(StringBuilder sql, List<Object> params, String columnName, Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        sql.append(" AND ").append(columnName).append(" IN (")
                .append(String.join(", ", java.util.Collections.nCopies(values.size(), "?")))
                .append(")");
        params.addAll(values);
    }

    private Map<String, Object> readJsonMap(String value) {
        if (value == null || value.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(value, MAP_TYPE);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to deserialize JSON payload", ex);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize JSON payload", ex);
        }
    }

    private Timestamp toTimestamp(OffsetDateTime offsetDateTime) {
        return offsetDateTime == null ? null : Timestamp.from(offsetDateTime.toInstant());
    }

    private OffsetDateTime toOffsetDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant().atOffset(ZoneOffset.UTC);
    }

    private List<KnowledgeSearchCandidate> executeSimilaritySearch(
            Connection connection,
            String sql,
            List<Object> params,
            int hnswEfSearch
    ) throws SQLException {
        if (hnswEfSearch <= 0) {
            throw new SQLException("hnsw.ef_search must be greater than 0");
        }
        SQLException primaryFailure = null;
        try (Statement setEfSearch = connection.createStatement()) {
            setEfSearch.execute("SET hnsw.ef_search = " + hnswEfSearch);
        }
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindParams(statement, params);
            try (ResultSet rs = statement.executeQuery()) {
                List<KnowledgeSearchCandidate> results = new ArrayList<>();
                int rowNum = 0;
                while (rs.next()) {
                    results.add(mapSearchCandidate(rs, rowNum++));
                }
                return results;
            }
        } catch (SQLException ex) {
            primaryFailure = ex;
            throw ex;
        } finally {
            try (Statement resetStatement = connection.createStatement()) {
                resetStatement.execute("RESET hnsw.ef_search");
            } catch (SQLException resetEx) {
                if (primaryFailure != null) {
                    primaryFailure.addSuppressed(resetEx);
                } else {
                    throw resetEx;
                }
            }
        }
    }

    private void bindParams(PreparedStatement statement, List<Object> params) throws SQLException {
        for (int index = 0; index < params.size(); index++) {
            Object value = params.get(index);
            if (value instanceof Integer intValue) {
                statement.setInt(index + 1, intValue);
            } else if (value instanceof Long longValue) {
                statement.setLong(index + 1, longValue);
            } else {
                statement.setObject(index + 1, value);
            }
        }
    }

    private Object[] prepend(Object first, List<?> remaining) {
        Object[] values = new Object[remaining.size() + 1];
        values[0] = first;
        for (int index = 0; index < remaining.size(); index++) {
            values[index + 1] = remaining.get(index);
        }
        return values;
    }

    private void validateEmbeddingWrites(List<ChunkEmbeddingWrite> writes) {
        Set<Long> chunkIds = new java.util.HashSet<>();
        for (ChunkEmbeddingWrite write : writes) {
            if (write == null || write.chunkId() == null) {
                throw new IllegalArgumentException("Embedding write chunkId is required");
            }
            if (!chunkIds.add(write.chunkId())) {
                throw new IllegalArgumentException("Embedding batch contains duplicate chunkId " + write.chunkId());
            }
            if (write.model() == null || write.model().isBlank()) {
                throw new IllegalArgumentException("Embedding write model is required");
            }
            if (write.dimension() <= 0) {
                throw new IllegalArgumentException("Embedding write dimension must be greater than zero");
            }
            if (write.embedding() == null || write.embedding().size() != write.dimension()) {
                int actual = write.embedding() == null ? 0 : write.embedding().size();
                throw new IllegalArgumentException(
                        "Embedding write dimension mismatch: declared=%d actual=%d".formatted(write.dimension(), actual)
                );
            }
            if (write.embedding().stream().anyMatch(value -> value == null || !Double.isFinite(value))) {
                throw new IllegalArgumentException("Embedding write contains a non-finite value");
            }
            if (write.contentHash() == null || write.contentHash().isBlank()) {
                throw new IllegalArgumentException("Embedding write contentHash is required");
            }
        }
    }

    private void lockAndValidateCurrentChunks(List<ChunkEmbeddingWrite> writes) {
        String placeholders = String.join(", ", java.util.Collections.nCopies(writes.size(), "?"));
        Map<Long, ChunkWriteState> states = jdbcTemplate.query(
                "SELECT id, content_hash, active FROM knowledge_chunk WHERE id IN (" + placeholders + ") FOR UPDATE",
                (rs, rowNum) -> new ChunkWriteState(
                        rs.getLong("id"),
                        rs.getString("content_hash"),
                        rs.getBoolean("active")
                ),
                writes.stream().map(ChunkEmbeddingWrite::chunkId).toArray()
        ).stream().collect(Collectors.toMap(ChunkWriteState::chunkId, state -> state));
        for (ChunkEmbeddingWrite write : writes) {
            ChunkWriteState state = states.get(write.chunkId());
            if (state == null || !state.active() || !Objects.equals(state.contentHash(), write.contentHash())) {
                throw new IllegalStateException("Embedding write is stale for chunk " + write.chunkId());
            }
        }
    }

    private String toVectorParameter(List<Double> embedding) {
        StringBuilder builder = new StringBuilder("[");
        for (int index = 0; index < embedding.size(); index++) {
            if (index > 0) {
                builder.append(',');
            }
            builder.append(Double.toString(embedding.get(index)));
        }
        return builder.append(']').toString();
    }

    private String sha256(String value) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                builder.append(String.format(Locale.ROOT, "%02x", b));
            }
            return builder.toString();
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", ex);
        }
    }

    private record ChunkRecord(Long id, String contentHash, String embeddingStatus, OffsetDateTime embeddedAt) {
    }

    private record ChunkWriteState(Long chunkId, String contentHash, boolean active) {
    }

    public record LexicalPairEmbeddingState(
            String sourceId,
            String embeddingStatus,
            OffsetDateTime lastEmbeddedAt
    ) {
    }

    public record ChunkEmbeddingWrite(
            Long chunkId,
            String model,
            int dimension,
            String contentHash,
            List<Double> embedding
    ) {
    }

    public record UpsertDocumentResult(Long documentId, List<PendingChunkEmbedding> pendingChunkEmbeddings) {
    }

    public record KnowledgeIndexCoverage(int activeChunkCount, int searchableChunkCount) {
        public boolean isComplete() {
            return activeChunkCount > 0 && activeChunkCount == searchableChunkCount;
        }
    }
}
