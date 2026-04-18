package com.huashi.eftransfer.app.common.idempotency;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Repository
public class IdempotencyRecordRepository {

    private final JdbcTemplate jdbcTemplate;

    public IdempotencyRecordRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public IdempotencyRecord findByRequestKey(String requestKey) {
        List<IdempotencyRecord> records = jdbcTemplate.query(
                """
                SELECT id, request_key, request_path, request_method, request_hash, response_code, response_body, expires_at
                FROM idempotency_record
                WHERE deleted = FALSE
                  AND request_key = ?
                LIMIT 1
                """,
                this::mapRecord,
                requestKey
        );
        return records.isEmpty() ? null : records.getFirst();
    }

    public void insertProcessing(
            String requestKey,
            String requestPath,
            String requestMethod,
            String requestHash,
            OffsetDateTime expiresAt,
            Long actorUserId
    ) throws DataIntegrityViolationException {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        jdbcTemplate.update(
                """
                INSERT INTO idempotency_record (
                    request_key, request_path, request_method, request_hash, response_code, response_body,
                    expires_at, created_at, created_by, updated_at, updated_by, deleted
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                requestKey,
                requestPath,
                requestMethod,
                requestHash,
                "PROCESSING",
                null,
                timestamp(expiresAt),
                timestamp(now),
                actorUserId,
                timestamp(now),
                actorUserId,
                Boolean.FALSE
        );
    }

    public void markCompleted(String requestKey, String responseCode, String responseBody, Long actorUserId) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        jdbcTemplate.update(
                """
                UPDATE idempotency_record
                SET response_code = ?,
                    response_body = ?,
                    updated_at = ?,
                    updated_by = ?
                WHERE request_key = ?
                  AND deleted = FALSE
                """,
                responseCode,
                responseBody,
                timestamp(now),
                actorUserId,
                requestKey
        );
    }

    public void deleteByRequestKey(String requestKey) {
        jdbcTemplate.update(
                """
                DELETE FROM idempotency_record
                WHERE request_key = ?
                """,
                requestKey
        );
    }

    public int deleteExpired(OffsetDateTime now) {
        return jdbcTemplate.update(
                """
                DELETE FROM idempotency_record
                WHERE expires_at <= ?
                """,
                timestamp(now)
        );
    }

    private IdempotencyRecord mapRecord(ResultSet resultSet, int rowNum) throws SQLException {
        return new IdempotencyRecord(
                resultSet.getLong("id"),
                resultSet.getString("request_key"),
                resultSet.getString("request_path"),
                resultSet.getString("request_method"),
                resultSet.getString("request_hash"),
                resultSet.getString("response_code"),
                resultSet.getString("response_body"),
                offsetDateTime(resultSet.getTimestamp("expires_at"))
        );
    }

    private Timestamp timestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    private OffsetDateTime offsetDateTime(Timestamp value) {
        return value == null ? null : value.toInstant().atOffset(ZoneOffset.UTC);
    }
}
