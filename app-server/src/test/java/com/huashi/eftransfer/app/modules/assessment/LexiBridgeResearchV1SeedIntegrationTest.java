package com.huashi.eftransfer.app.modules.assessment;

import com.huashi.eftransfer.app.modules.assessment.service.LexiBridgeResearchSeedInitializer;
import com.huashi.eftransfer.app.support.AbstractWebIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LexiBridgeResearchV1SeedIntegrationTest extends AbstractWebIntegrationTest {

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private LexiBridgeResearchSeedInitializer initializer;

    @Test
    void seedContainsNameAndContactBeforeScoreFieldsWithSixtyFormalQuestions() {
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
        Integer removedLegacyCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM assessment_questionnaire_item
                WHERE questionnaire_version_id = ? AND deleted = FALSE
                  AND item_code IN ('BASIC-STATUS','BASIC-FRENCH-DURATION','BASIC-OTHER-LANGUAGE')
                """, Integer.class, versionId);
        Boolean nameRequired = jdbcTemplate.queryForObject("""
                SELECT required_answer FROM assessment_questionnaire_item
                WHERE questionnaire_version_id = ? AND item_code = 'BASIC-NAME' AND deleted = FALSE
                """, Boolean.class, versionId);

        assertThat(formalCount).isEqualTo(60);
        assertThat(profileCount).isEqualTo(8);
        assertThat(removedLegacyCount).isZero();
        assertThat(nameRequired).isTrue();
        assertThat(sortOrderOf(versionId, "BASIC-NAME")).isEqualTo(2);
        assertThat(sortOrderOf(versionId, "BASIC-CONTACT")).isEqualTo(3);
        assertThat(sortOrderOf(versionId, "BASIC-GAOKAO-ENGLISH")).isEqualTo(4);
        assertThat(sortOrderOf(versionId, "BASIC-ENGLISH-MAJOR")).isEqualTo(5);
        assertThat(sortOrderOf(versionId, "BASIC-CET4")).isEqualTo(6);
        assertThat(sortOrderOf(versionId, "BASIC-CET6")).isEqualTo(7);
        assertThat(sortOrderOf(versionId, "BASIC-TEM4")).isEqualTo(8);
        assertThat(sortOrderOf(versionId, "BASIC-TEM8")).isEqualTo(9);
        assertThat(sortOrderOf(versionId, "P1A-01")).isEqualTo(10);
    }

    @Test
    void seedAppliesSixtyMinuteDurationToPaperAndPublishedSnapshots() {
        Long versionId = versionId();
        Long paperId = jdbcTemplate.queryForObject(
                "SELECT paper_id FROM assessment_questionnaire_version WHERE id = ?", Long.class, versionId);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT duration_minutes FROM assessment_paper WHERE id = ?", Integer.class, paperId)).isEqualTo(60);
        assertThat(jdbcTemplate.queryForList(
                "SELECT duration_minutes FROM assessment_publish WHERE paper_id = ? AND deleted = FALSE",
                Integer.class, paperId)).allSatisfy(minutes -> assertThat(minutes).isEqualTo(60));
        assertThat(jdbcTemplate.queryForObject("""
                SELECT stem_text FROM assessment_question
                WHERE paper_id = ? AND section_code = 'BASIC_INFO' AND question_type = 'INSTRUCTION' AND deleted = FALSE
                """, String.class, paperId)).contains("约 60 分钟");
    }

    @Test
    void rerunRestoresSoftDeletedFieldsAndSynchronizesOrderDurationSnapshotsAndAttemptSnapshots() throws Exception {
        Long versionId = versionId();
        Long paperId = jdbcTemplate.queryForObject(
                "SELECT paper_id FROM assessment_questionnaire_version WHERE id = ?", Long.class, versionId);
        Long basicSectionId = jdbcTemplate.queryForObject("""
                SELECT id FROM assessment_questionnaire_section
                WHERE questionnaire_version_id = ? AND section_code = 'BASIC_INFO' AND deleted = FALSE
                """, Long.class, versionId);
        Long ownerId = jdbcTemplate.queryForObject(
                "SELECT owner_user_id FROM assessment_paper WHERE id = ?", Long.class, paperId);

        softDeleteItem(versionId, paperId, "BASIC-NAME");
        softDeleteItem(versionId, paperId, "BASIC-CONTACT");

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

        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("""
                INSERT INTO assessment_attempt
                    (publish_id,paper_id,status,started_at,expires_at,answered_count,version,created_by,updated_by)
                VALUES (?,?,'IN_PROGRESS',?,?,2,7,0,0)
                """, publishId, paperId, now, now.plusMinutes(40));
        Long attemptId = jdbcTemplate.queryForObject(
                "SELECT MAX(id) FROM assessment_attempt WHERE paper_id = ?", Long.class, paperId);
        Long gaokaoQuestionId = questionIdOf(versionId, "BASIC-GAOKAO-ENGLISH");
        Long p1aQuestionId = questionIdOf(versionId, "P1A-01");
        jdbcTemplate.update("""
                INSERT INTO assessment_attempt_answer
                    (attempt_id,question_id,question_order,question_type,stem_text_snapshot,options_json_snapshot,
                     correct_answer_json,response_json,answered,correct,score_awarded,created_by,updated_by)
                VALUES (?,?,2,'NUMBER','高考英语分数','[]','[]','["130"]',TRUE,TRUE,1,0,0)
                """, attemptId, gaokaoQuestionId);
        jdbcTemplate.update("""
                INSERT INTO assessment_attempt_answer
                    (attempt_id,question_id,question_order,question_type,stem_text_snapshot,options_json_snapshot,
                     correct_answer_json,response_json,answered,correct,score_awarded,created_by,updated_by)
                VALUES (?,?,8,'SINGLE_CHOICE','description stem','[]','["A"]','["A"]',TRUE,TRUE,1,0,0)
                """, attemptId, p1aQuestionId);

        initializer.run(null);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT deleted FROM assessment_questionnaire_item WHERE item_code = 'BASIC-NAME'",
                Boolean.class)).isFalse();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT deleted FROM assessment_questionnaire_item WHERE item_code = 'BASIC-CONTACT'",
                Boolean.class)).isFalse();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT deleted FROM assessment_question WHERE id = ?", Boolean.class,
                questionIdOf(versionId, "BASIC-NAME"))).isFalse();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT deleted FROM assessment_questionnaire_item WHERE item_code = 'BASIC-LEGACY-REMOVED'",
                Boolean.class)).isTrue();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT deleted FROM assessment_question WHERE id = ?", Boolean.class, legacyQuestionId)).isTrue();

        assertThat(jdbcTemplate.queryForList("""
                SELECT sort_order FROM assessment_question
                WHERE paper_id = ? AND deleted = FALSE ORDER BY sort_order
                """, Integer.class, paperId)).containsExactlyElementsOf(
                java.util.stream.IntStream.rangeClosed(1, 69).boxed().toList());
        assertThat(sortOrderOf(versionId, "BASIC-NAME")).isEqualTo(2);
        assertThat(sortOrderOf(versionId, "BASIC-CONTACT")).isEqualTo(3);
        assertThat(sortOrderOf(versionId, "BASIC-GAOKAO-ENGLISH")).isEqualTo(4);
        assertThat(sortOrderOf(versionId, "BASIC-ENGLISH-MAJOR")).isEqualTo(5);
        assertThat(sortOrderOf(versionId, "BASIC-CET4")).isEqualTo(6);
        assertThat(sortOrderOf(versionId, "BASIC-CET6")).isEqualTo(7);
        assertThat(sortOrderOf(versionId, "BASIC-TEM4")).isEqualTo(8);
        assertThat(sortOrderOf(versionId, "BASIC-TEM8")).isEqualTo(9);
        assertThat(sortOrderOf(versionId, "P1A-01")).isEqualTo(10);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT question_count FROM assessment_paper WHERE id = ?", Integer.class, paperId)).isEqualTo(69);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT total_score FROM assessment_paper WHERE id = ?", Integer.class, paperId)).isEqualTo(60);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT duration_minutes FROM assessment_paper WHERE id = ?", Integer.class, paperId)).isEqualTo(60);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT question_count_snapshot FROM assessment_publish WHERE id = ?", Integer.class, publishId)).isEqualTo(69);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT total_score_snapshot FROM assessment_publish WHERE id = ?", Integer.class, publishId)).isEqualTo(60);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT duration_minutes FROM assessment_publish WHERE id = ?", Integer.class, publishId)).isEqualTo(60);

        List<Map<String, Object>> answers = jdbcTemplate.queryForList("""
                SELECT question_id, question_order, question_type, stem_text_snapshot, response_json, answered
                FROM assessment_attempt_answer WHERE attempt_id = ? ORDER BY question_order
                """, attemptId);
        assertThat(answers).hasSize(2);
        assertThat(answers.get(0))
                .containsEntry("question_id", gaokaoQuestionId)
                .containsEntry("question_order", 2)
                .containsEntry("question_type", "NUMBER")
                .containsEntry("response_json", "[\"130\"]");
        assertThat(answers.get(1))
                .containsEntry("question_id", p1aQuestionId)
                .containsEntry("question_order", 8)
                .containsEntry("response_json", "[\"A\"]");
        Long oldAttemptDeadlineMinutes = jdbcTemplate.queryForObject("""
                SELECT TIMESTAMPDIFF(MINUTE, started_at, expires_at) FROM assessment_attempt WHERE id = ?
                """, Long.class, attemptId);
        assertThat(oldAttemptDeadlineMinutes).isEqualTo(40);
    }

    @Test
    void rerunCreatesNewQuestionVersionAboveHistoricalDeletedVersionsWhenSeedHashChanges() throws Exception {
        Long gaokaoVersionOne = jdbcTemplate.queryForObject("""
                SELECT id FROM assessment_question_version
                WHERE question_code = 'BASIC-GAOKAO-ENGLISH' AND version_no = 1 AND deleted = FALSE
                """, Long.class);
        Long bankId = jdbcTemplate.queryForObject(
                "SELECT question_bank_id FROM assessment_question_version WHERE id = ?", Long.class, gaokaoVersionOne);

        // Simulate the pre-update production state: the active version still carries the old
        // content hash, and a historical version was soft-deleted but still occupies its unique key.
        jdbcTemplate.update("""
                UPDATE assessment_question_version SET content_hash = ?
                WHERE id = ? AND deleted = FALSE
                """, "a".repeat(64), gaokaoVersionOne);
        jdbcTemplate.update("""
                INSERT INTO assessment_question_version
                    (question_bank_id, question_code, version_no, question_type, content_hash, deleted, created_by, updated_by)
                VALUES (?, 'BASIC-GAOKAO-ENGLISH', 9, 'NUMBER', ?, TRUE, 0, 0)
                """, bankId, "b".repeat(64));

        initializer.run(null);

        Integer activeGaokaoVersions = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM assessment_question_version
                WHERE question_code = 'BASIC-GAOKAO-ENGLISH' AND deleted = FALSE
                """, Integer.class);
        assertThat(activeGaokaoVersions).isEqualTo(2);
        Integer highestGaokaoVersion = jdbcTemplate.queryForObject("""
                SELECT MAX(version_no) FROM assessment_question_version
                WHERE question_code = 'BASIC-GAOKAO-ENGLISH'
                """, Integer.class);
        assertThat(highestGaokaoVersion).isEqualTo(10);
        Long referencedVersion = jdbcTemplate.queryForObject("""
                SELECT v.id FROM assessment_question_version v
                JOIN assessment_questionnaire_item i ON i.question_version_id = v.id
                WHERE i.item_code = 'BASIC-GAOKAO-ENGLISH'
                """, Long.class);
        assertThat(referencedVersion).isNotEqualTo(gaokaoVersionOne);
    }

    private void softDeleteItem(Long versionId, Long paperId, String itemCode) {
        Long itemId = jdbcTemplate.queryForObject("""
                SELECT id FROM assessment_questionnaire_item
                WHERE questionnaire_version_id = ? AND item_code = ? AND deleted = FALSE
                """, Long.class, versionId, itemCode);
        Long questionId = jdbcTemplate.queryForObject(
                "SELECT assessment_question_id FROM assessment_questionnaire_item WHERE id = ?", Long.class, itemId);
        jdbcTemplate.update("UPDATE assessment_questionnaire_item SET deleted = TRUE WHERE id = ?", itemId);
        jdbcTemplate.update("UPDATE assessment_question SET deleted = TRUE WHERE id = ?", questionId);
    }

    private Long questionIdOf(Long versionId, String itemCode) {
        return jdbcTemplate.queryForObject("""
                SELECT assessment_question_id FROM assessment_questionnaire_item
                WHERE questionnaire_version_id = ? AND item_code = ? AND deleted = FALSE
                """, Long.class, versionId, itemCode);
    }

    private int sortOrderOf(Long versionId, String itemCode) {
        return jdbcTemplate.queryForObject("""
                SELECT q.sort_order FROM assessment_question q
                JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
                WHERE i.questionnaire_version_id = ? AND i.item_code = ? AND q.deleted = FALSE
                """, Integer.class, versionId, itemCode);
    }

    private Long versionId() {
        return jdbcTemplate.queryForObject("""
                SELECT id FROM assessment_questionnaire_version
                WHERE source_package_code = 'LEXIBRIDGE_RESEARCH_V1' AND deleted = FALSE
                """, Long.class);
    }
}
