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
import java.util.Map;
import java.util.UUID;

/**
 * Seeds the FF4 V2 four-type question bank (LEXIBRIDGE_FF4_V2) from the
 * LEXIBRIDGE_RESEARCH_V3 JSON package, for the student self-practice module.
 *
 * The same JSON package that once produced the LEXIBRIDGE_RESEARCH_V3
 * research questionnaire is now consumed as a plain question bank only:
 * no questionnaire / paper / section rows are created and the package never
 * appears in the research release flow (V1 remains the only research
 * questionnaire). Practice sessions snapshot these bank questions directly.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 99)
public class LexiBridgePracticeBankSeedInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LexiBridgePracticeBankSeedInitializer.class);
    private static final String PACKAGE_CODE = "LEXIBRIDGE_RESEARCH_V3";
    private static final String BANK_CODE = "LEXIBRIDGE_FF4_V2";
    private static final String RESOURCE = "assessment-seeds/LEXIBRIDGE_RESEARCH_V3.json";
    private static final String SYSTEM_USERNAME = "lexibridge.seed";
    private static final String LEGACY_QUESTIONNAIRE_CODE = "LEXIBRIDGE_RESEARCH_V3";

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder;
    private final boolean enabled;

    public LexiBridgePracticeBankSeedInitializer(
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
        warnAboutLegacyQuestionnaire();
        JsonNode seed = readSeed();
        Long ownerId = ensureSystemOwner();
        Long bankId = ensureQuestionBank(ownerId, seed);
        syncQuestionVersions(bankId, seed);
        ensureImport(bankId, seed);
        insertReviewIssues(bankId, seed);
        log.info("event=lexibridge_practice_bank_ready bankCode={} items={} scoredItems={} formalSections={}",
                BANK_CODE, seed.path("items").size(), countScoredItems(seed), countFormalSections(seed));
    }

    /**
     * Databases created before the practice-bank refactor may still hold the
     * legacy LEXIBRIDGE_RESEARCH_V3 questionnaire rows. They are left intact
     * (never auto-deleted) and must be removed manually with the standard
     * schema reset flow so the package stops appearing in the research flow.
     */
    private void warnAboutLegacyQuestionnaire() {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM assessment_questionnaire
                WHERE questionnaire_code = ? AND deleted = FALSE
                """, Integer.class, LEGACY_QUESTIONNAIRE_CODE);
        if (count != null && count > 0) {
            log.warn("event=lexibridge_practice_bank_legacy_questionnaire_detected questionnaireCode={} "
                    + "reason=v3_questionnaire_removed_use_schema_reset", LEGACY_QUESTIONNAIRE_CODE);
        }
    }

    private Long ensureQuestionBank(Long ownerId, JsonNode seed) {
        List<Long> existing = jdbcTemplate.query(
                "SELECT id FROM assessment_question_bank WHERE bank_code = ? AND deleted = FALSE",
                (resultSet, rowNumber) -> resultSet.getLong(1), BANK_CODE);
        if (!existing.isEmpty()) {
            return existing.getFirst();
        }
        jdbcTemplate.update("""
                INSERT INTO assessment_question_bank
                    (bank_code,name,description,owner_user_id,visibility,status,created_by,updated_by)
                VALUES (?,?,?,?, 'SHARED','ACTIVE',?,?)
                """, BANK_CODE, "Lexi-Bridge FF4 V2 四类题题库", seed.path("questionnaire").path("description").asString(),
                ownerId, ownerId, ownerId);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM assessment_question_bank WHERE bank_code = ?", Long.class, BANK_CODE);
    }

    private void syncQuestionVersions(Long bankId, JsonNode seed) throws IOException {
        java.util.Set<String> desiredCodes = new java.util.LinkedHashSet<>();
        for (JsonNode item : seed.path("items")) {
            desiredCodes.add(item.path("itemCode").asString());
            upsertQuestionVersion(bankId, item);
        }
        softDeleteRemovedQuestionVersions(bankId, desiredCodes);
    }

    /**
     * Soft-deletes every question version whose code no longer exists in the
     * seed package, so the practice bank mirrors the package exactly and stale
     * items (dropped from an earlier package revision) never surface in the
     * student self-practice module.
     */
    private void softDeleteRemovedQuestionVersions(Long bankId, java.util.Set<String> desiredCodes) {
        jdbcTemplate.query("""
                        SELECT id, question_code FROM assessment_question_version
                        WHERE question_bank_id = ? AND deleted = FALSE
                        """, (resultSet, rowNumber) -> Map.entry(resultSet.getLong(1), resultSet.getString(2)), bankId)
                .stream()
                .filter(entry -> !desiredCodes.contains(entry.getValue()))
                .forEach(entry -> jdbcTemplate.update("""
                        UPDATE assessment_question_version
                        SET deleted = TRUE, updated_at = CURRENT_TIMESTAMP
                        WHERE id = ?
                        """, entry.getKey()));
    }

    /**
     * Idempotently inserts the newest question version for an item: a version
     * whose content hash already exists is kept, soft-deleted versions with
     * the same hash are restored, otherwise a new version_no is created.
     */
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
        if (matchingVersion != null) {
            return matchingVersion.id();
        }
        List<Long> deletedIds = jdbcTemplate.query("""
                SELECT id FROM assessment_question_version
                WHERE question_bank_id = ? AND question_code = ? AND content_hash = ? AND deleted = TRUE
                ORDER BY version_no DESC LIMIT 1
                """, (resultSet, rowNumber) -> resultSet.getLong(1), bankId, questionCode, contentHash);
        if (!deletedIds.isEmpty()) {
            jdbcTemplate.update("UPDATE assessment_question_version SET deleted = FALSE WHERE id = ?", deletedIds.getFirst());
            return deletedIds.getFirst();
        }
        Integer maxVersion = jdbcTemplate.queryForObject("""
                SELECT COALESCE(MAX(version_no), 0) FROM assessment_question_version
                WHERE question_bank_id = ? AND question_code = ?
                """, Integer.class, bankId, questionCode);
        return insertQuestionVersion(bankId, item, maxVersion + 1);
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

    private void ensureImport(Long bankId, JsonNode seed) throws IOException {
        Integer existing = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM assessment_question_bank_import WHERE import_key = ? AND deleted = FALSE",
                Integer.class, PACKAGE_CODE);
        if (existing != null && existing > 0) {
            return;
        }
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
                "READY", sourcePayload, summary, differences, "[]");
    }

    private void insertReviewIssues(Long bankId, JsonNode seed) throws IOException {
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
                    """, importId(bankId), questionVersionId, issue.path("issueCode").asString(), issue.path("severity").asString(),
                    issue.hasNonNull("itemCode") ? issue.path("itemCode").asString() : PACKAGE_CODE,
                    issue.path("description").asString(), objectMapper.writeValueAsString(issue));
        }
    }

    private Long importId(Long bankId) {
        return jdbcTemplate.queryForObject("""
                SELECT id FROM assessment_question_bank_import
                WHERE question_bank_id = ? AND import_key = ? AND deleted = FALSE LIMIT 1
                """, Long.class, bankId, PACKAGE_CODE);
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

    private String nullableText(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() || value.asString().isBlank() ? null : value.asString();
    }

    private BigDecimal decimal(JsonNode node, String field, BigDecimal defaultValue) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? defaultValue : value.decimalValue();
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

    private record QuestionVersionRef(Long id, int versionNo, String contentHash) {
    }
}
