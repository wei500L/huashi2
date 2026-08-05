package com.huashi.eftransfer.app.modules.assessment.service;

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
import java.util.List;
import java.util.UUID;

@Component
@Order(Ordered.LOWEST_PRECEDENCE - 100)
public class LexiBridgeResearchSeedInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LexiBridgeResearchSeedInitializer.class);
    private static final String PACKAGE_CODE = "LEXIBRIDGE_RESEARCH_V1";
    private static final String BANK_CODE = "LEXIBRIDGE_SHARED";
    private static final String RESOURCE = "assessment-seeds/LEXIBRIDGE_RESEARCH_V1.json";
    private static final String SYSTEM_USERNAME = "lexibridge.seed";

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder;
    private final boolean enabled;

    public LexiBridgeResearchSeedInitializer(
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper,
            PasswordEncoder passwordEncoder,
            @Value("${app.assessment.seed.lexibridge-enabled:true}") boolean enabled
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws IOException {
        if (!enabled || exists("assessment_questionnaire", "questionnaire_code", PACKAGE_CODE)) {
            return;
        }
        JsonNode seed = readSeed();
        Long ownerId = ensureSystemOwner();
        Long bankId = insertQuestionBank(ownerId, seed);
        Long paperId = insertPaper(ownerId, seed);
        Long questionnaireId = insertQuestionnaire(ownerId, seed);
        Long versionId = insertQuestionnaireVersion(questionnaireId, paperId, seed);
        Long importId = insertImport(bankId, seed);
        insertSectionsAndItems(bankId, paperId, versionId, importId, seed);
        insertReviewIssues(bankId, importId, seed);
        log.info("event=lexibridge_seed_ready packageCode={} scoredItems=60 formalSections=7 status=REVIEW_REQUIRED",
                PACKAGE_CODE);
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
                """, BANK_CODE, "Lexi-Bridge 共享研究题库", seed.path("questionnaire").path("description").asString(),
                ownerId, ownerId, ownerId);
        return id("assessment_question_bank", "bank_code", BANK_CODE);
    }

    private Long insertPaper(Long ownerId, JsonNode seed) {
        JsonNode questionnaire = seed.path("questionnaire");
        jdbcTemplate.update("""
                INSERT INTO assessment_paper
                    (paper_code,title,description,owner_user_id,status,duration_minutes,question_count,total_score,created_by,updated_by)
                VALUES (?,?,?,?,?,?,?,?,?,?)
                """, PACKAGE_CODE, questionnaire.path("title").asString(), questionnaire.path("description").asString(),
                ownerId, "DRAFT", questionnaire.path("durationMinutes").asInt(40), 60, 60, ownerId, ownerId);
        return id("assessment_paper", "paper_code", PACKAGE_CODE);
    }

    private Long insertQuestionnaire(Long ownerId, JsonNode seed) {
        JsonNode questionnaire = seed.path("questionnaire");
        jdbcTemplate.update("""
                INSERT INTO assessment_questionnaire
                    (questionnaire_code,title,description,owner_user_id,status,latest_version_no,created_by,updated_by)
                VALUES (?,?,?,?, 'REVIEW_REQUIRED',1,?,?)
                """, PACKAGE_CODE, questionnaire.path("title").asString(), questionnaire.path("description").asString(),
                ownerId, ownerId, ownerId);
        return id("assessment_questionnaire", "questionnaire_code", PACKAGE_CODE);
    }

    private Long insertQuestionnaireVersion(Long questionnaireId, Long paperId, JsonNode seed) {
        JsonNode questionnaire = seed.path("questionnaire");
        jdbcTemplate.update("""
                INSERT INTO assessment_questionnaire_version
                    (questionnaire_id,paper_id,version_no,status,scoring_version,ai_prompt_version,source_package_code,created_by,updated_by)
                VALUES (?,?,1,'REVIEW_REQUIRED',?,?,?,?,?)
                """, questionnaireId, paperId, questionnaire.path("scoringVersion").asString(),
                questionnaire.path("aiPromptVersion").asString(), PACKAGE_CODE, 0L, 0L);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM assessment_questionnaire_version WHERE questionnaire_id = ? AND version_no = 1",
                Long.class, questionnaireId);
    }

    private Long insertImport(Long bankId, JsonNode seed) throws IOException {
        JsonNode source = seed.path("source");
        String sourcePayload = objectMapper.writeValueAsString(seed);
        String summary = objectMapper.writeValueAsString(java.util.Map.of(
                "packageCode", PACKAGE_CODE,
                "scoredItemCount", 60,
                "formalSectionCount", 7,
                "basicItemCount", 11));
        String differences = objectMapper.writeValueAsString(seed.path("reviewIssues"));
        jdbcTemplate.update("""
                INSERT INTO assessment_question_bank_import
                    (question_bank_id,import_key,source_file_name,source_format,source_sha256,status,
                     source_payload_json,preflight_summary_json,differences_json,errors_json,created_by,updated_by)
                VALUES (?,?,?,?,?,'REVIEW_REQUIRED',?,?,?,?,0,0)
                """, bankId, PACKAGE_CODE, "Lexi-bridge 大创测试题目编写2.docx", "DOCX_PAIR",
                source.path("questionnaireSha256").asString(), sourcePayload, summary, differences, "[]");
        return id("assessment_question_bank_import", "import_key", PACKAGE_CODE);
    }

    private void insertSectionsAndItems(
            Long bankId,
            Long paperId,
            Long questionnaireVersionId,
            Long importId,
            JsonNode seed
    ) throws IOException {
        java.util.Map<String, Long> sectionIds = new java.util.LinkedHashMap<>();
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
            jdbcTemplate.update("""
                    INSERT INTO assessment_questionnaire_item
                        (questionnaire_version_id,section_id,assessment_question_id,question_version_id,item_code,
                         required_answer,scored,weight,transfer_category,context_level,construct_code,target_word,
                         option_explanations_json,display_condition_json,created_by,updated_by)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,0,0)
                    """, questionnaireVersionId, sectionIds.get(item.path("sectionCode").asString()), assessmentQuestionId,
                    questionVersionId, item.path("itemCode").asString(), item.path("requiredAnswer").asBoolean(),
                    item.path("scored").asBoolean(), decimal(item, "weight", BigDecimal.ONE),
                    nullableText(item, "transferCategory"), nullableText(item, "contextLevel"),
                    nullableText(item, "constructCode"), nullableText(item, "targetWord"),
                    objectMapper.writeValueAsString(item.path("optionExplanations")),
                    item.path("displayCondition").isNull() ? null : objectMapper.writeValueAsString(item.path("displayCondition")));
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
            Long questionVersionId = jdbcTemplate.queryForObject("""
                    SELECT id FROM assessment_question_version
                    WHERE question_bank_id = ? AND question_code = ? AND version_no = 1 AND deleted = FALSE
                    """, Long.class, bankId, issue.path("itemCode").asString());
            jdbcTemplate.update("""
                    INSERT INTO assessment_content_review_issue
                        (import_id,question_version_id,issue_code,severity,status,source_reference,description,source_value_json,
                         candidate_value_json,created_by,updated_by)
                    VALUES (?,?,?,?,'OPEN',?,?,?,NULL,0,0)
                    """, importId, questionVersionId, issue.path("issueCode").asString(), issue.path("severity").asString(),
                    issue.path("itemCode").asString(), issue.path("description").asString(),
                    objectMapper.writeValueAsString(issue));
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
}
