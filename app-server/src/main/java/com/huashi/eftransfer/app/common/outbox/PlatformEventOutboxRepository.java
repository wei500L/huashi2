package com.huashi.eftransfer.app.common.outbox;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class PlatformEventOutboxRepository {

    private final JdbcTemplate jdbcTemplate;

    public PlatformEventOutboxRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(PlatformOutboxMessage message) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        jdbcTemplate.update(
                """
                INSERT INTO platform_event_outbox (
                    event_id, event_type, exchange_name, routing_key, payload_json, headers_json, trace_id,
                    status, attempt_count, next_attempt_at, created_at, created_by, updated_at, updated_by, deleted
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                message.eventId(),
                message.eventType(),
                message.exchangeName(),
                message.routingKey(),
                message.payloadJson(),
                message.headersJson(),
                message.traceId(),
                PlatformEventOutboxStatus.PENDING.name(),
                0,
                timestamp(now),
                timestamp(now),
                0L,
                timestamp(now),
                0L,
                Boolean.FALSE
        );
    }

    @Transactional
    public List<PlatformEventOutboxRecord> claimBatch(int limit, Duration stuckThreshold) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime staleBefore = now.minus(stuckThreshold);
        List<Long> ids = jdbcTemplate.queryForList(
                """
                SELECT id
                FROM platform_event_outbox
                WHERE deleted = FALSE
                  AND (
                      (status IN ('PENDING', 'FAILED') AND next_attempt_at <= ?)
                      OR (status = 'IN_PROGRESS' AND processing_started_at IS NOT NULL AND processing_started_at <= ?)
                  )
                ORDER BY next_attempt_at ASC, id ASC
                LIMIT ?
                FOR UPDATE SKIP LOCKED
                """,
                Long.class,
                timestamp(now),
                timestamp(staleBefore),
                limit
        );
        if (ids.isEmpty()) {
            return List.of();
        }

        String placeholders = placeholders(ids.size());
        List<Object> params = new ArrayList<>();
        params.add(PlatformEventOutboxStatus.IN_PROGRESS.name());
        params.add(timestamp(now));
        params.add(timestamp(now));
        params.addAll(ids);
        jdbcTemplate.update(
                "UPDATE platform_event_outbox SET status = ?, processing_started_at = ?, updated_at = ? WHERE id IN (" + placeholders + ")",
                params.toArray()
        );
        return findByIds(ids);
    }

    public void markPublished(Long id) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        jdbcTemplate.update(
                """
                UPDATE platform_event_outbox
                SET status = ?, attempt_count = attempt_count + 1, published_at = ?, processing_started_at = NULL,
                    last_error = NULL, next_attempt_at = NULL, updated_at = ?
                WHERE id = ?
                """,
                PlatformEventOutboxStatus.PUBLISHED.name(),
                timestamp(now),
                timestamp(now),
                id
        );
    }

    public void markFailed(Long id, OffsetDateTime nextAttemptAt, String lastError) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        jdbcTemplate.update(
                """
                UPDATE platform_event_outbox
                SET status = ?, attempt_count = attempt_count + 1, last_error = ?, next_attempt_at = ?,
                    processing_started_at = NULL, updated_at = ?
                WHERE id = ?
                """,
                PlatformEventOutboxStatus.FAILED.name(),
                truncate(lastError),
                timestamp(nextAttemptAt),
                timestamp(now),
                id
        );
    }

    public PlatformEventOutboxRecord replay(Long id) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        jdbcTemplate.update(
                """
                UPDATE platform_event_outbox
                SET status = ?, next_attempt_at = ?, last_error = NULL, processing_started_at = NULL, updated_at = ?
                WHERE id = ?
                """,
                PlatformEventOutboxStatus.PENDING.name(),
                timestamp(now),
                timestamp(now),
                id
        );
        return findById(id);
    }

    public List<PlatformEventOutboxRecord> list(String status, int limit) {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT id, event_id, event_type, exchange_name, routing_key, payload_json, headers_json, trace_id,
                       status, attempt_count, next_attempt_at, last_error, processing_started_at, published_at, created_at, updated_at
                FROM platform_event_outbox
                WHERE deleted = FALSE
                """);
        if (status != null && !status.isBlank()) {
            sql.append(" AND status = ?");
            params.add(status.trim().toUpperCase(Locale.ROOT));
        }
        sql.append(" ORDER BY updated_at DESC, id DESC LIMIT ?");
        params.add(limit);
        return jdbcTemplate.query(sql.toString(), this::mapRecord, params.toArray());
    }

    public PlatformEventOutboxRecord findById(Long id) {
        List<PlatformEventOutboxRecord> records = jdbcTemplate.query(
                """
                SELECT id, event_id, event_type, exchange_name, routing_key, payload_json, headers_json, trace_id,
                       status, attempt_count, next_attempt_at, last_error, processing_started_at, published_at, created_at, updated_at
                FROM platform_event_outbox
                WHERE deleted = FALSE
                  AND id = ?
                LIMIT 1
                """,
                this::mapRecord,
                id
        );
        return records.isEmpty() ? null : records.getFirst();
    }

    public int countPending() {
        Integer value = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM platform_event_outbox WHERE deleted = FALSE AND status = ?",
                Integer.class,
                PlatformEventOutboxStatus.PENDING.name()
        );
        return value == null ? 0 : value;
    }

    public int countFailed() {
        Integer value = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM platform_event_outbox WHERE deleted = FALSE AND status = ?",
                Integer.class,
                PlatformEventOutboxStatus.FAILED.name()
        );
        return value == null ? 0 : value;
    }

    public double oldestPendingAgeSeconds() {
        List<Timestamp> rows = jdbcTemplate.query(
                """
                SELECT created_at
                FROM platform_event_outbox
                WHERE deleted = FALSE
                  AND status IN ('PENDING', 'FAILED')
                ORDER BY created_at ASC
                LIMIT 1
                """,
                (rs, rowNum) -> rs.getTimestamp("created_at")
        );
        if (rows.isEmpty() || rows.getFirst() == null) {
            return 0D;
        }
        return Duration.between(rows.getFirst().toInstant(), OffsetDateTime.now(ZoneOffset.UTC).toInstant()).toSeconds();
    }

    private List<PlatformEventOutboxRecord> findByIds(List<Long> ids) {
        String placeholders = placeholders(ids.size());
        Map<Long, PlatformEventOutboxRecord> records = jdbcTemplate.query(
                """
                SELECT id, event_id, event_type, exchange_name, routing_key, payload_json, headers_json, trace_id,
                       status, attempt_count, next_attempt_at, last_error, processing_started_at, published_at, created_at, updated_at
                FROM platform_event_outbox
                WHERE id IN (""" + placeholders + ")",
                this::mapRecord,
                ids.toArray()
        ).stream().collect(Collectors.toMap(PlatformEventOutboxRecord::id, record -> record, (left, right) -> left, LinkedHashMap::new));

        List<PlatformEventOutboxRecord> ordered = new ArrayList<>();
        for (Long id : ids) {
            PlatformEventOutboxRecord record = records.get(id);
            if (record != null) {
                ordered.add(record);
            }
        }
        return ordered;
    }

    private PlatformEventOutboxRecord mapRecord(ResultSet rs, int rowNum) throws SQLException {
        return new PlatformEventOutboxRecord(
                rs.getLong("id"),
                rs.getString("event_id"),
                rs.getString("event_type"),
                rs.getString("exchange_name"),
                rs.getString("routing_key"),
                rs.getString("payload_json"),
                rs.getString("headers_json"),
                rs.getString("trace_id"),
                PlatformEventOutboxStatus.valueOf(rs.getString("status")),
                rs.getInt("attempt_count"),
                offsetDateTime(rs, "next_attempt_at"),
                rs.getString("last_error"),
                offsetDateTime(rs, "processing_started_at"),
                offsetDateTime(rs, "published_at"),
                offsetDateTime(rs, "created_at"),
                offsetDateTime(rs, "updated_at")
        );
    }

    private OffsetDateTime offsetDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant().atOffset(ZoneOffset.UTC);
    }

    private Timestamp timestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    private String placeholders(int size) {
        return "?,".repeat(size).replaceAll(",$", "");
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= 1000 ? value : value.substring(0, 1000);
    }
}
