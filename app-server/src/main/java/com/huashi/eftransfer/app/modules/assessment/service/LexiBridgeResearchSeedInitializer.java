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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        if (!enabled) {
            return;
        }
        JsonNode seed = readSeed();
        if (exists("assessment_questionnaire", "questionnaire_code", PACKAGE_CODE)) {
            updateResearchPackageContent(seed);
            backfillResearchPaperPurpose();
            return;
        }
        Long ownerId = ensureSystemOwner();
        Long bankId = insertQuestionBank(ownerId, seed);
        Long paperId = insertPaper(ownerId, seed);
        Long questionnaireId = insertQuestionnaire(ownerId, seed);
        Long versionId = insertQuestionnaireVersion(questionnaireId, paperId, seed);
        Long importId = insertImport(bankId, seed);
        insertSectionsAndItems(bankId, paperId, versionId, importId, seed);
        synchronizeCounts(paperId, seed.path("items").size(), seedDurationMinutes(seed));
        insertReviewIssues(bankId, importId, seed);
        log.info("event=lexibridge_seed_ready packageCode={} scoredItems={} formalSections={} basicItems={} status=REVIEW_REQUIRED",
                PACKAGE_CODE, countScoredItems(seed), countFormalSections(seed), countBasicItems(seed));
    }

    private void backfillResearchPaperPurpose() {
        jdbcTemplate.update("""
                UPDATE assessment_paper
                SET paper_purpose = ?
                WHERE paper_code LIKE ? AND deleted = FALSE AND paper_purpose <> ?
                """, AssessmentPaperPurpose.RESEARCH_SURVEY.name(), PACKAGE_CODE + "%",
                AssessmentPaperPurpose.RESEARCH_SURVEY.name());
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
        Long bankId = id("assessment_question_bank", "bank_code", BANK_CODE);

        Set<String> desiredSectionCodes = new LinkedHashSet<>();
        for (JsonNode section : seed.path("sections")) {
            desiredSectionCodes.add(section.path("sectionCode").asString());
        }
        softDeleteRemovedSections(versionId, desiredSectionCodes);
        restoreSoftDeletedSections(versionId, desiredSectionCodes);
        jdbcTemplate.update("""
                UPDATE assessment_questionnaire_section
                SET sort_order = sort_order + 1000
                WHERE questionnaire_version_id = ? AND deleted = FALSE
                """, versionId);

        Map<String, Long> sectionIds = new LinkedHashMap<>();
        for (JsonNode section : seed.path("sections")) {
            String sectionCode = section.path("sectionCode").asString();
            jdbcTemplate.update("""
                    UPDATE assessment_questionnaire_section
                    SET title = ?, description = ?, shared_material = ?, sort_order = ?, formal_section = ?, scored_item_count = ?
                    WHERE questionnaire_version_id = ? AND section_code = ? AND deleted = FALSE
                    """, section.path("title").asString(), nullableText(section, "description"),
                    nullableText(section, "sharedMaterial"), section.path("sortOrder").asInt(),
                    section.path("formalSection").asBoolean(), section.path("scoredItemCount").asInt(),
                    versionId, sectionCode);
            sectionIds.put(sectionCode, jdbcTemplate.queryForObject("""
                    SELECT id FROM assessment_questionnaire_section
                    WHERE questionnaire_version_id = ? AND section_code = ? AND deleted = FALSE
                    """, Long.class, versionId, sectionCode));
        }

        Set<String> desiredItemCodes = new LinkedHashSet<>();
        for (JsonNode item : seed.path("items")) {
            desiredItemCodes.add(item.path("itemCode").asString());
        }
        softDeleteRemovedItems(versionId, paperId, desiredItemCodes);

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
                RestoredItem restored = restoreSoftDeletedItem(versionId, paperId, itemCode);
                if (restored != null) {
                    updateAssessmentQuestion(restored.questionId(), questionVersionId, item, globalOrder);
                    updateQuestionnaireItem(restored.itemId(),
                            sectionIds.get(item.path("sectionCode").asString()), questionVersionId, item);
                } else {
                    Long questionId = insertAssessmentQuestion(paperId, questionVersionId, item, globalOrder);
                    insertQuestionnaireItem(versionId, sectionIds.get(item.path("sectionCode").asString()),
                            questionId, questionVersionId, item);
                }
            } else {
                Long questionId = jdbcTemplate.queryForObject("""
                        SELECT assessment_question_id FROM assessment_questionnaire_item
                        WHERE id = ? AND deleted = FALSE
                        """, Long.class, questionnaireItemId);
                updateAssessmentQuestion(questionId, questionVersionId, item, globalOrder);
                updateQuestionnaireItem(questionnaireItemId,
                        sectionIds.get(item.path("sectionCode").asString()), questionVersionId, item);
            }
            globalOrder++;
        }
        synchronizeCounts(paperId, seed.path("items").size(), seedDurationMinutes(seed));
        log.info("event=lexibridge_seed_updated packageCode={} scoredItems={} formalSections={} basicItems={}",
                PACKAGE_CODE, countScoredItems(seed), countFormalSections(seed), countBasicItems(seed));
    }

    /**
     * Re-activates a soft-deleted questionnaire item together with its paper question so a
     * field removed in an earlier seed run can return without violating the unique keys the
     * soft-deleted rows still occupy. Returns both restored row ids, or null when the item
     * has never existed in this questionnaire version.
     */
    private RestoredItem restoreSoftDeletedItem(Long versionId, Long paperId, String itemCode) {
        List<Long> deletedItemIds = jdbcTemplate.query("""
                SELECT id FROM assessment_questionnaire_item
                WHERE questionnaire_version_id = ? AND item_code = ? AND deleted = TRUE
                ORDER BY id DESC LIMIT 1
                """, (resultSet, rowNumber) -> resultSet.getLong(1), versionId, itemCode);
        if (deletedItemIds.isEmpty()) {
            return null;
        }
        Long itemId = deletedItemIds.getFirst();
        Long questionId = jdbcTemplate.queryForObject("""
                SELECT assessment_question_id FROM assessment_questionnaire_item WHERE id = ?
                """, Long.class, itemId);
        jdbcTemplate.update("UPDATE assessment_questionnaire_item SET deleted = FALSE WHERE id = ?", itemId);
        jdbcTemplate.update("""
                UPDATE assessment_question SET deleted = FALSE
                WHERE id = ? AND paper_id = ? AND deleted = TRUE
                """, questionId, paperId);
        return new RestoredItem(itemId, questionId);
    }

    private void softDeleteRemovedSections(Long versionId, Set<String> desiredSectionCodes) {
        jdbcTemplate.query("""
                        SELECT id, section_code FROM assessment_questionnaire_section
                        WHERE questionnaire_version_id = ? AND deleted = FALSE
                        """, (resultSet, rowNumber) -> Map.entry(resultSet.getLong(1), resultSet.getString(2)), versionId)
                .stream()
                .filter(section -> !desiredSectionCodes.contains(section.getValue()))
                .forEach(section -> jdbcTemplate.update("""
                        UPDATE assessment_questionnaire_section
                        SET sort_order = sort_order + 1000000, deleted = TRUE
                        WHERE id = ?
                        """, section.getKey()));
    }

    /**
     * Re-activates a soft-deleted section so a section removed in an earlier seed run can return
     * without leaving the update loop's deleted=FALSE lookups (and item section_id foreign keys)
     * pointing at nothing. Sort order and metadata are re-applied by the update loop.
     */
    private void restoreSoftDeletedSections(Long versionId, Set<String> desiredSectionCodes) {
        jdbcTemplate.query("""
                        SELECT id, section_code FROM assessment_questionnaire_section
                        WHERE questionnaire_version_id = ? AND deleted = TRUE
                        """, (resultSet, rowNumber) -> Map.entry(resultSet.getLong(1), resultSet.getString(2)), versionId)
                .stream()
                .filter(section -> desiredSectionCodes.contains(section.getValue()))
                .forEach(section -> jdbcTemplate.update("""
                        UPDATE assessment_questionnaire_section
                        SET deleted = FALSE
                        WHERE id = ?
                        """, section.getKey()));
    }

    private void softDeleteRemovedItems(Long versionId, Long paperId, Set<String> desiredItemCodes) {
        jdbcTemplate.query("""
                        SELECT id, assessment_question_id, item_code FROM assessment_questionnaire_item
                        WHERE questionnaire_version_id = ? AND deleted = FALSE
                        """, (resultSet, rowNumber) -> new RemovedItem(
                        resultSet.getLong(1), resultSet.getLong(2), resultSet.getString(3)), versionId)
                .stream()
                .filter(item -> !desiredItemCodes.contains(item.itemCode()))
                .forEach(item -> {
                    jdbcTemplate.update("UPDATE assessment_questionnaire_item SET deleted = TRUE WHERE id = ?", item.itemId());
                    jdbcTemplate.update("""
                            UPDATE assessment_question SET deleted = TRUE
                            WHERE id = ? AND paper_id = ? AND deleted = FALSE
                            """, item.questionId(), paperId);
                });
    }

    private void synchronizeCounts(Long paperId, int itemCount, int durationMinutes) {
        Integer totalScore = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(score), 0) FROM assessment_question
                WHERE paper_id = ? AND deleted = FALSE
                """, Integer.class, paperId);
        jdbcTemplate.update("""
                UPDATE assessment_paper SET question_count = ?, total_score = ?, duration_minutes = ?
                WHERE id = ? AND deleted = FALSE
                """, itemCount, totalScore == null ? 0 : totalScore, durationMinutes, paperId);
        jdbcTemplate.update("""
                UPDATE assessment_publish SET question_count_snapshot = ?, total_score_snapshot = ?, duration_minutes = ?
                WHERE paper_id = ? AND deleted = FALSE
                """, itemCount, totalScore == null ? 0 : totalScore, durationMinutes, paperId);
    }

    private Long upsertQuestionVersion(Long bankId, JsonNode item) throws IOException {
        String questionCode = item.path("itemCode").asString();
        String contentHash = item.path("contentHash").asString();
        List<QuestionVersionRef> existing = jdbcTemplate.query("""
                SELECT id, version_no, content_hash FROM assessment_question_version
                WHERE question_bank_id = ? AND question_code = ? AND deleted = FALSE
                ORDER BY version_no DESC
                """, (resultSet, rowNumber) -> new QuestionVersionRef(
                resultSet.getLong(1), resultSet.getInt(2), resultSet.getString(3)), bankId, questionCode);
        QuestionVersionRef matchingVersion = existing.stream()
                .filter(version -> contentHash.equals(version.contentHash()))
                .findFirst().orElse(null);
        if (matchingVersion != null) return matchingVersion.id();
        Long restoredVersionId = restoreSoftDeletedQuestionVersion(bankId, questionCode, contentHash);
        if (restoredVersionId != null) return restoredVersionId;
        Integer maxVersion = jdbcTemplate.queryForObject("""
                SELECT COALESCE(MAX(version_no), 0) FROM assessment_question_version
                WHERE question_bank_id = ? AND question_code = ?
                """, Integer.class, bankId, questionCode);
        return insertQuestionVersion(bankId, item, maxVersion + 1);
    }

    private Long restoreSoftDeletedQuestionVersion(Long bankId, String questionCode, String contentHash) {
        List<Long> deletedIds = jdbcTemplate.query("""
                SELECT id FROM assessment_question_version
                WHERE question_bank_id = ? AND question_code = ? AND content_hash = ? AND deleted = TRUE
                ORDER BY version_no DESC LIMIT 1
                """, (resultSet, rowNumber) -> resultSet.getLong(1), bankId, questionCode, contentHash);
        if (deletedIds.isEmpty()) {
            return null;
        }
        jdbcTemplate.update("UPDATE assessment_question_version SET deleted = FALSE WHERE id = ?", deletedIds.getFirst());
        return deletedIds.getFirst();
    }

    private void updateAssessmentQuestion(Long questionId, Long questionVersionId, JsonNode item, int sortOrder) throws IOException {
        jdbcTemplate.update("""
                UPDATE assessment_question
                SET question_type = ?, sort_order = ?, stem_text = ?, prompt_text = ?, options_json = ?, correct_answer_json = ?,
                    explanation_text = ?, score = ?, section_code = ?, required_answer = ?, weight = ?,
                    transfer_category = ?, context_level = ?, construct_code = ?, target_word = ?,
                    option_explanations_json = ?, display_condition_json = ?, question_version_id = ?
                WHERE id = ? AND deleted = FALSE
                """, item.path("questionType").asString(), sortOrder, item.path("stemText").asString(),
                nullableText(item, "promptText"), objectMapper.writeValueAsString(item.path("options")),
                objectMapper.writeValueAsString(item.path("correctAnswers")), nullableText(item, "explanationText"),
                item.path("score").asInt(), item.path("sectionCode").asString(), item.path("requiredAnswer").asBoolean(),
                decimal(item, "weight", BigDecimal.ONE), nullableText(item, "transferCategory"),
                nullableText(item, "contextLevel"), nullableText(item, "constructCode"), nullableText(item, "targetWord"),
                objectMapper.writeValueAsString(item.path("optionExplanations")),
                item.path("displayCondition").isNull() ? null : objectMapper.writeValueAsString(item.path("displayCondition")),
                questionVersionId, questionId);
    }

    private void updateQuestionnaireItem(
            Long questionnaireItemId,
            Long sectionId,
            Long questionVersionId,
            JsonNode item
    ) throws IOException {
        jdbcTemplate.update("""
                UPDATE assessment_questionnaire_item
                SET section_id = ?, question_version_id = ?, required_answer = ?, scored = ?, weight = ?, transfer_category = ?, context_level = ?,
                    construct_code = ?, target_word = ?, option_explanations_json = ?, display_condition_json = ?
                WHERE id = ? AND deleted = FALSE
                """, sectionId, questionVersionId, item.path("requiredAnswer").asBoolean(), item.path("scored").asBoolean(),
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
                """, BANK_CODE, "Lexi-Bridge 共享研究题库", seed.path("questionnaire").path("description").asString(),
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
                seedDurationMinutes(seed), scoredCount, scoredCount, ownerId, ownerId);
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
                "scoredItemCount", countScoredItems(seed),
                "formalSectionCount", countFormalSections(seed),
                "basicItemCount", countBasicItems(seed)));
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
            insertQuestionnaireItem(questionnaireVersionId, sectionIds.get(item.path("sectionCode").asString()),
                    assessmentQuestionId, questionVersionId, item);
        }
    }

    private Long insertQuestionVersion(Long bankId, JsonNode item) throws IOException {
        return insertQuestionVersion(bankId, item, 1);
    }

    private Long insertQuestionVersion(Long bankId, JsonNode item, int versionNo) throws IOException {
        jdbcTemplate.update("""
                INSERT INTO assessment_question_version
                    (question_bank_id,question_code,version_no,question_type,stem_text,prompt_text,options_json,
                     correct_answer_json,explanation_text,option_explanations_json,required_answer,weight,
                     transfer_category,context_level,construct_code,target_word,display_condition_json,source_reference,
                     content_hash,created_by,updated_by)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,0,0)
                """, bankId, item.path("itemCode").asString(), versionNo, item.path("questionType").asString(),
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
                WHERE question_bank_id = ? AND question_code = ? AND version_no = ?
                """, Long.class, bankId, item.path("itemCode").asString(), versionNo);
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

    private int seedDurationMinutes(JsonNode seed) {
        return seed.path("questionnaire").path("durationMinutes").asInt(60);
    }

    private int countScoredItems(JsonNode seed) {
        int count = 0;
        for (JsonNode item : seed.path("items")) {
            if (item.path("scored").asBoolean(false)) count++;
        }
        return count;
    }

    private int countFormalSections(JsonNode seed) {
        int count = 0;
        for (JsonNode section : seed.path("sections")) {
            if (section.path("formalSection").asBoolean(false)) count++;
        }
        return count;
    }

    private int countBasicItems(JsonNode seed) {
        int count = 0;
        for (JsonNode item : seed.path("items")) {
            if ("BASIC_INFO".equals(item.path("sectionCode").asString())) count++;
        }
        return count;
    }

    private record RemovedItem(Long itemId, Long questionId, String itemCode) {
    }

    private record RestoredItem(Long itemId, Long questionId) {
    }

    private record QuestionVersionRef(Long id, int versionNo, String contentHash) {
    }
}
