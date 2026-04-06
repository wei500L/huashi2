package com.huashi.eftransfer.app.modules.assessment;

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

class AssessmentRecipientMigrationTest {

    private static final DateTimeFormatter SQL_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Test
    void shouldBackfillHistoricalRecipientsForRemovedStudents() throws Exception {
        String databaseName = "assessment-recipient-" + UUID.randomUUID();
        String jdbcUrl = "jdbc:h2:mem:%s;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1".formatted(databaseName);

        Flyway.configure()
                .dataSource(jdbcUrl, "sa", "")
                .locations("classpath:db/migration")
                .target(MigrationVersion.fromVersion("16"))
                .load()
                .migrate();

        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "");
             Statement statement = connection.createStatement()) {
            LocalDateTime publishedAt = LocalDateTime.of(2026, 3, 1, 9, 0);
            LocalDateTime joinedAt = publishedAt.minusDays(10);
            LocalDateTime leftAt = publishedAt.plusDays(5);

            statement.executeUpdate("""
                    INSERT INTO users (id, username, email, password_hash, display_name, enabled, created_at, updated_at, deleted)
                    VALUES
                      (1, 'teacher.migration', 'teacher.migration@ef.local', 'hash', 'Teacher Migration', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
                      (2, 'student.migration', 'student.migration@ef.local', 'hash', 'Student Migration', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE)
                    """);
            statement.executeUpdate("""
                    INSERT INTO teaching_class (id, class_code, class_name, grade_name, teacher_user_id, active, created_at, updated_at, deleted)
                    VALUES (11, 'CLS-MIG-01', '迁移回补班级', 'Grade 10', 1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE)
                    """);
            statement.executeUpdate("""
                    INSERT INTO teaching_class_student (id, teaching_class_id, student_user_id, joined_at, left_at, active, created_at, updated_at, deleted)
                    VALUES (21, 11, 2, TIMESTAMP '%s', TIMESTAMP '%s', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, TRUE)
                    """.formatted(joinedAt.format(SQL_TIMESTAMP), leftAt.format(SQL_TIMESTAMP)));
            statement.executeUpdate("""
                    INSERT INTO assessment_paper (
                        id, paper_code, title, description, owner_user_id, status, duration_minutes,
                        question_count, total_score, created_at, updated_at, deleted
                    )
                    VALUES (31, 'ASM-MIG-01', '迁移回补测评', 'migration test', 1, 'PUBLISHED', 30, 1, 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE)
                    """);
            statement.executeUpdate("""
                    INSERT INTO assessment_publish (
                        id, paper_id, teaching_class_id, published_by, status, paper_title_snapshot,
                        paper_description_snapshot, question_count_snapshot, total_score_snapshot,
                        duration_minutes, instructions_text, starts_at, due_at, published_at,
                        created_at, updated_at, deleted
                    )
                    VALUES (
                        41, 31, 11, 1, 'PUBLISHED', '迁移回补测评', 'migration test',
                        1, 10, 30, '请作答', NULL, NULL, TIMESTAMP '%s',
                        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE
                    )
                    """.formatted(publishedAt.format(SQL_TIMESTAMP)));
        }

        Flyway.configure()
                .dataSource(jdbcUrl, "sa", "")
                .locations("classpath:db/migration")
                .load()
                .migrate();

        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "");
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT COUNT(*)
                     FROM assessment_publish_recipient
                     WHERE publish_id = 41
                       AND student_user_id = 2
                       AND deleted = FALSE
                     """)) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getInt(1)).isEqualTo(1);
        }
    }
}
