package com.huashi.eftransfer.app.modules.analytics.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Repository
public class AdminDashboardRepository {

    private static final String AI_FALLBACK_CONDITION = "(generation_source = 'RULE_FALLBACK' OR fallback_reason IS NOT NULL)";

    private final JdbcTemplate jdbcTemplate;

    public AdminDashboardRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long countUsers() {
        return queryForLong("SELECT COUNT(*) FROM users WHERE deleted = FALSE");
    }

    public long countEnabledUsers() {
        return queryForLong("SELECT COUNT(*) FROM users WHERE deleted = FALSE AND enabled = TRUE");
    }

    public long countRegistrationsSince(LocalDateTime since) {
        return queryForLong(
                "SELECT COUNT(*) FROM users WHERE deleted = FALSE AND created_at >= ?",
                Timestamp.valueOf(since)
        );
    }

    public long countActiveUsersSince(LocalDateTime since) {
        return queryForLong(
                "SELECT COUNT(*) FROM users WHERE deleted = FALSE AND last_login_at IS NOT NULL AND last_login_at >= ?",
                Timestamp.valueOf(since)
        );
    }

    public long countCompletedDiagnosisSince(LocalDateTime since) {
        return queryForLong(
                """
                SELECT COUNT(*)
                FROM diagnosis_session
                WHERE deleted = FALSE
                  AND status = 'COMPLETED'
                  AND completed_at IS NOT NULL
                  AND completed_at >= ?
                """,
                Timestamp.valueOf(since)
        );
    }

    public long countCompletedTrainingSince(LocalDateTime since) {
        return queryForLong(
                """
                SELECT COUNT(*)
                FROM training_session
                WHERE deleted = FALSE
                  AND status = 'COMPLETED'
                  AND completed_at IS NOT NULL
                  AND completed_at >= ?
                """,
                Timestamp.valueOf(since)
        );
    }

    public long countSubmittedAssessmentsSince(LocalDateTime since) {
        return queryForLong(
                """
                SELECT COUNT(*)
                FROM assessment_attempt
                WHERE deleted = FALSE
                  AND status = 'SUBMITTED'
                  AND submitted_at IS NOT NULL
                  AND submitted_at >= ?
                """,
                Timestamp.valueOf(since)
        );
    }

    public long countAiCallsSince(LocalDateTime since) {
        return queryForLong(
                """
                SELECT COUNT(*)
                FROM ai_generation_record
                WHERE deleted = FALSE
                  AND generated_at >= ?
                """,
                Timestamp.valueOf(since)
        );
    }

    public long countAiFallbacksSince(LocalDateTime since) {
        return queryForLong(
                """
                SELECT COUNT(*)
                FROM ai_generation_record
                WHERE deleted = FALSE
                  AND generated_at >= ?
                  AND """ + AI_FALLBACK_CONDITION,
                Timestamp.valueOf(since)
        );
    }

    public Map<LocalDate, Long> registrationsByDay(LocalDate startDate, LocalDate endDate) {
        return queryDailyCount(
                """
                SELECT CAST(created_at AS DATE) AS bucket_day, COUNT(*) AS total_count
                FROM users
                WHERE deleted = FALSE
                  AND created_at >= ?
                  AND created_at < ?
                GROUP BY CAST(created_at AS DATE)
                ORDER BY bucket_day ASC
                """,
                startDate,
                endDate
        );
    }

    public Map<LocalDate, Long> completedDiagnosisByDay(LocalDate startDate, LocalDate endDate) {
        return queryDailyCount(
                """
                SELECT CAST(completed_at AS DATE) AS bucket_day, COUNT(*) AS total_count
                FROM diagnosis_session
                WHERE deleted = FALSE
                  AND status = 'COMPLETED'
                  AND completed_at IS NOT NULL
                  AND completed_at >= ?
                  AND completed_at < ?
                GROUP BY CAST(completed_at AS DATE)
                ORDER BY bucket_day ASC
                """,
                startDate,
                endDate
        );
    }

    public Map<LocalDate, Long> completedTrainingByDay(LocalDate startDate, LocalDate endDate) {
        return queryDailyCount(
                """
                SELECT CAST(completed_at AS DATE) AS bucket_day, COUNT(*) AS total_count
                FROM training_session
                WHERE deleted = FALSE
                  AND status = 'COMPLETED'
                  AND completed_at IS NOT NULL
                  AND completed_at >= ?
                  AND completed_at < ?
                GROUP BY CAST(completed_at AS DATE)
                ORDER BY bucket_day ASC
                """,
                startDate,
                endDate
        );
    }

    public Map<LocalDate, Long> submittedAssessmentsByDay(LocalDate startDate, LocalDate endDate) {
        return queryDailyCount(
                """
                SELECT CAST(submitted_at AS DATE) AS bucket_day, COUNT(*) AS total_count
                FROM assessment_attempt
                WHERE deleted = FALSE
                  AND status = 'SUBMITTED'
                  AND submitted_at IS NOT NULL
                  AND submitted_at >= ?
                  AND submitted_at < ?
                GROUP BY CAST(submitted_at AS DATE)
                ORDER BY bucket_day ASC
                """,
                startDate,
                endDate
        );
    }

    public Map<LocalDate, Long> aiCallsByDay(LocalDate startDate, LocalDate endDate) {
        return queryDailyCount(
                """
                SELECT CAST(generated_at AS DATE) AS bucket_day, COUNT(*) AS total_count
                FROM ai_generation_record
                WHERE deleted = FALSE
                  AND generated_at >= ?
                  AND generated_at < ?
                GROUP BY CAST(generated_at AS DATE)
                ORDER BY bucket_day ASC
                """,
                startDate,
                endDate
        );
    }

    public Map<LocalDate, Long> aiFallbacksByDay(LocalDate startDate, LocalDate endDate) {
        return queryDailyCount(
                """
                SELECT CAST(generated_at AS DATE) AS bucket_day, COUNT(*) AS total_count
                FROM ai_generation_record
                WHERE deleted = FALSE
                  AND generated_at >= ?
                  AND generated_at < ?
                  AND """ + AI_FALLBACK_CONDITION + """
                GROUP BY CAST(generated_at AS DATE)
                ORDER BY bucket_day ASC
                """,
                startDate,
                endDate
        );
    }

    public Map<String, Long> aiSceneDistributionSince(LocalDateTime since) {
        List<Map.Entry<String, Long>> rows = jdbcTemplate.query(
                """
                SELECT scene, COUNT(*) AS total_count
                FROM ai_generation_record
                WHERE deleted = FALSE
                  AND generated_at >= ?
                GROUP BY scene
                ORDER BY total_count DESC, scene ASC
                """,
                (rs, rowNum) -> Map.entry(rs.getString("scene"), rs.getLong("total_count")),
                Timestamp.valueOf(since)
        );
        Map<String, Long> result = new LinkedHashMap<>();
        for (Map.Entry<String, Long> row : rows) {
            result.put(normalizeScene(row.getKey()), row.getValue());
        }
        return result;
    }

    private Map<LocalDate, Long> queryDailyCount(String sql, LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime endExclusive = endDate.plusDays(1).atStartOfDay();
        List<Map.Entry<LocalDate, Long>> rows = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> Map.entry(rs.getDate("bucket_day").toLocalDate(), rs.getLong("total_count")),
                Timestamp.valueOf(start),
                Timestamp.valueOf(endExclusive)
        );
        Map<LocalDate, Long> result = new LinkedHashMap<>();
        for (Map.Entry<LocalDate, Long> row : rows) {
            result.put(row.getKey(), row.getValue());
        }
        return result;
    }

    private long queryForLong(String sql, Object... args) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, args);
        return value == null ? 0L : value;
    }

    private String normalizeScene(String scene) {
        if (scene == null || scene.isBlank()) {
            return "UNKNOWN";
        }
        return scene.trim().toUpperCase(Locale.ROOT);
    }
}
