package com.huashi.eftransfer.app.modules.assessment;

import com.huashi.eftransfer.app.support.AbstractWebIntegrationTest;
import com.huashi.eftransfer.app.modules.assessment.service.LexiBridgeResearchV3SeedInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the V3 seed initializer creates an APPROVED questionnaire
 * with the four FF4 sections when its flag is enabled, and that the released
 * V1 package is left untouched by the V3 seed run.
 */
@TestPropertySource(properties = "app.assessment.seed.lexibridge-v3-enabled=true")
class LexiBridgeResearchV3SeedIntegrationTest extends AbstractWebIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private LexiBridgeResearchV3SeedInitializer initializer;

    @Test
    void v3SeedCreatesFourSectionQuestionnaireInApprovedState() {
        Integer questionnaireCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM assessment_questionnaire WHERE questionnaire_code = 'LEXIBRIDGE_RESEARCH_V3' AND deleted = FALSE",
                Integer.class);
        assertThat(questionnaireCount).isEqualTo(1);
        Integer versionCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM assessment_questionnaire_version
                WHERE source_package_code = 'LEXIBRIDGE_RESEARCH_V3' AND status = 'APPROVED' AND deleted = FALSE
                """, Integer.class);
        assertThat(versionCount).isEqualTo(1);
        Integer sectionCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM assessment_questionnaire_section s
                JOIN assessment_questionnaire_version v ON v.id = s.questionnaire_version_id
                WHERE v.source_package_code = 'LEXIBRIDGE_RESEARCH_V3'
                  AND s.formal_section = TRUE AND s.deleted = FALSE
                """, Integer.class);
        assertThat(sectionCount).isEqualTo(4);
        Integer spellingItems = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM assessment_question q
                JOIN assessment_questionnaire_version v ON v.paper_id = q.paper_id
                WHERE v.source_package_code = 'LEXIBRIDGE_RESEARCH_V3'
                  AND q.question_type = 'SPELLING' AND q.deleted = FALSE
                """, Integer.class);
        assertThat(spellingItems).isGreaterThanOrEqualTo(1);
    }

    @Test
    void v3SeedDoesNotTouchTheReleasedV1Package() {
        Integer v1Count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM assessment_questionnaire WHERE questionnaire_code = 'LEXIBRIDGE_RESEARCH_V1' AND deleted = FALSE",
                Integer.class);
        assertThat(v1Count).isEqualTo(1);
        Integer v1VersionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM assessment_questionnaire_version WHERE source_package_code = 'LEXIBRIDGE_RESEARCH_V1' AND deleted = FALSE",
                Integer.class);
        assertThat(v1VersionCount).isEqualTo(1);
    }

    @Test
    void rerunDoesNotRewriteAnApprovedOrPublishedSnapshot() throws Exception {
        Long paperId = jdbcTemplate.queryForObject("""
                SELECT paper_id FROM assessment_questionnaire_version
                WHERE source_package_code = 'LEXIBRIDGE_RESEARCH_V3' AND deleted = FALSE
                """, Long.class);
        Long questionId = jdbcTemplate.queryForObject(
                "SELECT id FROM assessment_question WHERE paper_id = ? AND deleted = FALSE ORDER BY sort_order LIMIT 1",
                Long.class, paperId);
        jdbcTemplate.update("UPDATE assessment_question SET stem_text = 'MANUAL_REVIEWED_SNAPSHOT' WHERE id = ?", questionId);
        jdbcTemplate.update("UPDATE assessment_paper SET status = 'PUBLISHED' WHERE id = ?", paperId);

        initializer.run(null);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT stem_text FROM assessment_question WHERE id = ?", String.class, questionId))
                .isEqualTo("MANUAL_REVIEWED_SNAPSHOT");
    }
}
