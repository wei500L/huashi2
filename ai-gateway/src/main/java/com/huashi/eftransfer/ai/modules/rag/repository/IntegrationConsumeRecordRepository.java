package com.huashi.eftransfer.ai.modules.rag.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class IntegrationConsumeRecordRepository {

    private final JdbcTemplate jdbcTemplate;

    public IntegrationConsumeRecordRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean isSucceeded(String consumerName, String eventId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM integration_consume_record
                        WHERE consumer_name = ?
                          AND event_id = ?
                          AND status = 'SUCCEEDED'
                        """,
                Integer.class,
                consumerName,
                eventId
        );
        return count != null && count > 0;
    }

    public void markSucceeded(String consumerName, String eventId, String eventType) {
        jdbcTemplate.update(
                """
                        INSERT INTO integration_consume_record (
                            consumer_name,
                            event_id,
                            event_type,
                            status,
                            error_message,
                            processed_at,
                            created_at,
                            updated_at
                        )
                        VALUES (?, ?, ?, 'SUCCEEDED', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        ON CONFLICT (consumer_name, event_id)
                        DO UPDATE SET
                            event_type = EXCLUDED.event_type,
                            status = 'SUCCEEDED',
                            error_message = NULL,
                            processed_at = CURRENT_TIMESTAMP,
                            updated_at = CURRENT_TIMESTAMP
                        """,
                consumerName,
                eventId,
                eventType
        );
    }

    public void markFailed(String consumerName, String eventId, String eventType, String errorMessage) {
        jdbcTemplate.update(
                """
                        INSERT INTO integration_consume_record (
                            consumer_name,
                            event_id,
                            event_type,
                            status,
                            error_message,
                            processed_at,
                            created_at,
                            updated_at
                        )
                        VALUES (?, ?, ?, 'FAILED', ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        ON CONFLICT (consumer_name, event_id)
                        DO UPDATE SET
                            event_type = EXCLUDED.event_type,
                            status = 'FAILED',
                            error_message = EXCLUDED.error_message,
                            processed_at = CURRENT_TIMESTAMP,
                            updated_at = CURRENT_TIMESTAMP
                        """,
                consumerName,
                eventId,
                eventType,
                errorMessage
        );
    }
}
