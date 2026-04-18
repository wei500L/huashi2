package com.huashi.eftransfer.app.modules.diagnosis;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SessionConcurrencyMigrationTest {

    private static final DateTimeFormatter SQL_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Test
    void shouldBackfillDuplicateInProgressSessionsAndEnforceSingleActiveConstraint() throws Exception {
        String databaseName = "session-concurrency-" + UUID.randomUUID();
        String jdbcUrl = "jdbc:h2:mem:%s;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1".formatted(databaseName);

        Flyway.configure()
                .dataSource(jdbcUrl, "sa", "")
                .locations("classpath:db/migration")
                .target(MigrationVersion.fromVersion("26"))
                .load()
                .migrate();

        LocalDateTime earlier = LocalDateTime.of(2026, 4, 1, 9, 0);
        LocalDateTime later = earlier.plusMinutes(5);

        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO users (id, username, email, password_hash, display_name, enabled, created_at, updated_at, deleted)
                    VALUES
                      (1, 'teacher.migration', 'teacher.migration@ef.local', 'hash', 'Teacher Migration', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
                      (2, 'student.migration', 'student.migration@ef.local', 'hash', 'Student Migration', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE)
                    """);
            statement.executeUpdate("""
                    INSERT INTO diagnosis_template (
                        id, template_name, description, owner_user_id, status, estimated_duration_minutes, scoring_version,
                        item_count, metadata_json, created_at, updated_at, deleted
                    )
                    VALUES (11, 'Migration Template', 'migration test', 1, 'PUBLISHED', 10, 'RULE_V1', 0, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE)
                    """);
            statement.executeUpdate("""
                    INSERT INTO diagnosis_session (
                        id, template_id, owner_user_id, status, session_seed, total_items, answered_items,
                        current_item_order, started_at, created_at, updated_at, deleted
                    )
                    VALUES
                      (21, 11, 2, 'IN_PROGRESS', 1001, 3, 1, 2, TIMESTAMP '%s', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
                      (22, 11, 2, 'IN_PROGRESS', 1002, 3, 2, 3, TIMESTAMP '%s', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE)
                    """.formatted(earlier.format(SQL_TIMESTAMP), later.format(SQL_TIMESTAMP)));
            statement.executeUpdate("""
                    INSERT INTO diagnosis_summary (
                        id, session_id, owner_user_id, template_id, positive_transfer_score, negative_transfer_risk,
                        context_sensitivity, semantic_discrimination, overall_accuracy, average_reaction_time_ms,
                        error_type_distribution_json, high_risk_lexical_pairs_json, chart_payload_json,
                        generated_at, scoring_version, created_at, updated_at, deleted
                    )
                    VALUES (31, 22, 2, 11, 0.5, 0.5, 0.5, 0.5, 0.5, 800, '[]', '[]', '{}', CURRENT_TIMESTAMP, 'RULE_V1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE)
                    """);
            statement.executeUpdate("""
                    INSERT INTO training_plan (
                        id, owner_user_id, source_diagnosis_session_id, source_diagnosis_summary_id, status, priority_mode,
                        recommended_difficulty, risk_level, estimated_training_volume, recommendation_reason,
                        target_metrics_json, generated_at, created_at, updated_at, deleted
                    )
                    VALUES (41, 2, 22, 31, 'GENERATED', 'FALSE_FRIEND_DISCRIM', 3, 'HIGH', 6, 'migration test', '{}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE)
                    """);
            statement.executeUpdate("""
                    INSERT INTO training_session (
                        id, plan_id, owner_user_id, mode, status, session_seed, total_items, answered_items,
                        current_item_order, planned_difficulty, risk_level, started_at, created_at, updated_at, deleted
                    )
                    VALUES
                      (51, 41, 2, 'FALSE_FRIEND_DISCRIM', 'IN_PROGRESS', 2001, 4, 1, 2, 3, 'HIGH', TIMESTAMP '%s', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
                      (52, 41, 2, 'FALSE_FRIEND_DISCRIM', 'IN_PROGRESS', 2002, 4, 2, 3, 3, 'HIGH', TIMESTAMP '%s', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE)
                    """.formatted(earlier.format(SQL_TIMESTAMP), later.format(SQL_TIMESTAMP)));

            statement.executeUpdate("""
                    INSERT INTO idempotency_record (
                        id, request_key, request_path, request_method, response_code, response_body, expires_at, created_at, updated_at, deleted
                    )
                    VALUES (61, 'legacy-key', '/api/example', 'POST', 'SUCCESS', '{}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE)
                    """);
        }

        Flyway.configure()
                .dataSource(jdbcUrl, "sa", "")
                .locations("classpath:db/migration")
                .load()
                .migrate();

        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "");
             Statement statement = connection.createStatement()) {
            try (ResultSet diagnosisRows = statement.executeQuery("""
                    SELECT id, status
                    FROM diagnosis_session
                    WHERE owner_user_id = 2
                    ORDER BY id ASC
                    """)) {
                assertThat(diagnosisRows.next()).isTrue();
                assertThat(diagnosisRows.getLong("id")).isEqualTo(21L);
                assertThat(diagnosisRows.getString("status")).isEqualTo("ABANDONED");
                assertThat(diagnosisRows.next()).isTrue();
                assertThat(diagnosisRows.getLong("id")).isEqualTo(22L);
                assertThat(diagnosisRows.getString("status")).isEqualTo("IN_PROGRESS");
            }

            try (ResultSet trainingRows = statement.executeQuery("""
                    SELECT id, status
                    FROM training_session
                    WHERE owner_user_id = 2
                    ORDER BY id ASC
                    """)) {
                assertThat(trainingRows.next()).isTrue();
                assertThat(trainingRows.getLong("id")).isEqualTo(51L);
                assertThat(trainingRows.getString("status")).isEqualTo("ABANDONED");
                assertThat(trainingRows.next()).isTrue();
                assertThat(trainingRows.getLong("id")).isEqualTo(52L);
                assertThat(trainingRows.getString("status")).isEqualTo("IN_PROGRESS");
            }

            try (ResultSet requestHashColumn = statement.executeQuery("""
                    SELECT request_hash
                    FROM idempotency_record
                    WHERE id = 61
                    """)) {
                assertThat(requestHashColumn.next()).isTrue();
                assertThat(requestHashColumn.getString("request_hash")).isNull();
            }

            assertThatThrownBy(() -> statement.executeUpdate("""
                    INSERT INTO diagnosis_session (
                        id, template_id, owner_user_id, status, session_seed, total_items, answered_items,
                        current_item_order, started_at, created_at, updated_at, deleted
                    )
                    VALUES (23, 11, 2, 'IN_PROGRESS', 1003, 3, 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE)
                    """))
                    .isInstanceOf(Exception.class);

            assertThatThrownBy(() -> statement.executeUpdate("""
                    INSERT INTO training_session (
                        id, plan_id, owner_user_id, mode, status, session_seed, total_items, answered_items,
                        current_item_order, planned_difficulty, risk_level, started_at, created_at, updated_at, deleted
                    )
                    VALUES (53, 41, 2, 'FALSE_FRIEND_DISCRIM', 'IN_PROGRESS', 2003, 4, 0, 1, 3, 'HIGH', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE)
                    """))
                    .isInstanceOf(Exception.class);
        }
    }
}
