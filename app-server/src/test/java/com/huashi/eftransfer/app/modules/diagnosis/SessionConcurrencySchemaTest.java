package com.huashi.eftransfer.app.modules.diagnosis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SessionConcurrencySchemaTest {

    private JdbcTemplate jdbcTemplate;
    private String jdbcUrl;

    @BeforeEach
    void setUp() {
        String databaseName = "session-schema-" + UUID.randomUUID();
        jdbcUrl = "jdbc:h2:mem:%s;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1".formatted(databaseName);

        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl(jdbcUrl);
        dataSource.setUsername("sa");
        dataSource.setPassword("");

        new ResourceDatabasePopulator(new ClassPathResource("schema-h2.sql")).execute(dataSource);
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Test
    void shouldRejectSecondInProgressSessionForSameUser() throws Exception {
        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO users (id, username, email, password_hash, display_name, enabled, created_at, updated_at, deleted)
                    VALUES
                      (1, 'teacher.schema', 'teacher.schema@ef.local', 'hash', 'Teacher Schema', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
                      (2, 'student.schema', 'student.schema@ef.local', 'hash', 'Student Schema', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE)
                    """);
            statement.executeUpdate("""
                    INSERT INTO diagnosis_template (
                        id, template_name, description, owner_user_id, status, estimated_duration_minutes, scoring_version,
                        item_count, metadata_json, created_at, updated_at, deleted
                    )
                    VALUES (11, 'Schema Template', 'schema test', 1, 'PUBLISHED', 10, 'RULE_V1', 0, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE)
                    """);
            statement.executeUpdate("""
                    INSERT INTO diagnosis_session (
                        id, template_id, owner_user_id, status, session_seed, total_items, answered_items,
                        current_item_order, started_at, created_at, updated_at, deleted
                    )
                    VALUES (21, 11, 2, 'IN_PROGRESS', 1001, 3, 1, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE)
                    """);

            assertThatThrownBy(() -> statement.executeUpdate("""
                    INSERT INTO diagnosis_session (
                        id, template_id, owner_user_id, status, session_seed, total_items, answered_items,
                        current_item_order, started_at, created_at, updated_at, deleted
                    )
                    VALUES (22, 11, 2, 'IN_PROGRESS', 1002, 3, 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE)
                    """))
                    .isInstanceOf(Exception.class);
        }
    }

    @Test
    void shouldExposeRequestHashColumn() throws Exception {
        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "");
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT COUNT(*)
                     FROM information_schema.columns
                     WHERE table_name = 'IDEMPOTENCY_RECORD'
                       AND column_name = 'REQUEST_HASH'
                     """)) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getInt(1)).isEqualTo(1);
        }
    }
}
