package com.huashi.eftransfer.app.modules.assessment.service;

import com.huashi.eftransfer.shared.enums.AssessmentPaperPurpose;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Seeds the LEXIBRIDGE_RESEARCH_V3 questionnaire from its JSON package.
 *
 * V3 is a separate questionnaire/paper generated from the FF4 V2 four-type
 * question bank. The released V1 package and its rows are never modified by
 * this initializer. The questionnaire content may be created in APPROVED
 * state, but the paper remains DRAFT and is never published automatically.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 99)
public class LexiBridgeResearchV3SeedInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LexiBridgeResearchV3SeedInitializer.class);
    private static final String PACKAGE_CODE = "LEXIBRIDGE_RESEARCH_V3";
    private static final String BANK_CODE = "LEXIBRIDGE_FF4_V2";
    private static final String RESOURCE = "assessment-seeds/LEXIBRIDGE_RESEARCH_V3.json";
    private static final String SYSTEM_USERNAME = "lexibridge.seed";

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder;
    private final boolean enabled;

    public LexiBridgeResearchV3SeedInitializer(
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper,
            PasswordEncoder passwordEncoder,
            @Value("${app.assessment.seed.lexibridge-v3-enabled:false}") boolean enabled
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws IOException {
        if (!enabled) {
            return;
        }
        JsonNode seed = readSeed();
        if (exists("assessment_questionnaire", "questionnaire_code", PACKAGE_CODE)) {
            updateResearchPackageContent(seed);
            return;
        }
        Long ownerId = ensureSystemOwner();
        Long bankId = insertQuestionBank(ownerId, seed);
        Long paperId = insertPaper(ownerId, seed);
        Long questionnaireId = insertQuestionnaire(ownerId, seed);
        Long versionId = insertQuestionnaireVersion(questionnaireId, paperId, seed);
        Long importId = insertImport(bankId, seed);
        insertSectionsAndItems(bankId, paperId, versionId, importId, seed);
        insertReviewIssues(bankId, importId, seed);
        log.info("event=lexibridge_v3_seed_ready packageCode={} status={}", PACKAGE_CODE, seedStatus(seed));
    }

    private void updateResearchPackageContent(JsonNode seed) throws IOException {
        Long questionnaireId = id("assessment_questionnaire", "questionnaire_code", PACKAGE_CODE);
        Long versionId = jdbcTemplate.queryForObject("""
                SELECT id FROM assessment_questionnaire_version
                WHERE questionnaire_id = ? AND deleted = FALSE ORDER BY version_no DESC LIMIT 1
                """, Long.class, questionnaireId);
        Long paperId = jdbcTemplate.queryForObject("""
                SELECT paper_id FROM assessment_questionnaire_version WHERE id = ? AND deleted = FALSE
                """, Long.class, versionId);
        if (!isMutableDraft(questionnaireId, versionId, paperId)) {
            log.warn("event=lexibridge_v3_seed_update_skipped packageCode={} reason=immutable_or_published", PACKAGE_CODE);
            return;
        }
        Long bankId = id("assessment_question_bank", "bank_code", BANK_CODE);

        Map<String, Long> sectionIds = new LinkedHashMap<>();
        for (JsonNode section : seed.path("sections")) {
            String sectionCode = section.path("sectionCode").asString();
            jdbcTemplate.update("""
                    UPDATE assessment_questionnaire_section
                    SET title = ?, description = ?, shared_material = ?, scored_item_count = ?
                    WHERE questionnaire_version_id = ? AND section_code = ? AND deleted = FALSE
                    """, section.path("title").asString(), nullableText(section, "description"),
                    nullableText(section, "sharedMaterial"), section.path("scoredItemCount").asInt(),
                    versionId, sectionCode);
            sectionIds.put(sectionCode, jdbcTemplate.queryForObject("""
                    SELECT id FROM assessment_questionnaire_section
                    WHERE questionnaire_version_id = ? AND section_code = ? AND deleted = FALSE
                    """, Long.class, versionId, sectionCode));
        }

        int globalOrder = 1;
        for (JsonNode item : seed.path("items")) {
            String itemCode = item.path("itemCode").asString();
            Long questionnaireItemId = jdbcTemplate.query("""
                    SELECT id FROM assessment_questionnaire_item
                    WHERE questionnaire_version_id = ? AND item_code = ? AND deleted = FALSE
                    """, (resultSet, rowNumber) -> resultSet.getLong(1), versionId, itemCode)
                    .stream().findFirst().orElse(null);
            Long questionVersionId = upsertQuestionVersion(bankId, item);
            if (questionnaireItemId == null) {
                jdbcTemplate.update("""
                        UPDATE assessment_question
                        SET sort_order = sort_order + 1
                        WHERE paper_id = ? AND sort_order >= ? AND deleted = FALSE
                        """, paperId, globalOrder);
                Long questionId = insertAssessmentQuestion(paperId, questionVersionId, item, globalOrder);
                insertQuestionnaireItem(versionId, sectionIds.get(item.path("sectionCode").asString()),
                        questionId, questionVersionId, item);
            } else {
                Long questionId = jdbcTemplate.queryForObject("""
                        SELECT assessment_question_id FROM assessment_questionnaire_item
                        WHERE id = ? AND deleted = FALSE
                        """, Long.class, questionnaireItemId);
                updateAssessmentQuestion(questionId, item);
                updateQuestionnaireItem(questionnaireItemId, item);
            }
            globalOrder++;
        }
        String status = seedStatus(seed);
        jdbcTemplate.update("UPDATE assessment_questionnaire SET status = ? WHERE id = ? AND deleted = FALSE",
                status, questionnaireId);
        jdbcTemplate.update("UPDATE assessment_questionnaire_version SET status = ? WHERE id = ? AND deleted = FALSE",
                status, versionId);
        log.info("event=lexibridge_v3_seed_updated packageCode={}", PACKAGE_CODE);
    }

    private boolean isMutableDraft(Long questionnaireId, Long versionId, Long paperId) {
        String questionnaireStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM assessment_questionnaire WHERE id = ? AND deleted = FALSE",
                String.class, questionnaireId);
        String versionStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM assessment_questionnaire_version WHERE id = ? AND deleted = FALSE",
                String.class, versionId);
        String paperStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM assessment_paper WHERE id = ? AND deleted = FALSE",
                String.class, paperId);
        Integer publishCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM assessment_publish WHERE paper_id = ? AND deleted = FALSE",
                Integer.class, paperId);
        return ("REVIEW_REQUIRED".equalsIgnoreCase(questionnaireStatus)
                || "DRAFT".equalsIgnoreCase(questionnaireStatus))
                && ("REVIEW_REQUIRED".equalsIgnoreCase(versionStatus) || "DRAFT".equalsIgnoreCase(versionStatus))
                && "DRAFT".equalsIgnoreCase(paperStatus)
                && (publishCount == null || publishCount == 0);
    }

    private Long upsertQuestionVersion(Long bankId, JsonNode item) throws IOException {
        List<Long> existing = jdbcTemplate.query("""
                SELECT id FROM assessment_question_version
                WHERE question_bank_id = ? AND question_code = ? AND version_no = 1 AND deleted = FALSE
                """, (resultSet, rowNumber) -> resultSet.getLong(1), bankId, item.path("itemCode").asString());
        if (existing.isEmpty()) {
            return insertQuestionVersion(bankId, item);
        }
        Long id = existing.getFirst();
        jdbcTemplate.update("""
                UPDATE assessment_question_version
                SET question_type = ?, stem_text = ?, prompt_text = ?, options_json = ?, correct_answer_json = ?,
                    explanation_text = ?, option_explanations_json = ?, required_answer = ?, weight = ?,
                    transfer_category = ?, context_level = ?, construct_code = ?, target_word = ?,
                    display_condition_json = ?, source_reference = ?, content_hash = ?
                WHERE id = ? AND deleted = FALSE
                """, item.path("questionType").asString(), item.path("stemText").asString(),
                nullableText(item, "promptText"), objectMapper.writeValueAsString(item.path("options")),
                objectMapper.writeValueAsString(item.path("correctAnswers")), nullableText(item, "explanationText"),
                objectMapper.writeValueAsString(item.path("optionExplanations")), item.path("requiredAnswer").asBoolean(),
                decimal(item, "weight", BigDecimal.ONE), nullableText(item, "transferCategory"),
                nullableText(item, "contextLevel"), nullableText(item, "constructCode"), nullableText(item, "targetWord"),
                item.path("displayCondition").isNull() ? null : objectMapper.writeValueAsString(item.path("displayCondition")),
                nullableText(item, "sourceReference"), item.path("contentHash").asString(), id);
        return id;
    }

    private void updateAssessmentQuestion(Long questionId, JsonNode item) throws IOException {
        jdbcTemplate.update("""
                UPDATE assessment_question
                SET question_type = ?, stem_text = ?, prompt_text = ?, options_json = ?, correct_answer_json = ?,
                    explanation_text = ?, score = ?, section_code = ?, required_answer = ?, weight = ?,
                    transfer_category = ?, context_level = ?, construct_code = ?, target_word = ?,
                    option_explanations_json = ?, display_condition_json = ?
                WHERE id = ? AND deleted = FALSE
                """, item.path("questionType").asString(), item.path("stemText").asString(),
                nullableText(item, "promptText"), objectMapper.writeValueAsString(item.path("options")),
                objectMapper.writeValueAsString(item.path("correctAnswers")), nullableText(item, "explanationText"),
                item.path("score").asInt(), item.path("sectionCode").asString(), item.path("requiredAnswer").asBoolean(),
                decimal(item, "weight", BigDecimal.ONE), nullableText(item, "transferCategory"),
                nullableText(item, "contextLevel"), nullableText(item, "constructCode"), nullableText(item, "targetWord"),
                objectMapper.writeValueAsString(item.path("optionExplanations")),
                item.path("displayCondition").isNull() ? null : objectMapper.writeValueAsString(item.path("displayCondition")),
                questionId);
    }

    private void updateQuestionnaireItem(Long questionnaireItemId, JsonNode item) throws IOException {
        jdbcTemplate.update("""
                UPDATE assessment_questionnaire_item
                SET required_answer = ?, scored = ?, weight = ?, transfer_category = ?, context_level = ?,
                    construct_code = ?, target_word = ?, option_explanations_json = ?, display_condition_json = ?
                WHERE id = ? AND deleted = FALSE
                """, item.path("requiredAnswer").asBoolean(), item.path("scored").asBoolean(),
                decimal(item, "weight", BigDecimal.ONE), nullableText(item, "transferCategory"),
                nullableText(item, "contextLevel"), nullableText(item, "constructCode"), nullableText(item, "targetWord"),
                objectMapper.writeValueAsString(item.path("optionExplanations")),
                item.path("displayCondition").isNull() ? null : objectMapper.writeValueAsString(item.path("displayCondition")),
                questionnaireItemId);
    }

    private void insertQuestionnaireItem(
            Long questionnaireVersionId,
            Long sectionId,
            Long assessmentQuestionId,
            Long questionVersionId,
            JsonNode item
    ) throws IOException {
        jdbcTemplate.update("""
                INSERT INTO assessment_questionnaire_item
                    (questionnaire_version_id,section_id,assessment_question_id,question_version_id,item_code,
                     required_answer,scored,weight,transfer_category,context_level,construct_code,target_word,
                     option_explanations_json,display_condition_json,created_by,updated_by)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,0,0)
                """, questionnaireVersionId, sectionId, assessmentQuestionId, questionVersionId,
                item.path("itemCode").asString(), item.path("requiredAnswer").asBoolean(), item.path("scored").asBoolean(),
                decimal(item, "weight", BigDecimal.ONE), nullableText(item, "transferCategory"),
                nullableText(item, "contextLevel"), nullableText(item, "constructCode"), nullableText(item, "targetWord"),
                objectMapper.writeValueAsString(item.path("optionExplanations")),
                item.path("displayCondition").isNull() ? null : objectMapper.writeValueAsString(item.path("displayCondition")));
    }

    private JsonNode readSeed() throws IOException {
        ClassPathResource resource = new ClassPathResource(RESOURCE);
        try (var input = resource.getInputStream()) {
            return objectMapper.readTree(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    private Long ensureSystemOwner() {
        List<Long> existing = jdbcTemplate.query(
                "SELECT id FROM users WHERE username = ? AND deleted = FALSE LIMIT 1",
                (resultSet, rowNumber) -> resultSet.getLong(1), SYSTEM_USERNAME);
        if (!existing.isEmpty()) {
            return existing.getFirst();
        }
        jdbcTemplate.update("""
                INSERT INTO users (username,email,password_hash,display_name,enabled,created_by,updated_by)
                VALUES (?,?,?,?,FALSE,0,0)
                """, SYSTEM_USERNAME, "lexibridge.seed@system.invalid",
                passwordEncoder.encode(UUID.randomUUID().toString()), "Lexi-Bridge Seed Owner");
        return jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = ?", Long.class, SYSTEM_USERNAME);
    }

    private Long insertQuestionBank(Long ownerId, JsonNode seed) {
        jdbcTemplate.update("""
                INSERT INTO assessment_question_bank
                    (bank_code,name,description,owner_user_id,visibility,status,created_by,updated_by)
                VALUES (?,?,?,?, 'SHARED','ACTIVE',?,?)
                """, BANK_CODE, "Lexi-Bridge FF4 V2 四类题题库", seed.path("questionnaire").path("description").asString(),
                ownerId, ownerId, ownerId);
        return id("assessment_question_bank", "bank_code", BANK_CODE);
    }

    private Long insertPaper(Long ownerId, JsonNode seed) {
        JsonNode questionnaire = seed.path("questionnaire");
        int scoredCount = countScoredItems(seed);
        jdbcTemplate.update("""
                INSERT INTO assessment_paper
                    (paper_code,title,description,owner_user_id,paper_purpose,status,duration_minutes,question_count,total_score,created_by,updated_by)
                VALUES (?,?,?,?,?,?,?,?,?,?,?)
                """, PACKAGE_CODE, questionnaire.path("title").asString(), questionnaire.path("description").asString(),
                ownerId, AssessmentPaperPurpose.RESEARCH_SURVEY.name(), "DRAFT",
                questionnaire.path("durationMinutes").asInt(40), scoredCount, scoredCount, ownerId, ownerId);
        return id("assessment_paper", "paper_code", PACKAGE_CODE);
    }

    private int countScoredItems(JsonNode seed) {
        int count = 0;
        for (JsonNode item : seed.path("items")) {
            if (item.path("scored").asBoolean(false)) {
                count++;
            }
        }
        return count;
    }

    private long countFormalSections(JsonNode seed) {
        long count = 0;
        for (JsonNode section : seed.path("sections")) {
            if (section.path("formalSection").asBoolean(false)) {
                count++;
            }
        }
        return count;
    }

    private Long insertQuestionnaire(Long ownerId, JsonNode seed) {
        JsonNode questionnaire = seed.path("questionnaire");
        jdbcTemplate.update("""
                INSERT INTO assessment_questionnaire
                    (questionnaire_code,title,description,owner_user_id,status,latest_version_no,created_by,updated_by)
                VALUES (?,?,?,?, ?,1,?,?)
                """, PACKAGE_CODE, questionnaire.path("title").asString(), questionnaire.path("description").asString(),
                ownerId, seedStatus(seed), ownerId, ownerId);
        return id("assessment_questionnaire", "questionnaire_code", PACKAGE_CODE);
    }

    private Long insertQuestionnaireVersion(Long questionnaireId, Long paperId, JsonNode seed) {
        JsonNode questionnaire = seed.path("questionnaire");
        jdbcTemplate.update("""
                INSERT INTO assessment_questionnaire_version
                    (questionnaire_id,paper_id,version_no,status,scoring_version,ai_prompt_version,source_package_code,created_by,updated_by)
                VALUES (?,?,1,?,?,?,?,?,?)
                """, questionnaireId, paperId, seedStatus(seed), questionnaire.path("scoringVersion").asString(),
                questionnaire.path("aiPromptVersion").asString(), PACKAGE_CODE, 0L, 0L);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM assessment_questionnaire_version WHERE questionnaire_id = ? AND version_no = 1",
                Long.class, questionnaireId);
    }

    private Long insertImport(Long bankId, JsonNode seed) throws IOException {
        String sourcePayload = objectMapper.writeValueAsString(seed);
        String summary = objectMapper.writeValueAsString(Map.of(
                "packageCode", PACKAGE_CODE,
                "scoredItemCount", countScoredItems(seed),
                "formalSectionCount", (int) countFormalSections(seed)));
        String differences = objectMapper.writeValueAsString(seed.path("reviewIssues"));
        jdbcTemplate.update("""
                INSERT INTO assessment_question_bank_import
                    (question_bank_id,import_key,source_file_name,source_format,source_sha256,status,
                     source_payload_json,preflight_summary_json,differences_json,errors_json,created_by,updated_by)
                VALUES (?,?,?,?,?,?,?,?,?,?,0,0)
                """, bankId, PACKAGE_CODE, "LEXIBRIDGE_RESEARCH_V3.json", "JSON_PACKAGE",
                seed.path("source").path("questionnaireSha256").asString(),
                "APPROVED".equals(seedStatus(seed)) ? "READY" : "REVIEW_REQUIRED",
                sourcePayload, summary, differences, "[]");
        return id("assessment_question_bank_import", "import_key", PACKAGE_CODE);
    }

    private void insertSectionsAndItems(
            Long bankId,
            Long paperId,
            Long questionnaireVersionId,
            Long importId,
            JsonNode seed
    ) throws IOException {
        Map<String, Long> sectionIds = new LinkedHashMap<>();
        for (JsonNode section : seed.path("sections")) {
            jdbcTemplate.update("""
                    INSERT INTO assessment_questionnaire_section
                        (questionnaire_version_id,section_code,title,description,shared_material,sort_order,
                         formal_section,scored_item_count,created_by,updated_by)
                    VALUES (?,?,?,?,?,?,?,?,0,0)
                    """, questionnaireVersionId, section.path("sectionCode").asString(), section.path("title").asString(),
                    nullableText(section, "description"), nullableText(section, "sharedMaterial"),
                    section.path("sortOrder").asInt(), section.path("formalSection").asBoolean(),
                    section.path("scoredItemCount").asInt());
            Long sectionId = jdbcTemplate.queryForObject("""
                    SELECT id FROM assessment_questionnaire_section
                    WHERE questionnaire_version_id = ? AND section_code = ?
                    """, Long.class, questionnaireVersionId, section.path("sectionCode").asString());
            sectionIds.put(section.path("sectionCode").asString(), sectionId);
        }

        int globalOrder = 1;
        for (JsonNode item : seed.path("items")) {
            Long questionVersionId = insertQuestionVersion(bankId, item);
            Long assessmentQuestionId = insertAssessmentQuestion(paperId, questionVersionId, item, globalOrder++);
            insertQuestionnaireItem(questionnaireVersionId, sectionIds.get(item.path("sectionCode").asString()),
                    assessmentQuestionId, questionVersionId, item);
        }
    }

    private Long insertQuestionVersion(Long bankId, JsonNode item) throws IOException {
        jdbcTemplate.update("""
                INSERT INTO assessment_question_version
                    (question_bank_id,question_code,version_no,question_type,stem_text,prompt_text,options_json,
                     correct_answer_json,explanation_text,option_explanations_json,required_answer,weight,
                     transfer_category,context_level,construct_code,target_word,display_condition_json,source_reference,
                     content_hash,created_by,updated_by)
                VALUES (?,?,1,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,0,0)
                """, bankId, item.path("itemCode").asString(), item.path("questionType").asString(),
                item.path("stemText").asString(), nullableText(item, "promptText"),
                objectMapper.writeValueAsString(item.path("options")), objectMapper.writeValueAsString(item.path("correctAnswers")),
                nullableText(item, "explanationText"), objectMapper.writeValueAsString(item.path("optionExplanations")),
                item.path("requiredAnswer").asBoolean(), decimal(item, "weight", BigDecimal.ONE),
                nullableText(item, "transferCategory"), nullableText(item, "contextLevel"),
                nullableText(item, "constructCode"), nullableText(item, "targetWord"),
                item.path("displayCondition").isNull() ? null : objectMapper.writeValueAsString(item.path("displayCondition")),
                nullableText(item, "sourceReference"), item.path("contentHash").asString());
        return jdbcTemplate.queryForObject("""
                SELECT id FROM assessment_question_version
                WHERE question_bank_id = ? AND question_code = ? AND version_no = 1
                """, Long.class, bankId, item.path("itemCode").asString());
    }

    private Long insertAssessmentQuestion(Long paperId, Long questionVersionId, JsonNode item, int sortOrder) throws IOException {
        jdbcTemplate.update("""
                INSERT INTO assessment_question
                    (paper_id,question_type,sort_order,stem_text,prompt_text,options_json,correct_answer_json,
                     explanation_text,score,question_version_id,section_code,required_answer,weight,transfer_category,
                     context_level,construct_code,target_word,option_explanations_json,display_condition_json,created_by,updated_by)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """, paperId, item.path("questionType").asString(), sortOrder, item.path("stemText").asString(),
                nullableText(item, "promptText"), objectMapper.writeValueAsString(item.path("options")),
                objectMapper.writeValueAsString(item.path("correctAnswers")), nullableText(item, "explanationText"),
                item.path("score").asInt(), questionVersionId, item.path("sectionCode").asString(),
                item.path("requiredAnswer").asBoolean(), decimal(item, "weight", BigDecimal.ONE),
                nullableText(item, "transferCategory"), nullableText(item, "contextLevel"),
                nullableText(item, "constructCode"), nullableText(item, "targetWord"),
                objectMapper.writeValueAsString(item.path("optionExplanations")),
                item.path("displayCondition").isNull() ? null : objectMapper.writeValueAsString(item.path("displayCondition")),
                0L, 0L);
        return jdbcTemplate.queryForObject("""
                SELECT id FROM assessment_question WHERE paper_id = ? AND sort_order = ? AND deleted = FALSE
                """, Long.class, paperId, sortOrder);
    }

    private void insertReviewIssues(Long bankId, Long importId, JsonNode seed) throws IOException {
        for (JsonNode issue : seed.path("reviewIssues")) {
            Long questionVersionId = null;
            if (issue.hasNonNull("itemCode")) {
                List<Long> ids = jdbcTemplate.query("""
                        SELECT id FROM assessment_question_version
                        WHERE question_bank_id = ? AND question_code = ? AND version_no = 1 AND deleted = FALSE
                        """, (resultSet, rowNumber) -> resultSet.getLong(1), bankId, issue.path("itemCode").asString());
                questionVersionId = ids.isEmpty() ? null : ids.getFirst();
            }
            jdbcTemplate.update("""
                    INSERT INTO assessment_content_review_issue
                        (import_id,question_version_id,issue_code,severity,status,source_reference,description,source_value_json,
                         candidate_value_json,created_by,updated_by)
                    VALUES (?,?,?,?,'OPEN',?,?,?,NULL,0,0)
                    """, importId, questionVersionId, issue.path("issueCode").asString(), issue.path("severity").asString(),
                    issue.hasNonNull("itemCode") ? issue.path("itemCode").asString() : PACKAGE_CODE,
                    issue.path("description").asString(), objectMapper.writeValueAsString(issue));
        }
    }

    private boolean exists(String table, String column, String value) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE " + column + " = ? AND deleted = FALSE",
                Integer.class, value);
        return count != null && count > 0;
    }

    private Long id(String table, String column, String value) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM " + table + " WHERE " + column + " = ? AND deleted = FALSE",
                Long.class, value);
    }

    private String nullableText(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() || value.asString().isBlank() ? null : value.asString();
    }

    private BigDecimal decimal(JsonNode node, String field, BigDecimal defaultValue) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? defaultValue : value.decimalValue();
    }

    private String seedStatus(JsonNode seed) {
        String status = seed.path("questionnaire").path("status").asString("REVIEW_REQUIRED").toUpperCase();
        return "APPROVED".equals(status) ? "APPROVED" : "REVIEW_REQUIRED";
    }
}
