package com.huashi.eftransfer.app.modules.assessment;

import com.huashi.eftransfer.app.modules.assessment.service.LexiBridgeResearchSeedInitializer;
import com.huashi.eftransfer.app.support.AbstractWebIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class LexiBridgeResearchV1SeedIntegrationTest extends AbstractWebIntegrationTest {

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private LexiBridgeResearchSeedInitializer initializer;

    @Test
    void seedContainsOnlyTheDocumentedProfileFieldsAndSixtyFormalQuestions() {
        Long versionId = versionId();
        Integer formalCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM assessment_questionnaire_item i
                JOIN assessment_questionnaire_section s ON s.id = i.section_id
                WHERE i.questionnaire_version_id = ? AND i.deleted = FALSE AND s.deleted = FALSE
                  AND s.formal_section = TRUE
                """, Integer.class, versionId);
        Integer profileCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM assessment_questionnaire_item i
                JOIN assessment_questionnaire_section s ON s.id = i.section_id
                JOIN assessment_question q ON q.id = i.assessment_question_id
                WHERE i.questionnaire_version_id = ? AND i.deleted = FALSE AND s.deleted = FALSE
                  AND s.formal_section = FALSE AND q.question_type <> 'INSTRUCTION'
                """, Integer.class, versionId);
        Integer legacyCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM assessment_questionnaire_item
                WHERE questionnaire_version_id = ? AND deleted = FALSE
                  AND item_code IN ('BASIC-NAME','BASIC-CONTACT','BASIC-STATUS','BASIC-FRENCH-DURATION','BASIC-OTHER-LANGUAGE')
                """, Integer.class, versionId);

        assertThat(formalCount).isEqualTo(60);
        assertThat(profileCount).isEqualTo(6);
        assertThat(legacyCount).isZero();
    }

    @Test
    void rerunSoftDeletesRemovedItemsAndSynchronizesOrderAndSnapshots() throws Exception {
        Long versionId = versionId();
        Long paperId = jdbcTemplate.queryForObject(
                "SELECT paper_id FROM assessment_questionnaire_version WHERE id = ?", Long.class, versionId);
        Long basicSectionId = jdbcTemplate.queryForObject("""
                SELECT id FROM assessment_questionnaire_section
                WHERE questionnaire_version_id = ? AND section_code = 'BASIC_INFO' AND deleted = FALSE
                """, Long.class, versionId);
        Long ownerId = jdbcTemplate.queryForObject(
                "SELECT owner_user_id FROM assessment_paper WHERE id = ?", Long.class, paperId);

        jdbcTemplate.update("""
                INSERT INTO assessment_question
                    (paper_id,question_type,sort_order,stem_text,options_json,correct_answer_json,score,
                     section_code,required_answer,created_by,updated_by)
                VALUES (?,'SHORT_TEXT',999,'legacy profile field','[]','[]',0,'BASIC_INFO',FALSE,0,0)
                """, paperId);
        Long legacyQuestionId = jdbcTemplate.queryForObject("""
                SELECT id FROM assessment_question WHERE paper_id = ? AND sort_order = 999 AND deleted = FALSE
                """, Long.class, paperId);
        jdbcTemplate.update("""
                INSERT INTO assessment_questionnaire_item
                    (questionnaire_version_id,section_id,assessment_question_id,item_code,required_answer,scored,created_by,updated_by)
                VALUES (?,?,?,'BASIC-LEGACY-REMOVED',FALSE,FALSE,0,0)
                """, versionId, basicSectionId, legacyQuestionId);
        jdbcTemplate.update("""
                INSERT INTO assessment_publish
                    (paper_id,delivery_mode,published_by,status,paper_title_snapshot,question_count_snapshot,
                     total_score_snapshot,duration_minutes,result_release_policy,created_by,updated_by)
                VALUES (?,'PUBLIC',?,'PUBLISHED','seed snapshot',999,999,40,'IMMEDIATE',0,0)
                """, paperId, ownerId);
        Long publishId = jdbcTemplate.queryForObject(
                "SELECT MAX(id) FROM assessment_publish WHERE paper_id = ?", Long.class, paperId);

        initializer.run(null);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT deleted FROM assessment_questionnaire_item WHERE item_code = 'BASIC-LEGACY-REMOVED'",
                Boolean.class)).isTrue();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT deleted FROM assessment_question WHERE id = ?", Boolean.class, legacyQuestionId)).isTrue();
        assertThat(jdbcTemplate.queryForList("""
                SELECT sort_order FROM assessment_question
                WHERE paper_id = ? AND deleted = FALSE ORDER BY sort_order
                """, Integer.class, paperId)).containsExactlyElementsOf(
                java.util.stream.IntStream.rangeClosed(1, 67).boxed().toList());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT question_count FROM assessment_paper WHERE id = ?", Integer.class, paperId)).isEqualTo(67);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT question_count_snapshot FROM assessment_publish WHERE id = ?", Integer.class, publishId)).isEqualTo(67);
    }

    private Long versionId() {
        return jdbcTemplate.queryForObject("""
                SELECT id FROM assessment_questionnaire_version
                WHERE source_package_code = 'LEXIBRIDGE_RESEARCH_V1' AND deleted = FALSE
                """, Long.class);
    }
}
