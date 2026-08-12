package com.huashi.eftransfer.app.modules.lexicon.imports.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huashi.eftransfer.app.common.security.JwtPrincipal;
import com.huashi.eftransfer.app.common.util.SecurityUtils;
import com.huashi.eftransfer.app.modules.lexicon.dto.LexicalPairUpsertRequest;
import com.huashi.eftransfer.app.modules.lexicon.imports.dto.LexicalImportBatchPageQuery;
import com.huashi.eftransfer.app.modules.lexicon.imports.dto.LexicalImportRowPageQuery;
import com.huashi.eftransfer.app.modules.lexicon.imports.dto.LexicalImportRowUpdateRequest;
import com.huashi.eftransfer.app.modules.lexicon.imports.entity.LexicalImportBatchEntity;
import com.huashi.eftransfer.app.modules.lexicon.imports.entity.LexicalImportFileEntity;
import com.huashi.eftransfer.app.modules.lexicon.imports.entity.LexicalImportRowEntity;
import com.huashi.eftransfer.app.modules.lexicon.imports.mapper.LexicalImportBatchMapper;
import com.huashi.eftransfer.app.modules.lexicon.imports.mapper.LexicalImportFileMapper;
import com.huashi.eftransfer.app.modules.lexicon.imports.mapper.LexicalImportRowMapper;
import com.huashi.eftransfer.app.modules.lexicon.imports.support.LexicalImportBatchStatus;
import com.huashi.eftransfer.app.modules.lexicon.imports.support.LexicalImportCounts;
import com.huashi.eftransfer.app.modules.lexicon.imports.support.LexicalImportParsedRow;
import com.huashi.eftransfer.app.modules.lexicon.imports.support.LexicalImportRowDraft;
import com.huashi.eftransfer.app.modules.lexicon.imports.support.LexicalImportRowStatus;
import com.huashi.eftransfer.app.modules.lexicon.imports.support.LexicalImportSourceFormat;
import com.huashi.eftransfer.app.modules.lexicon.imports.support.LexicalImportTemplateSupport;
import com.huashi.eftransfer.app.modules.lexicon.imports.vo.LexicalImportBatchCreatedVO;
import com.huashi.eftransfer.app.modules.lexicon.imports.vo.LexicalImportBatchDetailVO;
import com.huashi.eftransfer.app.modules.lexicon.imports.vo.LexicalImportBatchSummaryVO;
import com.huashi.eftransfer.app.modules.lexicon.imports.vo.LexicalImportRowVO;
import com.huashi.eftransfer.app.modules.lexicon.entity.LexicalPairEntity;
import com.huashi.eftransfer.app.modules.lexicon.mapper.LexicalPairMapper;
import com.huashi.eftransfer.app.modules.lexicon.service.LexicalPairService;
import com.huashi.eftransfer.app.modules.user.entity.UserEntity;
import com.huashi.eftransfer.app.modules.user.mapper.UserMapper;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.enums.EmbeddingStatus;
import com.huashi.eftransfer.shared.exception.BusinessException;
import com.huashi.eftransfer.shared.page.PageQuery;
import com.huashi.eftransfer.shared.page.PageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class LexicalImportBatchService {

    private static final Logger log = LoggerFactory.getLogger(LexicalImportBatchService.class);
    private static final long MAX_FILE_SIZE_BYTES = 50L * 1024L * 1024L;
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final LexicalImportBatchMapper batchMapper;
    private final LexicalImportFileMapper fileMapper;
    private final LexicalImportRowMapper rowMapper;
    private final LexicalPairMapper lexicalPairMapper;
    private final UserMapper userMapper;
    private final LexicalImportFileParser fileParser;
    private final LexicalImportTemplateSupport templateSupport;
    private final LexicalPairService lexicalPairService;
    private final TaskExecutor taskExecutor;
    private final ObjectMapper objectMapper;

    public LexicalImportBatchService(
            LexicalImportBatchMapper batchMapper,
            LexicalImportFileMapper fileMapper,
            LexicalImportRowMapper rowMapper,
            LexicalPairMapper lexicalPairMapper,
            UserMapper userMapper,
            LexicalImportFileParser fileParser,
            LexicalImportTemplateSupport templateSupport,
            LexicalPairService lexicalPairService,
            @Qualifier("lexicalImportTaskExecutor") TaskExecutor taskExecutor,
            ObjectMapper objectMapper
    ) {
        this.batchMapper = batchMapper;
        this.fileMapper = fileMapper;
        this.rowMapper = rowMapper;
        this.lexicalPairMapper = lexicalPairMapper;
        this.userMapper = userMapper;
        this.fileParser = fileParser;
        this.templateSupport = templateSupport;
        this.lexicalPairService = lexicalPairService;
        this.taskExecutor = taskExecutor;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public LexicalImportBatchCreatedVO createBatch(MultipartFile file) {
        JwtPrincipal principal = requirePrincipal();
        byte[] content = readAndValidateFile(file);
        LexicalImportSourceFormat format = resolveSourceFormat(file.getOriginalFilename());

        LexicalImportBatchEntity batch = new LexicalImportBatchEntity();
        batch.setOwnerUserId(principal.userId());
        batch.setOriginalFilename(resolveFilename(file.getOriginalFilename()));
        batch.setContentType(trimToNull(file.getContentType()));
        batch.setFileSizeBytes(file.getSize());
        batch.setSourceFormat(format.name());
        batch.setStatus(LexicalImportBatchStatus.PARSING.name());
        batch.setTotalRows(0);
        batch.setReadyRows(0);
        batch.setInvalidRows(0);
        batch.setSkippedRows(0);
        batch.setImportedRows(0);
        batch.setErrorMessage(null);
        batchMapper.insert(batch);

        LexicalImportFileEntity storedFile = new LexicalImportFileEntity();
        storedFile.setBatchId(batch.getId());
        storedFile.setOriginalFilename(batch.getOriginalFilename());
        storedFile.setContentType(batch.getContentType());
        storedFile.setFileSizeBytes(batch.getFileSizeBytes());
        storedFile.setSha256(sha256(content));
        storedFile.setFileContent(content);
        fileMapper.insert(storedFile);

        dispatchAfterCommit(() -> taskExecutor.execute(() -> runWithPrincipal(principal, () -> parseBatch(batch.getId(), format))));
        return new LexicalImportBatchCreatedVO(batch.getId(), batch.getStatus());
    }

    public PageResult<LexicalImportBatchSummaryVO> pageBatches(LexicalImportBatchPageQuery query) {
        PageQuery pageQuery = query.toPageQuery();
        Long ownerFilter = resolveOwnerFilter(query.ownerUserId());
        var countWrapper = buildBatchPageWrapper(query, ownerFilter, false);
        var recordWrapper = buildBatchPageWrapper(query, ownerFilter, true);

        long total = batchMapper.selectCount(countWrapper);
        List<LexicalImportBatchEntity> batches = batchMapper.selectList(recordWrapper
                .last("LIMIT " + pageQuery.pageSize() + " OFFSET " + pageQuery.offset()));
        Map<Long, String> ownerNameMap = loadOwnerDisplayNames(batches.stream()
                .map(LexicalImportBatchEntity::getOwnerUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList());

        List<LexicalImportBatchSummaryVO> records = batches.stream()
                .map(batch -> toSummaryVO(batch, ownerNameMap.get(batch.getOwnerUserId())))
                .toList();
        return new PageResult<>(total, pageQuery.pageNo(), pageQuery.pageSize(), records);
    }

    static String normalizeView(String view) {
        if (view == null || view.isBlank()) {
            return "ALL";
        }
        return switch (view.trim().toUpperCase(Locale.ROOT)) {
            case "PENDING", "FAILED" -> view.trim().toUpperCase(Locale.ROOT);
            default -> "ALL";
        };
    }

    static String normalizeStatusFilter(String status) {
        return status == null || status.isBlank() ? null : parseStaticBatchStatus(status);
    }

    static String parseStaticBatchStatus(String value) {
        try {
            return LexicalImportBatchStatus.valueOf(value.trim().toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "Unsupported import batch status: " + value, 400);
        }
    }

    static boolean shouldApplyPendingView(String status, String view) {
        return normalizeStatusFilter(status) == null && "PENDING".equals(normalizeView(view));
    }

    static boolean shouldApplyFailedView(String status, String view) {
        return normalizeStatusFilter(status) == null && "FAILED".equals(normalizeView(view));
    }

    private void applyStatusOrViewFilter(
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<LexicalImportBatchEntity> wrapper,
            String status,
            String view
    ) {
        String normalizedStatus = normalizeStatusFilter(status);
        if (normalizedStatus != null) {
            wrapper.eq(LexicalImportBatchEntity::getStatus, normalizedStatus);
            return;
        }
        if (shouldApplyPendingView(null, view)) {
            wrapper.in(
                    LexicalImportBatchEntity::getStatus,
                    List.of(
                            LexicalImportBatchStatus.PARSING.name(),
                            LexicalImportBatchStatus.DRAFT.name(),
                            LexicalImportBatchStatus.IMPORTING.name()
                    )
            );
            return;
        }
        if (shouldApplyFailedView(null, view)) {
            wrapper.eq(LexicalImportBatchEntity::getStatus, LexicalImportBatchStatus.FAILED.name());
        }
    }

    private com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<LexicalImportBatchEntity> buildBatchPageWrapper(
            LexicalImportBatchPageQuery query,
            Long ownerFilter,
            boolean includeOrder
    ) {
        var wrapper = Wrappers.<LexicalImportBatchEntity>lambdaQuery();
        if (ownerFilter != null) {
            wrapper.eq(LexicalImportBatchEntity::getOwnerUserId, ownerFilter);
        }
        applyStatusOrViewFilter(wrapper, query.status(), query.view());
        if (query.keyword() != null && !query.keyword().isBlank()) {
            String keyword = "%" + query.keyword().trim().toLowerCase(Locale.ROOT) + "%";
            wrapper.apply("LOWER(original_filename) LIKE {0}", keyword);
        }
        if (includeOrder) {
            wrapper.orderByDesc(LexicalImportBatchEntity::getCreatedAt)
                    .orderByDesc(LexicalImportBatchEntity::getId);
        }
        return wrapper;
    }

    private com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<LexicalImportRowEntity> buildRowPageWrapper(
            Long batchId,
            LexicalImportRowPageQuery query,
            boolean includeOrder
    ) {
        var wrapper = Wrappers.<LexicalImportRowEntity>lambdaQuery()
                .eq(LexicalImportRowEntity::getBatchId, batchId);
        if (query.status() != null && !query.status().isBlank()) {
            wrapper.eq(LexicalImportRowEntity::getRowStatus, parseRowStatus(query.status()).name());
        }
        if (includeOrder) {
            wrapper.orderByAsc(LexicalImportRowEntity::getRowNumber)
                    .orderByAsc(LexicalImportRowEntity::getId);
        }
        return wrapper;
    }

    public LexicalImportBatchDetailVO getBatchDetail(Long batchId) {
        LexicalImportBatchEntity batch = requireAccessibleBatch(batchId);
        LexicalImportFileEntity file = requireFile(batch.getId());
        String ownerDisplayName = loadOwnerDisplayNames(List.of(batch.getOwnerUserId())).get(batch.getOwnerUserId());
        BatchEmbeddingSummary embeddingSummary = loadBatchEmbeddingSummary(batchId);
        return toDetailVO(batch, file, ownerDisplayName, embeddingSummary);
    }

    public List<String> listImportedLexicalPairIds(Long batchId) {
        requireAccessibleBatch(batchId);
        return loadImportedLexicalPairIds(batchId).stream()
                .map(String::valueOf)
                .toList();
    }

    public PageResult<LexicalImportRowVO> pageRows(Long batchId, LexicalImportRowPageQuery query) {
        requireAccessibleBatch(batchId);
        PageQuery pageQuery = query.toPageQuery();
        var countWrapper = buildRowPageWrapper(batchId, query, false);
        var recordWrapper = buildRowPageWrapper(batchId, query, true);

        long total = rowMapper.selectCount(countWrapper);
        List<LexicalImportRowEntity> rows = rowMapper.selectList(recordWrapper
                .last("LIMIT " + pageQuery.pageSize() + " OFFSET " + pageQuery.offset()));
        List<LexicalImportRowVO> records = rows.stream()
                .map(this::toRowVO)
                .toList();
        return new PageResult<>(total, pageQuery.pageNo(), pageQuery.pageSize(), records);
    }

    @Transactional
    public LexicalImportRowVO updateRow(Long batchId, Long rowId, LexicalImportRowUpdateRequest request) {
        LexicalImportBatchEntity batch = requireAccessibleBatch(batchId);
        if (batch.getStatus().equals(LexicalImportBatchStatus.PARSING.name())
                || batch.getStatus().equals(LexicalImportBatchStatus.IMPORTING.name())) {
            throw new BusinessException(ResultCode.CONFLICT, "Import batch is not editable right now", 409);
        }

        LexicalImportRowEntity row = requireRow(batchId, rowId);
        if (LexicalImportRowStatus.IMPORTED.name().equals(row.getRowStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "Imported rows can no longer be edited", 409);
        }

        LexicalImportRowDraft draft = templateSupport.toDraft(request);
        boolean skipped = Boolean.TRUE.equals(request.skipped());
        List<String> validationErrors = skipped ? List.of() : validateDraft(batchId, rowId, draft);

        row.setDraftJson(writeJson(draft));
        row.setValidationErrorsJson(validationErrors.isEmpty() ? null : writeJson(validationErrors));
        row.setImportMessage(null);
        row.setImportedLexicalPairId(null);
        row.setRowStatus(skipped
                ? LexicalImportRowStatus.SKIPPED.name()
                : validationErrors.isEmpty() ? LexicalImportRowStatus.READY.name() : LexicalImportRowStatus.INVALID.name());
        rowMapper.updateById(row);

        refreshBatchCounts(batchId, LexicalImportBatchStatus.DRAFT, null);
        return toRowVO(requireRow(batchId, rowId));
    }

    @Transactional
    public LexicalImportBatchCreatedVO commitBatch(Long batchId) {
        JwtPrincipal principal = requirePrincipal();
        LexicalImportBatchEntity batch = requireAccessibleBatch(batchId);
        if (LexicalImportBatchStatus.PARSING.name().equals(batch.getStatus())
                || LexicalImportBatchStatus.IMPORTING.name().equals(batch.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "Import batch is already processing", 409);
        }

        LexicalImportCounts counts = loadCounts(batchId);
        if (counts.readyRows() <= 0) {
            throw new BusinessException(ResultCode.CONFLICT, "No ready import rows to commit", 409);
        }

        batch.setStatus(LexicalImportBatchStatus.IMPORTING.name());
        batch.setErrorMessage(null);
        batch.setImportJobStartedAt(LocalDateTime.now());
        batch.setImportJobFinishedAt(null);
        batchMapper.updateById(batch);

        dispatchAfterCommit(() -> taskExecutor.execute(() -> runWithPrincipal(principal, () -> importBatch(batchId))));
        return new LexicalImportBatchCreatedVO(batchId, LexicalImportBatchStatus.IMPORTING.name());
    }

    public LexicalImportFileEntity loadFile(Long batchId) {
        requireAccessibleBatch(batchId);
        return requireFile(batchId);
    }

    private void parseBatch(Long batchId, LexicalImportSourceFormat format) {
        LexicalImportBatchEntity batch = batchMapper.selectById(batchId);
        if (batch == null) {
            return;
        }
        try {
            batch.setStatus(LexicalImportBatchStatus.PARSING.name());
            batch.setErrorMessage(null);
            batch.setParserJobStartedAt(LocalDateTime.now());
            batch.setParserJobFinishedAt(null);
            batchMapper.updateById(batch);

            rowMapper.delete(Wrappers.<LexicalImportRowEntity>lambdaQuery()
                    .eq(LexicalImportRowEntity::getBatchId, batchId));

            List<LexicalImportParsedRow> parsedRows = fileParser.parse(requireFile(batchId).getFileContent(), format);
            Set<String> seenPairKeys = new LinkedHashSet<>();
            for (LexicalImportParsedRow parsedRow : parsedRows) {
                List<String> validationErrors = validateDraft(batchId, null, parsedRow.draft(), seenPairKeys);
                LexicalImportRowEntity row = new LexicalImportRowEntity();
                row.setBatchId(batchId);
                row.setRowNumber(parsedRow.rowNumber());
                row.setRowStatus(validationErrors.isEmpty()
                        ? LexicalImportRowStatus.READY.name()
                        : LexicalImportRowStatus.INVALID.name());
                row.setDraftJson(writeJson(parsedRow.draft()));
                row.setValidationErrorsJson(validationErrors.isEmpty() ? null : writeJson(validationErrors));
                row.setImportMessage(null);
                rowMapper.insert(row);
            }

            LexicalImportBatchEntity refreshed = refreshBatchCounts(batchId, LexicalImportBatchStatus.DRAFT, null);
            refreshed.setParserJobFinishedAt(LocalDateTime.now());
            batchMapper.updateById(refreshed);
            log.info("event=lexical_import_batch_parsed batchId={} totalRows={} readyRows={} invalidRows={}",
                    batchId, refreshed.getTotalRows(), refreshed.getReadyRows(), refreshed.getInvalidRows());
        } catch (Exception exception) {
            LexicalImportBatchEntity failed = batchMapper.selectById(batchId);
            if (failed != null) {
                failed.setStatus(LexicalImportBatchStatus.FAILED.name());
                failed.setErrorMessage(resolveFailureMessage(exception));
                failed.setParserJobFinishedAt(LocalDateTime.now());
                batchMapper.updateById(failed);
            }
            log.warn("event=lexical_import_batch_parse_failed batchId={} message={}", batchId, resolveFailureMessage(exception));
        }
    }

    private void importBatch(Long batchId) {
        List<Long> importedLexicalPairIds = new ArrayList<>();
        try {
            List<LexicalImportRowEntity> readyRows = rowMapper.selectList(Wrappers.<LexicalImportRowEntity>lambdaQuery()
                    .eq(LexicalImportRowEntity::getBatchId, batchId)
                    .eq(LexicalImportRowEntity::getRowStatus, LexicalImportRowStatus.READY.name())
                    .orderByAsc(LexicalImportRowEntity::getRowNumber)
                    .orderByAsc(LexicalImportRowEntity::getId));

            for (LexicalImportRowEntity row : readyRows) {
                Long lexicalPairId = processReadyRow(batchId, row);
                if (lexicalPairId != null) {
                    importedLexicalPairIds.add(lexicalPairId);
                }
            }

            lexicalPairService.publishKnowledgeChangedEvent(importedLexicalPairIds);

            LexicalImportBatchEntity batch = refreshBatchCounts(batchId, LexicalImportBatchStatus.COMPLETED, null);
            batch.setImportJobFinishedAt(LocalDateTime.now());
            batchMapper.updateById(batch);
            log.info("event=lexical_import_batch_completed batchId={} importedRows={}", batchId, batch.getImportedRows());
        } catch (Exception exception) {
            if (!importedLexicalPairIds.isEmpty()) {
                try {
                    lexicalPairService.publishKnowledgeChangedEvent(importedLexicalPairIds);
                } catch (Exception republishException) {
                    log.warn("event=lexical_import_event_republish_failed batchId={} importedRows={} message={}",
                            batchId, importedLexicalPairIds.size(), resolveFailureMessage(republishException));
                }
            }
            LexicalImportBatchEntity batch = batchMapper.selectById(batchId);
            if (batch != null) {
                batch.setStatus(LexicalImportBatchStatus.FAILED.name());
                batch.setErrorMessage(resolveFailureMessage(exception));
                batch.setImportJobFinishedAt(LocalDateTime.now());
                applyCounts(batch, loadCounts(batchId));
                batchMapper.updateById(batch);
            }
            log.error("event=lexical_import_batch_failed batchId={} message={}", batchId, resolveFailureMessage(exception), exception);
        }
    }

    @Transactional
    protected Long processReadyRow(Long batchId, LexicalImportRowEntity row) {
        LexicalImportRowDraft draft = readDraft(row.getDraftJson());
        try {
            List<String> validationErrors = validateDraft(batchId, row.getId(), draft);
            if (!validationErrors.isEmpty()) {
                row.setRowStatus(LexicalImportRowStatus.INVALID.name());
                row.setValidationErrorsJson(writeJson(validationErrors));
                row.setImportMessage(validationErrors.getFirst());
                row.setImportedLexicalPairId(null);
                rowMapper.updateById(row);
                return null;
            }

            LexicalPairUpsertRequest request = templateSupport.toUpsertRequest(draft);
            Long lexicalPairId = lexicalPairService.createFromImport(request);
            row.setRowStatus(LexicalImportRowStatus.IMPORTED.name());
            row.setValidationErrorsJson(null);
            row.setImportMessage(null);
            row.setImportedLexicalPairId(lexicalPairId);
            rowMapper.updateById(row);
            return lexicalPairId;
        } catch (Exception exception) {
            List<String> errors = List.of(resolveFailureMessage(exception));
            row.setRowStatus(LexicalImportRowStatus.INVALID.name());
            row.setValidationErrorsJson(writeJson(errors));
            row.setImportMessage(errors.getFirst());
            row.setImportedLexicalPairId(null);
            rowMapper.updateById(row);
            return null;
        }
    }

    private LexicalImportBatchEntity refreshBatchCounts(
            Long batchId,
            LexicalImportBatchStatus status,
            String errorMessage
    ) {
        LexicalImportBatchEntity batch = batchMapper.selectById(batchId);
        if (batch == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Import batch was not found", 404);
        }
        applyCounts(batch, loadCounts(batchId));
        batch.setStatus(status.name());
        batch.setErrorMessage(errorMessage);
        batchMapper.updateById(batch);
        return batch;
    }

    private void applyCounts(LexicalImportBatchEntity batch, LexicalImportCounts counts) {
        batch.setTotalRows(counts.totalRows());
        batch.setReadyRows(counts.readyRows());
        batch.setInvalidRows(counts.invalidRows());
        batch.setSkippedRows(counts.skippedRows());
        batch.setImportedRows(counts.importedRows());
    }

    private LexicalImportCounts loadCounts(Long batchId) {
        int totalRows = rowMapper.selectCount(Wrappers.<LexicalImportRowEntity>lambdaQuery()
                .eq(LexicalImportRowEntity::getBatchId, batchId)).intValue();
        int readyRows = rowMapper.selectCount(Wrappers.<LexicalImportRowEntity>lambdaQuery()
                .eq(LexicalImportRowEntity::getBatchId, batchId)
                .eq(LexicalImportRowEntity::getRowStatus, LexicalImportRowStatus.READY.name())).intValue();
        int invalidRows = rowMapper.selectCount(Wrappers.<LexicalImportRowEntity>lambdaQuery()
                .eq(LexicalImportRowEntity::getBatchId, batchId)
                .eq(LexicalImportRowEntity::getRowStatus, LexicalImportRowStatus.INVALID.name())).intValue();
        int skippedRows = rowMapper.selectCount(Wrappers.<LexicalImportRowEntity>lambdaQuery()
                .eq(LexicalImportRowEntity::getBatchId, batchId)
                .eq(LexicalImportRowEntity::getRowStatus, LexicalImportRowStatus.SKIPPED.name())).intValue();
        int importedRows = rowMapper.selectCount(Wrappers.<LexicalImportRowEntity>lambdaQuery()
                .eq(LexicalImportRowEntity::getBatchId, batchId)
                .eq(LexicalImportRowEntity::getRowStatus, LexicalImportRowStatus.IMPORTED.name())).intValue();
        return new LexicalImportCounts(totalRows, readyRows, invalidRows, skippedRows, importedRows);
    }

    private List<String> validateDraft(Long batchId, Long rowId, LexicalImportRowDraft draft) {
        return validateDraft(batchId, rowId, draft, null);
    }

    private List<String> validateDraft(
            Long batchId,
            Long rowId,
            LexicalImportRowDraft draft,
            Set<String> seenPairKeys
    ) {
        List<String> errors = new ArrayList<>();
        try {
            LexicalPairUpsertRequest request = templateSupport.toUpsertRequest(draft);
            lexicalPairService.validateImportCandidate(request);
            String pairKey = normalizeImportKey(request);
            if (seenPairKeys != null && !seenPairKeys.add(pairKey)) {
                errors.add("Duplicate lexical pair in import file");
            } else if (hasDuplicateWithinBatch(batchId, rowId, pairKey)) {
                errors.add("Duplicate lexical pair in import batch");
            }
        } catch (Exception exception) {
            errors.add(resolveFailureMessage(exception));
        }
        return errors;
    }

    private boolean hasDuplicateWithinBatch(Long batchId, Long rowId, String pairKey) {
        List<LexicalImportRowEntity> rows = rowMapper.selectList(Wrappers.<LexicalImportRowEntity>lambdaQuery()
                .eq(LexicalImportRowEntity::getBatchId, batchId)
                .ne(rowId != null, LexicalImportRowEntity::getId, rowId));
        for (LexicalImportRowEntity candidate : rows) {
            if (LexicalImportRowStatus.SKIPPED.name().equals(candidate.getRowStatus())) {
                continue;
            }
            LexicalImportRowDraft candidateDraft = readDraft(candidate.getDraftJson());
            String candidateKey = normalizeImportKey(candidateDraft);
            if (pairKey.equals(candidateKey)) {
                return true;
            }
        }
        return false;
    }

    private String normalizePairKey(String englishWord, String frenchWord) {
        if (englishWord == null || frenchWord == null) {
            return "";
        }
        return englishWord.trim().toLowerCase(Locale.ROOT) + "::" + frenchWord.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeImportKey(LexicalPairUpsertRequest request) {
        if (hasText(request.sourceCode()) && hasText(request.contentVersion()) && hasText(request.wordId())) {
            return request.sourceCode().trim().toLowerCase(Locale.ROOT) + "::"
                    + request.contentVersion().trim().toLowerCase(Locale.ROOT) + "::"
                    + request.wordId().trim().toLowerCase(Locale.ROOT);
        }
        return normalizePairKey(request.englishWord(), request.frenchWord());
    }

    private String normalizeImportKey(LexicalImportRowDraft draft) {
        if (hasText(draft.sourceCode()) && hasText(draft.contentVersion()) && hasText(draft.wordId())) {
            return draft.sourceCode().trim().toLowerCase(Locale.ROOT) + "::"
                    + draft.contentVersion().trim().toLowerCase(Locale.ROOT) + "::"
                    + draft.wordId().trim().toLowerCase(Locale.ROOT);
        }
        return normalizePairKey(draft.englishWord(), draft.frenchWord());
    }

    private LexicalImportBatchEntity requireAccessibleBatch(Long batchId) {
        LexicalImportBatchEntity batch = batchMapper.selectById(batchId);
        if (batch == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Import batch was not found", 404);
        }
        if (!isAdmin() && !Objects.equals(batch.getOwnerUserId(), requireUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "You do not have permission to access this import batch", 403);
        }
        return batch;
    }

    private LexicalImportRowEntity requireRow(Long batchId, Long rowId) {
        LexicalImportRowEntity row = rowMapper.selectById(rowId);
        if (row == null || !Objects.equals(row.getBatchId(), batchId)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Import row was not found", 404);
        }
        return row;
    }

    private LexicalImportFileEntity requireFile(Long batchId) {
        LexicalImportFileEntity file = fileMapper.selectByBatchId(batchId);
        if (file == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Import file was not found", 404);
        }
        return file;
    }

    private Long resolveOwnerFilter(Long requestedOwnerUserId) {
        Long currentUserId = requireUserId();
        if (isAdmin()) {
            return requestedOwnerUserId;
        }
        if (requestedOwnerUserId != null && !Objects.equals(requestedOwnerUserId, currentUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "You do not have permission to access other users' imports", 403);
        }
        return currentUserId;
    }

    private Map<Long, String> loadOwnerDisplayNames(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(userIds).stream()
                .filter(Objects::nonNull)
                .collect(LinkedHashMap::new, (map, user) -> map.put(user.getId(), resolveDisplayName(user)), Map::putAll);
    }

    private String resolveDisplayName(UserEntity user) {
        if (user.getDisplayName() != null && !user.getDisplayName().isBlank()) {
            return user.getDisplayName();
        }
        return user.getUsername();
    }

    private byte[] readAndValidateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Import file must not be empty", 400);
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Import file must not exceed 50MB", 400);
        }
        resolveSourceFormat(file.getOriginalFilename());
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Failed to read import file", 400);
        }
    }

    private LexicalImportSourceFormat resolveSourceFormat(String filename) {
        String resolved = resolveFilename(filename).toLowerCase(Locale.ROOT);
        if (resolved.endsWith(".csv")) {
            return LexicalImportSourceFormat.CSV;
        }
        if (resolved.endsWith(".xlsx")) {
            return LexicalImportSourceFormat.XLSX;
        }
        throw new BusinessException(ResultCode.BAD_REQUEST, "Unsupported import file type", 400);
    }

    private String resolveFilename(String filename) {
        String resolved = trimToNull(filename);
        return resolved == null ? "import-file" : resolved;
    }

    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(content));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private LexicalImportBatchSummaryVO toSummaryVO(LexicalImportBatchEntity batch, String ownerDisplayName) {
        return new LexicalImportBatchSummaryVO(
                batch.getId(),
                batch.getStatus(),
                batch.getSourceFormat(),
                batch.getOriginalFilename(),
                batch.getContentType(),
                batch.getFileSizeBytes(),
                safeInt(batch.getTotalRows()),
                safeInt(batch.getReadyRows()),
                safeInt(batch.getInvalidRows()),
                safeInt(batch.getSkippedRows()),
                safeInt(batch.getImportedRows()),
                batch.getErrorMessage(),
                batch.getOwnerUserId(),
                ownerDisplayName,
                batch.getCreatedAt(),
                batch.getUpdatedAt(),
                batch.getParserJobFinishedAt(),
                batch.getImportJobFinishedAt()
        );
    }

    private LexicalImportBatchDetailVO toDetailVO(
            LexicalImportBatchEntity batch,
            LexicalImportFileEntity file,
            String ownerDisplayName,
            BatchEmbeddingSummary embeddingSummary
    ) {
        return new LexicalImportBatchDetailVO(
                batch.getId(),
                batch.getStatus(),
                batch.getSourceFormat(),
                batch.getOriginalFilename(),
                batch.getContentType(),
                batch.getFileSizeBytes(),
                safeInt(batch.getTotalRows()),
                safeInt(batch.getReadyRows()),
                safeInt(batch.getInvalidRows()),
                safeInt(batch.getSkippedRows()),
                safeInt(batch.getImportedRows()),
                embeddingSummary.pendingEmbeddingCount(),
                embeddingSummary.embeddedCount(),
                embeddingSummary.failedEmbeddingCount(),
                embeddingSummary.latestEmbeddedAt(),
                batch.getErrorMessage(),
                batch.getOwnerUserId(),
                ownerDisplayName,
                file.getSha256(),
                batch.getParserJobStartedAt(),
                batch.getParserJobFinishedAt(),
                batch.getImportJobStartedAt(),
                batch.getImportJobFinishedAt(),
                batch.getCreatedAt(),
                batch.getUpdatedAt()
        );
    }

    private BatchEmbeddingSummary loadBatchEmbeddingSummary(Long batchId) {
        List<Long> importedLexicalPairIds = loadImportedLexicalPairIds(batchId);
        if (importedLexicalPairIds.isEmpty()) {
            return new BatchEmbeddingSummary(0, 0, 0, null);
        }

        int pending = 0;
        int embedded = 0;
        int failed = 0;
        LocalDateTime latestEmbeddedAt = null;
        for (LexicalPairEntity pair : lexicalPairMapper.selectBatchIds(importedLexicalPairIds)) {
            String status = pair.getEmbeddingStatus();
            if (EmbeddingStatus.EMBEDDED.name().equals(status)) {
                embedded += 1;
                if (pair.getLastEmbeddedAt() != null && (latestEmbeddedAt == null || pair.getLastEmbeddedAt().isAfter(latestEmbeddedAt))) {
                    latestEmbeddedAt = pair.getLastEmbeddedAt();
                }
            } else if (EmbeddingStatus.FAILED.name().equals(status)) {
                failed += 1;
            } else {
                pending += 1;
            }
        }
        return new BatchEmbeddingSummary(pending, embedded, failed, latestEmbeddedAt);
    }

    private List<Long> loadImportedLexicalPairIds(Long batchId) {
        return rowMapper.selectList(Wrappers.<LexicalImportRowEntity>lambdaQuery()
                        .eq(LexicalImportRowEntity::getBatchId, batchId)
                        .isNotNull(LexicalImportRowEntity::getImportedLexicalPairId))
                .stream()
                .map(LexicalImportRowEntity::getImportedLexicalPairId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private record BatchEmbeddingSummary(
            int pendingEmbeddingCount,
            int embeddedCount,
            int failedEmbeddingCount,
            LocalDateTime latestEmbeddedAt
    ) {
    }

    private LexicalImportRowVO toRowVO(LexicalImportRowEntity row) {
        return new LexicalImportRowVO(
                row.getId(),
                row.getRowNumber(),
                row.getRowStatus(),
                readDraft(row.getDraftJson()),
                readErrors(row.getValidationErrorsJson()),
                row.getImportedLexicalPairId(),
                row.getImportMessage()
        );
    }

    private LexicalImportRowDraft readDraft(String draftJson) {
        try {
            return objectMapper.readValue(draftJson, LexicalImportRowDraft.class);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Failed to read lexical import row draft", exception);
        }
    }

    private List<String> readErrors(String errorsJson) {
        if (errorsJson == null || errorsJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(errorsJson, STRING_LIST_TYPE);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Failed to read lexical import row errors", exception);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Failed to serialize lexical import payload", exception);
        }
    }

    private String resolveFailureMessage(Exception exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof BusinessException businessException) {
                return businessException.getMessage();
            }
            current = current.getCause();
        }
        return exception.getMessage() == null ? "Unexpected import error" : exception.getMessage();
    }

    private JwtPrincipal requirePrincipal() {
        return SecurityUtils.getCurrentPrincipal()
                .orElseThrow(() -> new BusinessException(ResultCode.UNAUTHORIZED, "Authentication required", 401));
    }

    private Long requireUserId() {
        return requirePrincipal().userId();
    }

    private boolean isAdmin() {
        return SecurityUtils.getCurrentPrincipal()
                .map(principal -> principal.roles().contains("ADMIN"))
                .orElse(false);
    }

    private LexicalImportBatchStatus parseBatchStatus(String value) {
        try {
            return LexicalImportBatchStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException exception) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "Unsupported import batch status: " + value, 400);
        }
    }

    private LexicalImportRowStatus parseRowStatus(String value) {
        try {
            return LexicalImportRowStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException exception) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "Unsupported import row status: " + value, 400);
        }
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private void runWithPrincipal(JwtPrincipal principal, Runnable task) {
        SecurityContext previousContext = SecurityContextHolder.getContext();
        try {
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.authorities()));
            SecurityContextHolder.setContext(context);
            task.run();
        } finally {
            SecurityContextHolder.clearContext();
            if (previousContext != null && previousContext.getAuthentication() != null) {
                SecurityContextHolder.setContext(previousContext);
            }
        }
    }

    private void dispatchAfterCommit(Runnable task) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()
                || !TransactionSynchronizationManager.isActualTransactionActive()) {
            task.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                task.run();
            }
        });
    }
}
