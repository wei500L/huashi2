package com.huashi.eftransfer.app.modules.assessment;

import com.huashi.eftransfer.app.support.AbstractWebIntegrationTest;
import com.huashi.eftransfer.app.modules.assessment.service.LexiBridgePracticeBankSeedInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the practice-bank seed initializer creates only the FF4 V2 question
 * bank (no questionnaire / paper rows) when its flag is enabled, and that the
 * released V1 research package is left untouched by the seed run.
 */
@TestPropertySource(properties = "app.assessment.seed.lexibridge-v3-enabled=true")
class LexiBridgePracticeBankSeedIntegrationTest extends AbstractWebIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private LexiBridgePracticeBankSeedInitializer initializer;

    @Test
    void seedCreatesQuestionBankWithoutAnyQuestionnaireRows() {
        Integer bankCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM assessment_question_bank WHERE bank_code = 'LEXIBRIDGE_FF4_V2' AND deleted = FALSE",
                Integer.class);
        assertThat(bankCount).isEqualTo(1);
        Integer versionCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM assessment_question_version v
                JOIN assessment_question_bank b ON b.id = v.question_bank_id
                WHERE b.bank_code = 'LEXIBRIDGE_FF4_V2' AND v.deleted = FALSE
                """, Integer.class);
        assertThat(versionCount).isEqualTo(251);
        Integer spellingItems = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM assessment_question_version v
                JOIN assessment_question_bank b ON b.id = v.question_bank_id
                WHERE b.bank_code = 'LEXIBRIDGE_FF4_V2' AND v.question_type = 'SPELLING' AND v.deleted = FALSE
                """, Integer.class);
        assertThat(spellingItems).isGreaterThanOrEqualTo(170);
        Integer questionnaireCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM assessment_questionnaire WHERE questionnaire_code = 'LEXIBRIDGE_RESEARCH_V3' AND deleted = FALSE",
                Integer.class);
        assertThat(questionnaireCount).isZero();
        Integer paperCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM assessment_paper WHERE paper_code = 'LEXIBRIDGE_RESEARCH_V3' AND deleted = FALSE",
                Integer.class);
        assertThat(paperCount).isZero();
    }

    @Test
    void seedDoesNotTouchTheReleasedV1Package() {
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
    void rerunIsIdempotent() throws Exception {
        initializer.run(null);
        Integer versionCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM assessment_question_version v
                JOIN assessment_question_bank b ON b.id = v.question_bank_id
                WHERE b.bank_code = 'LEXIBRIDGE_FF4_V2' AND v.deleted = FALSE
                """, Integer.class);
        assertThat(versionCount).isEqualTo(251);
    }

    @Test
    void rerunSoftDeletesVersionsRemovedFromTheSeedPackage() throws Exception {
        Long bankId = jdbcTemplate.queryForObject(
                "SELECT id FROM assessment_question_bank WHERE bank_code = 'LEXIBRIDGE_FF4_V2'",
                Long.class);
        jdbcTemplate.update("""
                INSERT INTO assessment_question_version
                    (question_bank_id,question_code,version_no,question_type,stem_text,prompt_text,options_json,
                     correct_answer_json,explanation_text,option_explanations_json,required_answer,weight,
                     transfer_category,context_level,construct_code,target_word,display_condition_json,source_reference,
                     content_hash,created_by,updated_by)
                VALUES (?, 'STALE-REMOVED-CODE', 1, 'SINGLE_CHOICE', 'stale', NULL, '[]', '["A"]', NULL, '{}',
                        TRUE, 1.0, NULL, NULL, 'FF4_WORD_MEANING', NULL, NULL, NULL,
                        'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', 0, 0)
                """, bankId);

        initializer.run(null);

        Integer staleActive = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM assessment_question_version
                WHERE question_bank_id = ? AND question_code = 'STALE-REMOVED-CODE' AND deleted = FALSE
                """, Integer.class, bankId);
        assertThat(staleActive).isZero();
        Integer staleDeleted = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM assessment_question_version
                WHERE question_bank_id = ? AND question_code = 'STALE-REMOVED-CODE' AND deleted = TRUE
                """, Integer.class, bankId);
        assertThat(staleDeleted).isEqualTo(1);
    }
}
