package com.huashi.eftransfer.ai.modules.rag.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huashi.eftransfer.ai.modules.rag.support.IngestionJobRecord;
import com.huashi.eftransfer.ai.modules.rag.support.IngestionJobStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Repository
public class IngestionJobRepository {

    private static final TypeReference<List<String>> LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public IngestionJobRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public Long createPendingJob(String jobType, String mode, Set<String> sourceTypes, Set<String> sourceIds) {
        return jdbcTemplate.queryForObject(
                """
                        INSERT INTO ingestion_job (
                            job_type,
                            mode,
                            status,
                            source_types,
                            source_ids,
                            stats,
                            created_at,
                            updated_at
                        )
                        VALUES (?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb), '{}'::jsonb, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        RETURNING id
                        """,
                Long.class,
                jobType,
                mode,
                IngestionJobStatus.PENDING.name(),
                writeJson(sourceTypes == null ? List.of() : sourceTypes),
                writeJson(sourceIds == null ? List.of() : sourceIds)
        );
    }

    public void markRunning(Long jobId) {
        jdbcTemplate.update(
                "UPDATE ingestion_job SET status = ?, started_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                IngestionJobStatus.RUNNING.name(),
                jobId
        );
    }

    public void updateProgress(Long jobId, String lastCursor, OffsetDateTime lastSourceUpdatedAt, Map<String, Object> stats) {
        jdbcTemplate.update(
                """
                        UPDATE ingestion_job
                        SET last_cursor = ?,
                            last_source_updated_at = ?,
                            stats = CAST(? AS jsonb),
                            updated_at = CURRENT_TIMESTAMP
                        WHERE id = ?
                        """,
                lastCursor,
                toTimestamp(lastSourceUpdatedAt),
                writeJson(stats),
                jobId
        );
    }

    public void markSucceeded(Long jobId, OffsetDateTime lastSourceUpdatedAt, Map<String, Object> stats) {
        jdbcTemplate.update(
                """
                        UPDATE ingestion_job
                        SET status = ?,
                            finished_at = CURRENT_TIMESTAMP,
                            last_source_updated_at = ?,
                            stats = CAST(? AS jsonb),
                            error_message = NULL,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE id = ?
                        """,
                IngestionJobStatus.SUCCEEDED.name(),
                toTimestamp(lastSourceUpdatedAt),
                writeJson(stats),
                jobId
        );
    }

    public void markFailed(Long jobId, String errorMessage, OffsetDateTime lastSourceUpdatedAt, Map<String, Object> stats) {
        jdbcTemplate.update(
                """
                        UPDATE ingestion_job
                        SET status = ?,
                            finished_at = CURRENT_TIMESTAMP,
                            last_source_updated_at = ?,
                            stats = CAST(? AS jsonb),
                            error_message = ?,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE id = ?
                        """,
                IngestionJobStatus.FAILED.name(),
                toTimestamp(lastSourceUpdatedAt),
                writeJson(stats),
                errorMessage,
                jobId
        );
    }

    public OffsetDateTime findLatestSuccessfulWatermark(String jobType, String sourceType) {
        List<IngestionJobRecord> jobs = jdbcTemplate.query(
                """
                        SELECT id, job_type, mode, status, source_types::text AS source_types_json, source_ids::text AS source_ids_json,
                               last_cursor, last_source_updated_at, finished_at, stats::text AS stats_json
                        FROM ingestion_job
                        WHERE status = ? AND job_type = ?
                        ORDER BY finished_at DESC NULLS LAST, id DESC
                        LIMIT 20
                        """,
                this::mapJobRecord,
                IngestionJobStatus.SUCCEEDED.name(),
                jobType
        );

        for (IngestionJobRecord job : jobs) {
            List<String> sourceTypes = job.sourceTypes();
            if (sourceTypes == null || sourceTypes.isEmpty() || sourceTypes.contains(sourceType)) {
                return job.lastSourceUpdatedAt();
            }
        }
        return null;
    }

    public IngestionJobRecord findLatestSuccessfulJob(String jobType) {
        List<IngestionJobRecord> jobs = jdbcTemplate.query(
                """
                        SELECT id, job_type, mode, status, source_types::text AS source_types_json, source_ids::text AS source_ids_json,
                               last_cursor, last_source_updated_at, finished_at, stats::text AS stats_json
                        FROM ingestion_job
                        WHERE status = ? AND job_type = ?
                        ORDER BY finished_at DESC NULLS LAST, id DESC
                        LIMIT 1
                        """,
                this::mapJobRecord,
                IngestionJobStatus.SUCCEEDED.name(),
                jobType
        );
        return jobs.isEmpty() ? null : jobs.getFirst();
    }

    private IngestionJobRecord mapJobRecord(ResultSet rs, int rowNum) throws SQLException {
        return new IngestionJobRecord(
                rs.getLong("id"),
                rs.getString("job_type"),
                rs.getString("mode"),
                rs.getString("status"),
                readList(rs.getString("source_types_json")),
                readList(rs.getString("source_ids_json")),
                rs.getString("last_cursor"),
                toOffsetDateTime(rs.getTimestamp("last_source_updated_at")),
                toOffsetDateTime(rs.getTimestamp("finished_at")),
                readMap(rs.getString("stats_json"))
        );
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize ingestion job JSON", ex);
        }
    }

    private List<String> readList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(value, LIST_TYPE);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to deserialize ingestion job list JSON", ex);
        }
    }

    private Map<String, Object> readMap(String value) {
        if (value == null || value.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(value, MAP_TYPE);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to deserialize ingestion job stats JSON", ex);
        }
    }

    private Timestamp toTimestamp(OffsetDateTime offsetDateTime) {
        return offsetDateTime == null ? null : Timestamp.from(offsetDateTime.toInstant());
    }

    private OffsetDateTime toOffsetDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant().atOffset(ZoneOffset.UTC);
    }
}
