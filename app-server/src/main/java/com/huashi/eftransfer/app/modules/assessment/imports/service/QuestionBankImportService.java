package com.huashi.eftransfer.app.modules.assessment.imports.service;

import com.huashi.eftransfer.app.common.audit.service.AuditLogService;
import com.huashi.eftransfer.app.common.util.SecurityUtils;
import com.huashi.eftransfer.app.modules.assessment.imports.dto.ContentReviewResolutionRequest;
import com.huashi.eftransfer.app.modules.assessment.imports.dto.QuestionBankImportCommitRequest;
import com.huashi.eftransfer.app.modules.assessment.imports.dto.QuestionBankImportPackageRequest;
import com.huashi.eftransfer.app.modules.assessment.imports.vo.QuestionBankImportCommitVO;
import com.huashi.eftransfer.app.modules.assessment.imports.vo.QuestionBankImportIssueVO;
import com.huashi.eftransfer.app.modules.assessment.imports.vo.QuestionBankImportPreflightVO;
import com.huashi.eftransfer.app.modules.assessment.imports.vo.QuestionBankItemSummaryVO;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.exception.BusinessException;
import com.huashi.eftransfer.shared.page.PageResult;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class QuestionBankImportService {

    private static final String SHARED_BANK_CODE = "LEXIBRIDGE_SHARED";
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final QuestionBankImportPreflightValidator validator;
    private final AuditLogService auditLogService;

    public QuestionBankImportService(
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper,
            QuestionBankImportPreflightValidator validator,
            AuditLogService auditLogService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.auditLogService = auditLogService;
    }

    public PageResult<QuestionBankItemSummaryVO> listItems(int pageNo, int pageSize, String keyword, String tag, String reviewStatus) {
        int safePage = Math.max(pageNo, 1);
        int safeSize = Math.min(Math.max(pageSize, 1), 100);
        Long userId = currentUserId();
        StringBuilder where = new StringBuilder(" WHERE qv.deleted = FALSE AND qb.deleted = FALSE AND (qb.visibility = 'SHARED' OR qb.owner_user_id = ?)");
        List<Object> args = new ArrayList<>(List.of(userId));
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (qv.question_code LIKE ? OR qv.stem_text LIKE ? OR qv.target_word LIKE ?)");
            String value = "%" + keyword.trim() + "%";
            args.add(value); args.add(value); args.add(value);
        }
        if (tag != null && !tag.isBlank()) {
            where.append(" AND (qv.transfer_category = ? OR qv.context_level = ? OR qv.construct_code = ?)");
            args.add(tag.trim()); args.add(tag.trim()); args.add(tag.trim());
        }
        if (reviewStatus != null && !reviewStatus.isBlank()) {
            where.append(" AND (CASE WHEN EXISTS (SELECT 1 FROM assessment_content_review_issue ri WHERE ri.question_version_id = qv.id AND ri.status = 'REJECTED' AND ri.deleted = FALSE) THEN 'REJECTED' WHEN EXISTS (SELECT 1 FROM assessment_content_review_issue ri WHERE ri.question_version_id = qv.id AND ri.status = 'OPEN' AND ri.deleted = FALSE) THEN 'REVIEW_REQUIRED' ELSE 'APPROVED' END) = ?");
            args.add(reviewStatus.trim());
        }
        String from = " FROM assessment_question_version qv JOIN assessment_question_bank qb ON qb.id = qv.question_bank_id" + where;
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*)" + from, Long.class, args.toArray());
        int offset = (safePage - 1) * safeSize;
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(safeSize); pageArgs.add(offset);
        List<QuestionBankItemSummaryVO> records = jdbcTemplate.query("""
                SELECT qv.id,qv.question_code,qv.version_no,qv.question_type,qv.stem_text,
                       qv.transfer_category,qv.context_level,qv.construct_code,qv.target_word,
                       qv.updated_at,
                       CASE WHEN EXISTS (SELECT 1 FROM assessment_content_review_issue ri
                                         WHERE ri.question_version_id=qv.id AND ri.status='REJECTED' AND ri.deleted=FALSE)
                            THEN 'REJECTED'
                            WHEN EXISTS (SELECT 1 FROM assessment_content_review_issue ri
                                         WHERE ri.question_version_id=qv.id AND ri.status='OPEN' AND ri.deleted=FALSE)
                            THEN 'REVIEW_REQUIRED' ELSE 'APPROVED' END AS review_status
                """ + from + " ORDER BY qv.updated_at DESC, qv.question_code LIMIT ? OFFSET ?", (rs, row) -> {
            List<String> tags = new ArrayList<>();
            addTag(tags, rs.getString(6)); addTag(tags, rs.getString(7)); addTag(tags, rs.getString(8));
            return new QuestionBankItemSummaryVO(rs.getLong(1), rs.getString(2), rs.getInt(3), rs.getString(4),
                    rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8), rs.getString(9),
                    List.copyOf(tags), rs.getString(11), rs.getTimestamp(10) == null ? null : rs.getTimestamp(10).toLocalDateTime());
        }, pageArgs.toArray());
        return new PageResult<>(total == null ? 0 : total, safePage, safeSize, records);
    }

    public byte[] downloadTemplate(boolean json) {
        try {
            if (json) {
                Map<String, Object> template = new LinkedHashMap<>();
                template.put("Questionnaire", Map.of("code", "LEXIBRIDGE_RESEARCH_V1", "title", "", "description", "", "durationMinutes", 40, "scoringVersion", "SCORING_V1", "aiPromptVersion", "assessment-analysis/v1"));
                template.put("Sections", List.of(Map.of("sectionCode", "P1", "title", "", "description", "", "sharedMaterial", "", "sortOrder", 1, "formalSection", true)));
                Map<String, Object> itemTemplate = new LinkedHashMap<>();
                itemTemplate.put("itemCode", "P1-01"); itemTemplate.put("sectionCode", "P1"); itemTemplate.put("questionType", "SINGLE_CHOICE");
                itemTemplate.put("stemText", ""); itemTemplate.put("promptText", ""); itemTemplate.put("correctAnswers", List.of("A"));
                itemTemplate.put("explanationText", ""); itemTemplate.put("requiredAnswer", true); itemTemplate.put("scored", true);
                itemTemplate.put("weight", 1); itemTemplate.put("transferCategory", ""); itemTemplate.put("contextLevel", "WORD");
                itemTemplate.put("constructCode", "LEXICAL_TRANSFER"); itemTemplate.put("targetWord", ""); itemTemplate.put("displayConditionJson", "");
                template.put("Items", List.of(itemTemplate));
                template.put("Options", List.of(Map.of("itemCode", "P1-01", "optionCode", "A", "optionText", "", "correct", true, "explanation", "")));
                return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(template).getBytes(StandardCharsets.UTF_8);
            }
            try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                writeSheet(workbook, "Questionnaire", List.of("code", "title", "description", "durationMinutes", "scoringVersion", "aiPromptVersion"), List.of("LEXIBRIDGE_RESEARCH_V1", "", "", "40", "SCORING_V1", "assessment-analysis/v1"));
                writeSheet(workbook, "Sections", List.of("sectionCode", "title", "description", "sharedMaterial", "sortOrder", "formalSection"), List.of("P1", "", "", "", "1", "true"));
                writeSheet(workbook, "Items", List.of("itemCode", "sectionCode", "questionType", "stemText", "promptText", "correctAnswers", "explanationText", "requiredAnswer", "scored", "weight", "transferCategory", "contextLevel", "constructCode", "targetWord", "displayConditionJson"), List.of("P1-01", "P1", "SINGLE_CHOICE", "", "", "A", "", "true", "true", "1", "", "WORD", "LEXICAL_TRANSFER", "", ""));
                writeSheet(workbook, "Options", List.of("itemCode", "optionCode", "optionText", "correct", "explanation"), List.of("P1-01", "A", "", "true", ""));
                workbook.write(output);
                return output.toByteArray();
            }
        } catch (IOException exception) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "Unable to create import template", 500);
        }
    }

    @Transactional
    public QuestionBankImportPreflightVO preflight(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Import file is empty");
        }
        try {
            byte[] bytes = file.getBytes();
            QuestionBankImportPackageRequest request = parse(file.getOriginalFilename(), bytes);
            Long bankId = ensureSharedBank();
            Map<String, QuestionBankImportPreflightValidator.ExistingQuestion> existing = existingQuestions(bankId);
            QuestionBankImportPreflightValidator.Result result = validator.validate(request, existing);
            String payload = objectMapper.writeValueAsString(request);
            String issuesJson = objectMapper.writeValueAsString(result.issues());
            String summary = objectMapper.writeValueAsString(Map.of("itemCount", request.items().size(), "sectionCount", request.sections().size(), "scoredItemCount", result.scoredItemCount()));
            String importKey = "IMPORT_" + UUID.randomUUID().toString().replace("-", "");
            jdbcTemplate.update("""
                    INSERT INTO assessment_question_bank_import
                        (question_bank_id,import_key,source_file_name,source_format,source_sha256,status,source_payload_json,
                         preflight_summary_json,differences_json,errors_json,created_by,updated_by)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
                    """, bankId, importKey, file.getOriginalFilename(), extension(file.getOriginalFilename()), sha256(bytes), result.status(), payload,
                    summary, issuesJson, issuesJson, currentUserId(), currentUserId());
            Long importId = jdbcTemplate.queryForObject("SELECT id FROM assessment_question_bank_import WHERE import_key = ?", Long.class, importKey);
            for (QuestionBankImportPreflightValidator.Issue issue : result.issues()) {
                jdbcTemplate.update("""
                        INSERT INTO assessment_content_review_issue
                            (import_id,issue_code,severity,status,source_reference,description,source_value_json,created_by,updated_by)
                        VALUES (?,?,?,'OPEN',?,?,?, ?, ?)
                        """, importId, issue.code(), issue.severity(), issue.itemCode(), issue.message(), objectMapper.writeValueAsString(issue), currentUserId(), currentUserId());
            }
            auditLogService.record("ASSESSMENT_QUESTION_BANK_PREFLIGHT", "QUESTION_BANK_IMPORT", String.valueOf(importId), Map.of("fileName", file.getOriginalFilename(), "status", result.status()), "SUCCESS");
            return preflightView(importId, file.getOriginalFilename(), request.items().size() + request.sections().size() + request.options().size() + 1, result.issues());
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Unable to parse import file: " + exception.getMessage());
        }
    }

    @Transactional
    public QuestionBankImportCommitVO commit(Long importId, QuestionBankImportCommitRequest confirmation) {
        if (confirmation == null || !confirmation.confirmed()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Explicit import confirmation is required");
        }
        ImportRow imported = importRow(importId);
        if ("PREFLIGHT_FAILED".equals(imported.status())) {
            throw new BusinessException(ResultCode.CONFLICT, "Import has preflight errors");
        }
        try {
            QuestionBankImportPackageRequest request = objectMapper.readValue(imported.sourcePayloadJson(), QuestionBankImportPackageRequest.class);
            Long bankId = imported.questionBankId() == null ? ensureSharedBank() : imported.questionBankId();
            Map<String, List<QuestionBankImportPackageRequest.OptionRow>> optionsByItem = request.options().stream()
                    .collect(java.util.stream.Collectors.groupingBy(QuestionBankImportPackageRequest.OptionRow::itemCode, LinkedHashMap::new, java.util.stream.Collectors.toList()));
            Map<String, Long> questionVersions = new LinkedHashMap<>();
            for (QuestionBankImportPackageRequest.ItemRow item : request.items()) {
                questionVersions.put(item.itemCode(), upsertQuestionVersion(bankId, item, optionsByItem.getOrDefault(item.itemCode(), List.of())));
            }
            Long questionnaireId = questionnaireId(request.questionnaire().code());
            Long ownerId = currentUserId();
            if (questionnaireId == null) {
                jdbcTemplate.update("INSERT INTO assessment_questionnaire (questionnaire_code,title,description,owner_user_id,status,latest_version_no,created_by,updated_by) VALUES (?,?,?,?,'DRAFT',0,?,?)",
                        request.questionnaire().code(), request.questionnaire().title(), request.questionnaire().description(), ownerId, ownerId, ownerId);
                questionnaireId = questionnaireId(request.questionnaire().code());
            }
            int versionNo = nextQuestionnaireVersion(questionnaireId);
            String paperCode = (request.questionnaire().code() + "_V" + versionNo).substring(0, Math.min(64, request.questionnaire().code().length() + 3 + String.valueOf(versionNo).length()));
            jdbcTemplate.update("INSERT INTO assessment_paper (paper_code,title,description,owner_user_id,status,duration_minutes,question_count,total_score,created_by,updated_by) VALUES (?,?,?,?,?,?,?,?,?,?)",
                    paperCode, request.questionnaire().title(), request.questionnaire().description(), ownerId, "DRAFT", request.questionnaire().durationMinutes(), (int) request.items().stream().filter(QuestionBankImportPackageRequest.ItemRow::scored).count(), (int) request.items().stream().filter(QuestionBankImportPackageRequest.ItemRow::scored).count(), ownerId, ownerId);
            Long paperId = jdbcTemplate.queryForObject("SELECT id FROM assessment_paper WHERE paper_code = ?", Long.class, paperCode);
            String versionStatus = "REVIEW_REQUIRED".equals(imported.status()) ? "REVIEW_REQUIRED" : "APPROVED";
            jdbcTemplate.update("INSERT INTO assessment_questionnaire_version (questionnaire_id,paper_id,version_no,status,scoring_version,ai_prompt_version,source_package_code,created_by,updated_by) VALUES (?,?,?, ?,?,?,?, ?, ?)", questionnaireId, paperId, versionNo, versionStatus, request.questionnaire().scoringVersion(), request.questionnaire().aiPromptVersion(), imported.importKey(), ownerId, ownerId);
            Long questionnaireVersionId = jdbcTemplate.queryForObject("SELECT id FROM assessment_questionnaire_version WHERE questionnaire_id = ? AND version_no = ?", Long.class, questionnaireId, versionNo);
            Map<String, Long> sections = new LinkedHashMap<>();
            for (QuestionBankImportPackageRequest.SectionRow section : request.sections()) {
                int count = (int) request.items().stream().filter(item -> section.sectionCode().equals(item.sectionCode()) && item.scored()).count();
                jdbcTemplate.update("INSERT INTO assessment_questionnaire_section (questionnaire_version_id,section_code,title,description,shared_material,sort_order,formal_section,scored_item_count,created_by,updated_by) VALUES (?,?,?,?,?,?,?,?,?,?)",
                        questionnaireVersionId, section.sectionCode(), section.title(), section.description(), section.sharedMaterial(), section.sortOrder(), section.formalSection(), count, ownerId, ownerId);
                sections.put(section.sectionCode(), jdbcTemplate.queryForObject("SELECT id FROM assessment_questionnaire_section WHERE questionnaire_version_id = ? AND section_code = ?", Long.class, questionnaireVersionId, section.sectionCode()));
            }
            int sortOrder = 1;
            for (QuestionBankImportPackageRequest.ItemRow item : request.items()) {
                Long questionVersionId = questionVersions.get(item.itemCode());
                jdbcTemplate.update("""
                        INSERT INTO assessment_question (paper_id,question_type,sort_order,stem_text,prompt_text,options_json,correct_answer_json,explanation_text,score,question_version_id,section_code,required_answer,weight,transfer_category,context_level,construct_code,target_word,created_by,updated_by)
                        SELECT ?,question_type,?,stem_text,prompt_text,options_json,correct_answer_json,LEFT(explanation_text,1000),CASE WHEN ? THEN 1 ELSE 0 END,id,?,?,?,?,?,?,?,?,?
                        FROM assessment_question_version WHERE id = ?
                        """, paperId, sortOrder++, item.scored(), item.sectionCode(), item.requiredAnswer(), item.weight(), item.transferCategory(), item.contextLevel(), item.constructCode(), item.targetWord(), ownerId, ownerId, questionVersionId);
                Long assessmentQuestionId = jdbcTemplate.queryForObject("SELECT id FROM assessment_question WHERE paper_id = ? AND sort_order = ?", Long.class, paperId, sortOrder - 1);
                jdbcTemplate.update("INSERT INTO assessment_questionnaire_item (questionnaire_version_id,section_id,assessment_question_id,question_version_id,item_code,required_answer,scored,weight,transfer_category,context_level,construct_code,target_word,created_by,updated_by) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                        questionnaireVersionId, sections.get(item.sectionCode()), assessmentQuestionId, questionVersionId, item.itemCode(), item.requiredAnswer(), item.scored(), item.weight(), item.transferCategory(), item.contextLevel(), item.constructCode(), item.targetWord(), ownerId, ownerId);
                jdbcTemplate.update("UPDATE assessment_content_review_issue SET question_version_id = ? WHERE import_id = ? AND source_reference = ? AND deleted = FALSE", questionVersionId, importId, item.itemCode());
            }
            jdbcTemplate.update("UPDATE assessment_questionnaire SET latest_version_no = ?, status = ? WHERE id = ?", versionNo, versionStatus, questionnaireId);
            jdbcTemplate.update("UPDATE assessment_question_bank_import SET status='COMMITTED', committed_by=?, committed_at=CURRENT_TIMESTAMP, updated_by=? WHERE id=?", ownerId, ownerId, importId);
            auditLogService.record("ASSESSMENT_QUESTION_BANK_COMMIT", "QUESTION_BANK_IMPORT", String.valueOf(importId), Map.of("questionnaireId", questionnaireId, "paperId", paperId), "SUCCESS");
            return new QuestionBankImportCommitVO(importId, "COMMITTED", bankId, questionnaireId, questionnaireVersionId, paperId, request.items().size(), Math.toIntExact(countOpenReviewIssues(importId)));
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(ResultCode.CONFLICT, "Import commit failed: " + exception.getMessage());
        }
    }

    @Transactional
    public void resolveIssue(Long importId, Long issueId, ContentReviewResolutionRequest request) {
        Long userId = currentUserId();
        String decision = request.decision().trim().toUpperCase(Locale.ROOT);
        if (!decision.equals("APPROVED") && !decision.equals("REJECTED")) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Decision must be APPROVED or REJECTED");
        }
        int updated = jdbcTemplate.update("UPDATE assessment_content_review_issue SET status=?,resolved_by=?,resolved_at=CURRENT_TIMESTAMP,resolution_note=?,updated_by=? WHERE id=? AND import_id=? AND deleted=FALSE AND status='OPEN'", decision.equals("APPROVED") ? "RESOLVED" : "REJECTED", userId, request.resolutionNote().trim(), userId, issueId, importId);
        if (updated == 0) throw new BusinessException(ResultCode.NOT_FOUND, "Review issue not found or already resolved");
        auditLogService.record("ASSESSMENT_QUESTION_BANK_REVIEW", "CONTENT_REVIEW_ISSUE", String.valueOf(issueId), request, "SUCCESS");
    }

    @Transactional
    public QuestionBankImportCommitVO approveImport(Long importId) {
        ImportRow imported = importRow(importId);
        Long open = countOpenReviewIssues(importId);
        Long rejected = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM assessment_content_review_issue WHERE import_id=? AND status='REJECTED' AND deleted=FALSE", Long.class, importId);
        if (open > 0 || rejected > 0 || !("COMMITTED".equals(imported.status()) || "REVIEW_REQUIRED".equals(imported.status()))) {
            throw new BusinessException(ResultCode.CONFLICT, "Resolve all review issues before approval");
        }
        Long versionId = jdbcTemplate.queryForObject("SELECT qv.id FROM assessment_questionnaire_version qv JOIN assessment_questionnaire q ON q.id=qv.questionnaire_id WHERE qv.source_package_code=? ORDER BY qv.version_no DESC LIMIT 1", Long.class, imported.importKey());
        if (versionId == null) throw new BusinessException(ResultCode.NOT_FOUND, "Committed questionnaire version not found");
        jdbcTemplate.update("UPDATE assessment_questionnaire_version SET status='APPROVED',updated_by=? WHERE id=?", currentUserId(), versionId);
        jdbcTemplate.update("UPDATE assessment_questionnaire q JOIN assessment_questionnaire_version qv ON qv.questionnaire_id=q.id SET q.status='APPROVED',q.updated_by=? WHERE qv.id=?", currentUserId(), versionId);
        Long questionnaireId = jdbcTemplate.queryForObject("SELECT questionnaire_id FROM assessment_questionnaire_version WHERE id=?", Long.class, versionId);
        Long paperId = jdbcTemplate.queryForObject("SELECT paper_id FROM assessment_questionnaire_version WHERE id=?", Long.class, versionId);
        return new QuestionBankImportCommitVO(importId, imported.status(), imported.questionBankId(), questionnaireId, versionId, paperId, 0, 0);
    }

    private QuestionBankImportPreflightVO preflightView(Long importId, String fileName, int rowCount, List<QuestionBankImportPreflightValidator.Issue> issues) {
        long errors = issues.stream().filter(issue -> "ERROR".equals(issue.severity())).count();
        long warnings = issues.stream().filter(issue -> "WARNING".equals(issue.severity())).count();
        long reviews = issues.stream().filter(issue -> "REVIEW_REQUIRED".equals(issue.severity())).count();
        String status = errors > 0 ? "PREFLIGHT_FAILED" : reviews > 0 ? "REVIEW_REQUIRED" : "READY";
        return new QuestionBankImportPreflightVO(importId, status, fileName, rowCount, errors, warnings, reviews,
                issues.stream().map(issue -> new QuestionBankImportIssueVO("Items", null, null, issue.severity(), issue.code(), issue.itemCode(), issue.message())).toList());
    }

    private QuestionBankImportPackageRequest parse(String fileName, byte[] bytes) throws IOException {
        if (fileName != null && fileName.toLowerCase(Locale.ROOT).endsWith(".json")) return objectMapper.readValue(new String(bytes, StandardCharsets.UTF_8), QuestionBankImportPackageRequest.class);
        try (InputStream input = new java.io.ByteArrayInputStream(bytes); Workbook workbook = new XSSFWorkbook(input)) {
            DataFormatter formatter = new DataFormatter();
            Sheet questionnaireSheet = workbook.getSheet("Questionnaire");
            Sheet sectionsSheet = workbook.getSheet("Sections");
            Sheet itemsSheet = workbook.getSheet("Items");
            Sheet optionsSheet = workbook.getSheet("Options");
            if (questionnaireSheet == null || sectionsSheet == null || itemsSheet == null || optionsSheet == null) throw new IOException("Workbook must contain Questionnaire, Sections, Items and Options sheets");
            Row q = questionnaireSheet.getRow(1);
            var questionnaire = new QuestionBankImportPackageRequest.QuestionnaireRow(cell(formatter, q, 0), cell(formatter, q, 1), cell(formatter, q, 2), integer(formatter, q, 3), cell(formatter, q, 4), cell(formatter, q, 5));
            List<QuestionBankImportPackageRequest.SectionRow> sections = new ArrayList<>();
            for (int i=1; i<=sectionsSheet.getLastRowNum(); i++) { Row row=sectionsSheet.getRow(i); if(row==null||cell(formatter,row,0).isBlank()) continue; sections.add(new QuestionBankImportPackageRequest.SectionRow(cell(formatter,row,0),cell(formatter,row,1),cell(formatter,row,2),cell(formatter,row,3),integer(formatter,row,4),bool(formatter,row,5))); }
            List<QuestionBankImportPackageRequest.ItemRow> items = new ArrayList<>();
            for (int i=1; i<=itemsSheet.getLastRowNum(); i++) { Row row=itemsSheet.getRow(i); if(row==null||cell(formatter,row,0).isBlank()) continue; items.add(new QuestionBankImportPackageRequest.ItemRow(cell(formatter,row,0),cell(formatter,row,1),cell(formatter,row,2),cell(formatter,row,3),cell(formatter,row,4),split(cell(formatter,row,5)),cell(formatter,row,6),bool(formatter,row,7),bool(formatter,row,8),decimal(formatter,row,9),cell(formatter,row,10),cell(formatter,row,11),cell(formatter,row,12),cell(formatter,row,13),cell(formatter,row,14))); }
            List<QuestionBankImportPackageRequest.OptionRow> options = new ArrayList<>();
            for (int i=1; i<=optionsSheet.getLastRowNum(); i++) { Row row=optionsSheet.getRow(i); if(row==null||cell(formatter,row,0).isBlank()) continue; options.add(new QuestionBankImportPackageRequest.OptionRow(cell(formatter,row,0),cell(formatter,row,1),cell(formatter,row,2),bool(formatter,row,3),cell(formatter,row,4))); }
            return new QuestionBankImportPackageRequest(questionnaire, sections, items, options);
        }
    }

    private Long upsertQuestionVersion(Long bankId, QuestionBankImportPackageRequest.ItemRow item, List<QuestionBankImportPackageRequest.OptionRow> options) throws IOException {
        List<Map<String, String>> optionPayload = options.stream().map(option -> Map.of("key", option.optionCode(), "label", option.optionText())).toList();
        Map<String, String> optionExplanations = options.stream().filter(option -> option.explanation() != null && !option.explanation().isBlank())
                .collect(java.util.stream.Collectors.toMap(QuestionBankImportPackageRequest.OptionRow::optionCode, QuestionBankImportPackageRequest.OptionRow::explanation, (left, right) -> right, LinkedHashMap::new));
        String optionsJson = objectMapper.writeValueAsString(optionPayload);
        String correctJson = objectMapper.writeValueAsString(item.correctAnswers());
        Map<String, Object> canonical = new LinkedHashMap<>(); canonical.put("item", item); canonical.put("options", optionPayload); canonical.put("correct", item.correctAnswers());
        String hash = sha256(objectMapper.writeValueAsString(canonical).getBytes(StandardCharsets.UTF_8));
        List<Long> same = jdbcTemplate.query("SELECT id FROM assessment_question_version WHERE question_bank_id=? AND content_hash=? AND deleted=FALSE", (rs,row)->rs.getLong(1), bankId, hash);
        if (!same.isEmpty()) return same.getFirst();
        Integer version = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(version_no),0)+1 FROM assessment_question_version WHERE question_bank_id=? AND question_code=? AND deleted=FALSE", Integer.class, bankId, item.itemCode());
        jdbcTemplate.update("""
                INSERT INTO assessment_question_version
                    (question_bank_id,question_code,version_no,question_type,stem_text,prompt_text,options_json,correct_answer_json,explanation_text,option_explanations_json,required_answer,weight,transfer_category,context_level,construct_code,target_word,display_condition_json,source_reference,content_hash,created_by,updated_by)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """, bankId,item.itemCode(),version,item.questionType(),item.stemText(),item.promptText(),optionsJson,correctJson,item.explanationText(),objectMapper.writeValueAsString(optionExplanations),item.requiredAnswer(),item.weight(),item.transferCategory(),item.contextLevel(),item.constructCode(),item.targetWord(),item.displayConditionJson(),"question-bank-import:"+item.itemCode(),hash,currentUserId(),currentUserId());
        return jdbcTemplate.queryForObject("SELECT id FROM assessment_question_version WHERE question_bank_id=? AND question_code=? AND version_no=?", Long.class, bankId,item.itemCode(),version);
    }

    private Map<String, QuestionBankImportPreflightValidator.ExistingQuestion> existingQuestions(Long bankId) {
        return jdbcTemplate.query("SELECT q.question_code,q.stem_text,q.explanation_text FROM assessment_question_version q JOIN (SELECT question_code,MAX(version_no) version_no FROM assessment_question_version WHERE question_bank_id=? AND deleted=FALSE GROUP BY question_code) latest ON latest.question_code=q.question_code AND latest.version_no=q.version_no WHERE q.question_bank_id=? AND q.deleted=FALSE", (rs,row)->Map.entry(rs.getString(1), new QuestionBankImportPreflightValidator.ExistingQuestion(rs.getString(2),rs.getString(3))), bankId, bankId).stream().collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Long ensureSharedBank() {
        List<Long> ids = jdbcTemplate.query("SELECT id FROM assessment_question_bank WHERE bank_code=? AND deleted=FALSE", (rs,row)->rs.getLong(1), SHARED_BANK_CODE);
        if (!ids.isEmpty()) return ids.getFirst();
        Long owner = currentUserId();
        jdbcTemplate.update("INSERT INTO assessment_question_bank (bank_code,name,description,owner_user_id,visibility,status,created_by,updated_by) VALUES (?,?,?,?,'SHARED','ACTIVE',?,?)", SHARED_BANK_CODE,"Lexi-Bridge 共享研究题库","共享研究问卷题库",owner,owner,owner);
        return jdbcTemplate.queryForObject("SELECT id FROM assessment_question_bank WHERE bank_code=?", Long.class, SHARED_BANK_CODE);
    }

    private ImportRow importRow(Long importId) { return jdbcTemplate.query("SELECT id,question_bank_id,import_key,status,source_payload_json FROM assessment_question_bank_import WHERE id=? AND deleted=FALSE", (rs,row)->new ImportRow(rs.getLong(1),rs.getLong(2),rs.getString(3),rs.getString(4),rs.getString(5)), importId).stream().findFirst().orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND,"Import task not found")); }
    private Long questionnaireId(String code) { return jdbcTemplate.query("SELECT id FROM assessment_questionnaire WHERE questionnaire_code=? AND deleted=FALSE", (rs,row)->rs.getLong(1), code).stream().findFirst().orElse(null); }
    private int nextQuestionnaireVersion(Long questionnaireId) { Integer value=jdbcTemplate.queryForObject("SELECT COALESCE(MAX(version_no),0)+1 FROM assessment_questionnaire_version WHERE questionnaire_id=? AND deleted=FALSE",Integer.class,questionnaireId); return value == null ? 1 : value; }
    private long countOpenReviewIssues(Long importId) { Long value=jdbcTemplate.queryForObject("SELECT COUNT(*) FROM assessment_content_review_issue WHERE import_id=? AND status='OPEN' AND deleted=FALSE",Long.class,importId); return value == null ? 0 : value; }
    private Long currentUserId() { return SecurityUtils.getCurrentUserId().orElseThrow(() -> new BusinessException(ResultCode.UNAUTHORIZED,"Authentication required",401)); }
    private static void addTag(List<String> tags,String value){if(value!=null&&!value.isBlank())tags.add(value);}
    private static String extension(String file){ if(file==null)return "JSON"; int dot=file.lastIndexOf('.'); return dot<0?"JSON":file.substring(dot+1).toUpperCase(Locale.ROOT); }
    private static String sha256(byte[] bytes){try{byte[] digest=MessageDigest.getInstance("SHA-256").digest(bytes);StringBuilder s=new StringBuilder();for(byte b:digest)s.append(String.format("%02x",b));return s.toString();}catch(Exception e){throw new IllegalStateException(e);}}
    private static String cell(DataFormatter formatter,Row row,int index){return row==null||row.getCell(index)==null?"":formatter.formatCellValue(row.getCell(index)).trim();}
    private static Integer integer(DataFormatter f,Row r,int i){String v=cell(f,r,i);return v.isBlank()?null:Integer.valueOf(v.replace(",",""));}
    private static BigDecimal decimal(DataFormatter f,Row r,int i){String v=cell(f,r,i);return v.isBlank()?BigDecimal.ONE:new BigDecimal(v);}
    private static boolean bool(DataFormatter f,Row r,int i){return Boolean.parseBoolean(cell(f,r,i))||"1".equals(cell(f,r,i))||"是".equals(cell(f,r,i));}
    private static List<String> split(String value){if(value==null||value.isBlank())return List.of();return java.util.Arrays.stream(value.split("[,|;]")).map(String::trim).filter(v->!v.isBlank()).toList();}
    private static void writeSheet(Workbook workbook,String name,List<String> headers,List<String> values){Sheet sheet=workbook.createSheet(name);Row head=sheet.createRow(0);for(int i=0;i<headers.size();i++)head.createCell(i).setCellValue(headers.get(i));Row row=sheet.createRow(1);for(int i=0;i<values.size();i++)row.createCell(i).setCellValue(values.get(i));for(int i=0;i<headers.size();i++)sheet.autoSizeColumn(i);}
    private record ImportRow(Long id,Long questionBankId,String importKey,String status,String sourcePayloadJson){}
}
