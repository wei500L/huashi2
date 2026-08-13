package com.huashi.eftransfer.app.modules.assessment.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huashi.eftransfer.app.common.audit.service.AuditLogService;
import com.huashi.eftransfer.app.common.security.JwtPrincipal;
import com.huashi.eftransfer.app.common.util.SecurityUtils;
import com.huashi.eftransfer.app.modules.user.service.UserQueryService;
import com.huashi.eftransfer.app.modules.assessment.dto.ResearchExportRequest;
import com.huashi.eftransfer.app.modules.assessment.entity.ResearchExportJobEntity;
import com.huashi.eftransfer.app.modules.assessment.mapper.ResearchExportJobMapper;
import com.huashi.eftransfer.app.modules.assessment.support.AssessmentJsonCodec;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentAiAnalysisEntity;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentAttemptAnswerEntity;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentAttemptEntity;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentMetricSnapshotEntity;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentParticipantEntity;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentQuestionEntity;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentSubmissionFileEntity;
import com.huashi.eftransfer.app.modules.assessment.support.AssessmentOptionPayload;
import com.huashi.eftransfer.app.modules.assessment.vo.AssessmentAiAnalysisVO;
import com.huashi.eftransfer.app.modules.assessment.vo.ResearchAiReportContentVO;
import com.huashi.eftransfer.app.modules.assessment.vo.ResearchAiReportVO;
import com.huashi.eftransfer.app.modules.assessment.vo.ResearchAttemptSummaryVO;
import com.huashi.eftransfer.app.modules.assessment.vo.ResearchDimensionStatisticVO;
import com.huashi.eftransfer.app.modules.assessment.vo.ResearchExportJobVO;
import com.huashi.eftransfer.app.modules.assessment.vo.ResearchFlagCountVO;
import com.huashi.eftransfer.app.modules.assessment.vo.ResearchOptionStatisticVO;
import com.huashi.eftransfer.app.modules.assessment.vo.ResearchPublishOverviewVO;
import com.huashi.eftransfer.app.modules.assessment.vo.ResearchQuestionStatisticVO;
import com.huashi.eftransfer.app.modules.assessment.vo.ResearchQuestionStatisticsVO;
import com.huashi.eftransfer.app.modules.assessment.vo.ResearchRateVO;
import com.huashi.eftransfer.app.modules.assessment.vo.ResearchReactionTimeStatisticVO;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.enums.ResearchExportJobStatus;
import com.huashi.eftransfer.shared.exception.BusinessException;
import com.huashi.eftransfer.shared.page.PageResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ResearchExportService {

    private final ResearchAccessService accessService;
    private final ResearchAnalyticsService analyticsService;
    private final ResearchAiReportService aiReportService;
    private final ResearchExportJobMapper jobMapper;
    private final AssessmentJsonCodec jsonCodec;
    private final ObjectStorageService objectStorageService;
    private final AuditLogService auditLogService;
    private final UserQueryService userQueryService;
    private final TaskExecutor researchExportTaskExecutor;
    private final TransactionTemplate transactionTemplate;

    public ResearchExportService(
            ResearchAccessService accessService,
            ResearchAnalyticsService analyticsService,
            ResearchAiReportService aiReportService,
            ResearchExportJobMapper jobMapper,
            AssessmentJsonCodec jsonCodec,
            ObjectStorageService objectStorageService,
            AuditLogService auditLogService,
            UserQueryService userQueryService,
            @Qualifier("researchExportTaskExecutor") TaskExecutor researchExportTaskExecutor,
            PlatformTransactionManager transactionManager
    ) {
        this.accessService = accessService;
        this.analyticsService = analyticsService;
        this.aiReportService = aiReportService;
        this.jobMapper = jobMapper;
        this.jsonCodec = jsonCodec;
        this.objectStorageService = objectStorageService;
        this.auditLogService = auditLogService;
        this.userQueryService = userQueryService;
        this.researchExportTaskExecutor = researchExportTaskExecutor;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public ResearchExportJobVO createExport(Long publishId, ResearchExportRequest request) {
        ResearchAccessService.ResearchPublishAccess access = accessService.requireAccessibleResearchPublish(publishId);
        boolean includeSensitive = Boolean.TRUE.equals(request.includeSensitiveFields());
        if (includeSensitive && !accessService.canViewSensitiveFields(access)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "Sensitive research fields require paper-owner permission", 403);
        }
        String format = request.format() == null ? "CSV" : request.format().trim().toUpperCase(Locale.ROOT);
        if (!"CSV".equals(format) && !"XLSX".equals(format)) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "Export format must be CSV or XLSX", 400);
        }
        Long jobId = transactionTemplate.execute(status -> {
            ResearchExportJobEntity job = new ResearchExportJobEntity();
            job.setPublishId(publishId);
            job.setJobKey(UUID.randomUUID().toString().replace("-", ""));
            job.setStatus(ResearchExportJobStatus.PENDING.name());
            job.setFormat(format);
            job.setScope(request.scope() == null || request.scope().isBlank() ? "ATTEMPTS" : request.scope());
            job.setFilterJson(jsonCodec.write(ResearchQueryFilter.from(
                    request.status(), request.entryType(), request.qualityFlag(), request.aiStatus(),
                    request.submittedFrom(), request.submittedTo(), request.keyword())));
            job.setIncludeSensitiveFields(includeSensitive);
            job.setIncludeAttachmentManifest(Boolean.TRUE.equals(request.includeAttachmentManifest()));
            job.setRequestedBy(SecurityUtils.getCurrentUserId().orElse(null));
            job.setRequestedAt(LocalDateTime.now());
            jobMapper.insert(job);
            if (includeSensitive) {
                auditLogService.record("RESEARCH_SENSITIVE_EXPORT_CREATED", "RESEARCH_EXPORT_JOB", String.valueOf(job.getId()),
                        Map.of("publishId", publishId), "SUCCESS");
            }
            return job.getId();
        });
        if (jobId == null) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "Failed to create export job", 500);
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        researchExportTaskExecutor.execute(() -> claimAndProcess(jobId, authentication));
        return toVo(jobMapper.selectById(jobId));
    }

    @Transactional(readOnly = true)
    public ResearchExportJobVO getJob(Long jobId) {
        ResearchExportJobEntity job = jobMapper.selectById(jobId);
        if (job == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Export job was not found", 404);
        }
        accessService.requireAccessibleResearchPublish(job.getPublishId());
        return toVo(job);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> download(Long jobId) {
        ResearchExportJobEntity job = jobMapper.selectById(jobId);
        if (job == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Export job was not found", 404);
        }
        accessService.requireAccessibleResearchPublish(job.getPublishId());
        if (!ResearchExportJobStatus.COMPLETED.name().equalsIgnoreCase(job.getStatus()) || job.getObjectKey() == null) {
            throw new BusinessException(ResultCode.CONFLICT, "Export is not ready", 409);
        }
        byte[] bytes;
        try (var input = objectStorageService.open(job.getObjectKey())) {
            bytes = input.readAllBytes();
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Failed to read research export", exception);
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ResearchExportWorkbook.contentDisposition(job.getFileName()))
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(bytes.length))
                .contentType(MediaType.parseMediaType(job.getMimeType()))
                .body(bytes);
    }

    @Scheduled(fixedDelayString = "PT20S")
    public void processPending() {
        var staleBefore = LocalDateTime.now().minusSeconds(120);
        for (Long id : jobMapper.selectProcessableIds(5, staleBefore)) {
            if (jobMapper.claimForProcessing(id, staleBefore) == 1) {
                processJob(id, authenticationForJob(id));
            }
        }
    }

    void claimAndProcess(Long jobId, Authentication authentication) {
        LocalDateTime staleBefore = LocalDateTime.now().minusSeconds(120);
        if (jobMapper.claimForProcessing(jobId, staleBefore) == 1) {
            processJob(jobId, authentication);
        }
    }

    private Authentication authenticationForJob(Long jobId) {
        ResearchExportJobEntity job = jobMapper.selectById(jobId);
        if (job == null || job.getRequestedBy() == null) {
            return null;
        }
        return userQueryService.findEnabledById(job.getRequestedBy())
                .map(user -> {
                    java.util.Set<String> roles = userQueryService.getRoleCodes(user.getId());
                    var authorities = roles.stream()
                            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                            .collect(java.util.stream.Collectors.toSet());
                    JwtPrincipal principal = new JwtPrincipal(
                            user.getId(),
                            user.getUsername(),
                            user.getDisplayName(),
                            roles,
                            "research-export",
                            java.time.Instant.now().plusSeconds(3600),
                            authorities
                    );
                    return (Authentication) new UsernamePasswordAuthenticationToken(principal, null, authorities);
                })
                .orElse(null);
    }

    void processJob(Long jobId, Authentication authentication) {
        ResearchExportJobEntity job = jobMapper.selectById(jobId);
        if (job == null) {
            return;
        }
        SecurityContext previous = SecurityContextHolder.getContext();
        try {
            if (authentication != null) {
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authentication);
                SecurityContextHolder.setContext(context);
            }
            processJobInternal(job);
        } finally {
            SecurityContextHolder.setContext(previous);
        }
    }

    private void processJobInternal(ResearchExportJobEntity job) {
        try {
            ResearchQueryFilter filter = jsonCodec.read(job.getFilterJson(), ResearchQueryFilter.class);
            byte[] bytes;
            String fileName;
            String mime;
            String extension;
            if ("XLSX".equalsIgnoreCase(job.getFormat())) {
                BuiltExport built = writeResearchXlsx(job.getPublishId(), filter, Boolean.TRUE.equals(job.getIncludeSensitiveFields()));
                bytes = built.bytes();
                fileName = built.fileName();
                mime = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                extension = "xlsx";
            } else {
                PageResult<ResearchAttemptSummaryVO> page = analyticsService.listAttempts(
                        job.getPublishId(), filter, 1, 10_000, "submittedAt,desc");
                bytes = writeCsv(page.records(), Boolean.TRUE.equals(job.getIncludeAttachmentManifest()));
                fileName = ResearchExportWorkbook.exportFileName("研究问卷", null, "csv");
                mime = "text/csv";
                extension = "csv";
            }
            String objectKey = "research-exports/" + job.getPublishId() + "/" + job.getJobKey() + "." + extension;
            objectStorageService.put(objectKey, new ByteArrayInputStream(bytes), bytes.length, mime);
            job.setStatus(ResearchExportJobStatus.COMPLETED.name());
            job.setObjectKey(objectKey);
            job.setFileName(fileName);
            job.setMimeType(mime);
            job.setCompletedAt(LocalDateTime.now());
            jobMapper.updateById(job);
        } catch (Throwable exception) {
            job.setStatus(ResearchExportJobStatus.FAILED.name());
            job.setErrorMessage(exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage());
            try {
                jobMapper.updateById(job);
            } catch (Throwable ignored) {
                // Keep the original generation failure as the job outcome.
            }
        }
    }

    private byte[] writeCsv(List<ResearchAttemptSummaryVO> rows, boolean includeManifest) {
        StringBuilder builder = new StringBuilder();
        builder.append('\uFEFF');
        builder.append("participantCode,attemptNo,participantType,status,answeredCount,questionCount,percentageScore,effectiveDurationMs,qualityFlags,attachmentCount,aiAnalysisStatus,startedAt,lastSavedAt,submittedAt\n");
        for (ResearchAttemptSummaryVO row : rows) {
            builder.append(csv(row.participantCode())).append(',')
                    .append(row.attemptNo() == null ? 1 : row.attemptNo()).append(',')
                    .append(csv(row.participantType())).append(',')
                    .append(csv(row.status())).append(',')
                    .append(row.answeredCount()).append(',')
                    .append(row.questionCount()).append(',')
                    .append(row.percentageScore() == null ? "" : row.percentageScore()).append(',')
                    .append(row.effectiveDurationMs() == null ? "" : row.effectiveDurationMs()).append(',')
                    .append(csv(row.qualityFlags() == null ? "" : String.join("|", row.qualityFlags()))).append(',')
                    .append(row.attachmentCount()).append(',')
                    .append(csv(row.aiAnalysisStatus())).append(',')
                    .append(csv(row.startedAt() == null ? "" : row.startedAt().toString())).append(',')
                    .append(csv(row.lastSavedAt() == null ? "" : row.lastSavedAt().toString())).append(',')
                    .append(csv(row.submittedAt() == null ? "" : row.submittedAt().toString()))
                    .append('\n');
        }
        if (includeManifest) {
            builder.append("\nattachmentManifestHint,download via /api/teacher/research/files/{fileId}/download\n");
        }
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    private BuiltExport writeResearchXlsx(Long publishId, ResearchQueryFilter filter, boolean includeSensitiveFields) throws Exception {
        ResearchAnalyticsService.ResearchExportMaterial material = analyticsService.loadExportMaterial(publishId, filter);
        Map<Long, AssessmentQuestionEntity> questionsById = material.questions().stream()
                .collect(Collectors.toMap(AssessmentQuestionEntity::getId, question -> question, (left, right) -> left, LinkedHashMap::new));
        List<ResearchExportWorkbook.AttemptRow> attempts = new ArrayList<>();
        Map<Long, String> participantCodes = new LinkedHashMap<>();
        for (AssessmentAttemptEntity attempt : material.attempts()) {
            AssessmentParticipantEntity participant = material.participantsByAttempt().get(attempt.getId());
            AssessmentMetricSnapshotEntity metric = material.metricsByAttempt().get(attempt.getId());
            AssessmentAiAnalysisEntity analysis = material.analysesByAttempt().get(attempt.getId());
            List<AssessmentSubmissionFileEntity> files = material.filesByAttempt().getOrDefault(attempt.getId(), List.of());
            String code = analyticsService.formatParticipantCode(attempt.getParticipantId());
            participantCodes.put(attempt.getId(), code);
            Long durationMs = attempt.getStartedAt() == null || attempt.getSubmittedAt() == null
                    ? null
                    : Math.max(0, java.time.Duration.between(attempt.getStartedAt(), attempt.getSubmittedAt()).toMillis());
            attempts.add(new ResearchExportWorkbook.AttemptRow(
                    code,
                    PublicAssessmentService.attemptNoOf(attempt),
                    participant == null ? null : participant.getParticipantType(),
                    attempt.getStatus(),
                    attempt.getSubmitReason(),
                    attempt.getAnsweredCount(),
                    material.questions().isEmpty() ? attempt.getAnsweredCount() : material.questions().size(),
                    metric == null || metric.getPercentageScore() == null ? null : metric.getPercentageScore().doubleValue(),
                    durationMs,
                    metric == null ? List.of() : jsonCodec.readStringList(metric.getQualityFlagsJson()),
                    files.size(),
                    analysis == null ? null : analysis.getStatus(),
                    attempt.getStartedAt(),
                    attempt.getLastSavedAt(),
                    attempt.getSubmittedAt()
            ));
        }
        attempts.sort(Comparator.comparing((ResearchExportWorkbook.AttemptRow row) -> nullToEmpty(row.submittedAt() == null ? null : row.submittedAt().toString())).reversed());

        List<ResearchExportWorkbook.AnswerRow> answers = new ArrayList<>();
        for (AssessmentAttemptAnswerEntity answer : material.answers()) {
            AssessmentQuestionEntity question = questionsById.get(answer.getQuestionId());
            String sectionCode = question == null ? null : question.getSectionCode();
            boolean formal = ResearchExportWorkbook.resolveFormalSection(sectionCode, answer.getQuestionType());
            answers.add(new ResearchExportWorkbook.AnswerRow(
                    participantCodes.getOrDefault(answer.getAttemptId(), analyticsService.formatParticipantCode(null)),
                    answer.getQuestionOrder(),
                    answer.getQuestionType(),
                    sectionCode,
                    formal,
                    firstNonBlank(answer.getStemTextSnapshot(), question == null ? null : question.getStemText()),
                    formatOptions(answer.getOptionsJsonSnapshot(), question == null ? null : question.getOptionsJson()),
                    joinResponses(jsonCodec.readStringList(answer.getResponseJson())),
                    answer.getJustificationText(),
                    answer.getCorrect(),
                    answer.getScoreAwarded(),
                    answer.getQuestionScore(),
                    answer.getEffectiveDurationMs(),
                    answer.getResponseChangeCount()
            ));
        }

        List<ResearchExportWorkbook.QuestionRow> questions = material.questions().stream()
                .sorted(Comparator.comparing(question -> question.getSortOrder() == null ? 0 : question.getSortOrder()))
                .map(question -> new ResearchExportWorkbook.QuestionRow(
                        question.getSortOrder(),
                        question.getQuestionType(),
                        question.getSectionCode(),
                        ResearchExportWorkbook.resolveFormalSection(question.getSectionCode(), question.getQuestionType()),
                        question.getStemText(),
                        formatOptions(question.getOptionsJson(), null),
                        joinResponses(jsonCodec.readStringList(question.getCorrectAnswerJson()))
                ))
                .toList();

        List<ResearchExportWorkbook.AttachmentRow> attachments = new ArrayList<>();
        for (Map.Entry<Long, List<AssessmentSubmissionFileEntity>> entry : material.filesByAttempt().entrySet()) {
            String code = participantCodes.getOrDefault(entry.getKey(), "");
            for (AssessmentSubmissionFileEntity file : entry.getValue()) {
                AssessmentQuestionEntity question = questionsById.get(file.getQuestionId());
                attachments.add(new ResearchExportWorkbook.AttachmentRow(
                        code,
                        question == null ? null : question.getSortOrder(),
                        file.getId(),
                        file.getOriginalFileName(),
                        file.getMimeType(),
                        file.getSizeBytes(),
                        file.getScanStatus()
                ));
            }
        }

        String generatedAt = LocalDateTime.now(ZoneOffset.UTC)
                .atZone(ZoneOffset.UTC)
                .withZoneSameInstant(ZoneId.of("Asia/Shanghai"))
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        ResearchExportWorkbook.WorkbookData workbook = new ResearchExportWorkbook.WorkbookData(
                material.paperTitle(),
                material.releaseCode(),
                generatedAt,
                filterSummary(filter),
                attempts,
                answers,
                questions,
                attachments,
                includeSensitiveFields
        );
        populateAnalytics(workbook, publishId, filter, questionsById, participantCodes, material);
        return new BuiltExport(
                ResearchExportWorkbook.write(workbook),
                ResearchExportWorkbook.exportFileName(material.paperTitle(), material.releaseCode(), "xlsx")
        );
    }

    private record BuiltExport(byte[] bytes, String fileName) {
    }

    private void populateAnalytics(
            ResearchExportWorkbook.WorkbookData workbook,
            Long publishId,
            ResearchQueryFilter filter,
            Map<Long, AssessmentQuestionEntity> questionsById,
            Map<Long, String> participantCodes,
            ResearchAnalyticsService.ResearchExportMaterial material
    ) {
        ResearchPublishOverviewVO overview = analyticsService.overview(publishId, filter);
        ResearchQuestionStatisticsVO questionStats = analyticsService.questionStatistics(publishId, filter);
        workbook.summary = buildSummary(overview, workbook.attempts());
        workbook.questionStats = questionStats.questions().stream().map(row -> {
            AssessmentQuestionEntity question = questionsById.get(row.questionId());
            long answered = row.answeredCount();
            long skipped = row.skippedCount();
            long total = answered + skipped;
            return new ResearchExportWorkbook.QuestionStatRow(
                    row.questionOrder(),
                    row.questionCode(),
                    ResearchExportWorkbook.sectionLabel(row.sectionTitle(), ResearchExportWorkbook.resolveFormalSection(row.sectionTitle(), row.questionType())),
                    row.questionType(),
                    question == null ? "" : question.getStemText(),
                    answered,
                    skipped,
                    row.correctRate(),
                    total == 0 ? null : skipped / (double) total,
                    row.medianReactionMs(),
                    row.qualityWarning()
            );
        }).toList();
        workbook.hardQuestions = workbook.questionStats.stream()
                .filter(row -> row.correctRate() != null && row.answeredCount() >= 3)
                .sorted(Comparator.comparing(ResearchExportWorkbook.QuestionStatRow::correctRate)
                        .thenComparing(ResearchExportWorkbook.QuestionStatRow::questionOrder, Comparator.nullsLast(Integer::compareTo)))
                .limit(15)
                .toList();
        workbook.optionStats = analyticsService.optionStatistics(publishId, filter).questions().stream()
                .flatMap(question -> {
                    AssessmentQuestionEntity entity = questionsById.get(question.questionId());
                    List<String> correct = entity == null ? List.of() : jsonCodec.readStringList(entity.getCorrectAnswerJson());
                    List<ResearchOptionStatisticVO.ResearchOptionShareVO> options = question.options() == null ? List.of() : question.options();
                    return options.stream().map(option -> new ResearchExportWorkbook.OptionStatRow(
                            question.questionOrder(),
                            question.questionCode(),
                            entity == null ? "" : entity.getStemText(),
                            option.optionKey(),
                            option.optionLabel(),
                            option.optionKey() != null && correct.stream().anyMatch(value -> option.optionKey().equalsIgnoreCase(value)),
                            option.count(),
                            option.answeredShare(),
                            option.submittedShare()
                    ));
                })
                .toList();
        workbook.dimensions = analyticsService.dimensionStatistics(publishId, filter).dimensions().stream()
                .map(row -> new ResearchExportWorkbook.DimensionStatRow(row.dimension(), row.answeredCount(), row.correctCount(), row.correctRate()))
                .toList();
        workbook.reactionTimes = analyticsService.reactionTimeStatistics(publishId, filter).questions().stream()
                .map(row -> new ResearchExportWorkbook.ReactionStatRow(
                        row.questionOrder(), row.questionCode(), row.sampleCount(),
                        row.medianMs(), row.q1Ms(), row.q3Ms(), row.p90Ms()))
                .toList();
        long submitted = overview.funnel() == null ? 0 : overview.funnel().submitted();
        workbook.qualityFlags = overview.dataQuality() == null || overview.dataQuality().flagDistribution() == null
                ? List.of()
                : overview.dataQuality().flagDistribution().stream()
                .map(flag -> new ResearchExportWorkbook.QualityFlagRow(
                        flag.flag(),
                        flag.count(),
                        submitted == 0 ? null : flag.count() / (double) submitted))
                .toList();
        populateGroupAi(workbook, publishId);
        workbook.attemptAi = material.attempts().stream()
                .map(attempt -> toAttemptAiRow(attempt, material.analysesByAttempt().get(attempt.getId()), participantCodes))
                .filter(Objects::nonNull)
                .toList();
    }

    private List<ResearchExportWorkbook.KvRow> buildSummary(ResearchPublishOverviewVO overview, List<ResearchExportWorkbook.AttemptRow> attempts) {
        List<ResearchExportWorkbook.KvRow> rows = new ArrayList<>();
        var funnel = overview.funnel();
        var rates = overview.rates();
        addKv(rows, "回收漏斗", "已生成参与码", funnel == null ? null : funnel.codeGenerated(), "不含已停用");
        addKv(rows, "回收漏斗", "已核销参与码", funnel == null ? null : funnel.codeVerified(), "至少验证过一次");
        addKv(rows, "回收漏斗", "参与者", funnel == null ? null : funnel.participantCreated(), "创建了研究参与者档案");
        addKv(rows, "回收漏斗", "已开始", funnel == null ? null : funnel.attemptStarted(), "创建了答卷会话");
        addKv(rows, "回收漏斗", "作答中", funnel == null ? null : funnel.inProgress(), "");
        addKv(rows, "回收漏斗", "已提交", funnel == null ? null : funnel.submitted(), "");
        addKv(rows, "回收漏斗", "超时提交", funnel == null ? null : funnel.expired(), "submitReason=TIMEOUT");
        addKv(rows, "完成率", "完成率", formatRate(rates == null ? null : rates.completionRate()), "已提交 / 已开始");
        addKv(rows, "完成率", "参与码兑换率", formatRate(rates == null ? null : rates.codeRedemptionRate()), "已核销 / 已生成");
        addKv(rows, "完成率", "提交率", formatRate(rates == null ? null : rates.submissionRate()), "已提交 / 参与者");
        addKv(rows, "分数", "平均分", overview.score() == null || overview.score().average() == null ? "" : round(overview.score().average()), "参考分，0-100");
        addKv(rows, "分数", "中位分", overview.score() == null || overview.score().median() == null ? "" : overview.score().median(), "样本 " + (overview.score() == null ? 0 : overview.score().sampleCount()));
        addKv(rows, "分数", "P25 / P75 / P90", overview.score() == null ? "" : joinStats(overview.score().q1(), overview.score().q3(), overview.score().p90()), "");
        addKv(rows, "用时", "中位用时（秒）", overview.timing() == null ? "" : secondsText(overview.timing().median()), "从开始到提交");
        addKv(rows, "用时", "平均用时（秒）", overview.timing() == null || overview.timing().average() == null ? "" : round(overview.timing().average() / 1000d), "");
        addKv(rows, "用时", "P25 / P75 / P90（秒）", overview.timing() == null ? "" : joinStats(
                toWholeSeconds(overview.timing().q1()), toWholeSeconds(overview.timing().q3()), toWholeSeconds(overview.timing().p90())), "");
        addKv(rows, "质量", "有效样本", overview.dataQuality() == null ? null : overview.dataQuality().valid(), "已提交且无质量标记");
        addKv(rows, "质量", "带质量标记", overview.dataQuality() == null ? null : overview.dataQuality().flagged(), "过快/过短/计时缺失");
        addKv(rows, "AI", "模型完成", overview.ai() == null ? null : overview.ai().completed(), "");
        addKv(rows, "AI", "规则摘要", overview.ai() == null ? null : overview.ai().fallback(), "模型未出时的规则降级");
        addKv(rows, "AI", "失败 / 处理中", overview.ai() == null ? "" : overview.ai().failed() + " / " + (overview.ai().pending() + overview.ai().processing()), "");
        addKv(rows, "结构", "当前导出答卷数", attempts.size(), "受页面筛选影响");
        long timeout = attempts.stream().filter(row -> "TIMEOUT".equalsIgnoreCase(row.submitReason())).count();
        long qr = attempts.stream().filter(row -> "PUBLIC_QR".equalsIgnoreCase(row.participantType())).count();
        addKv(rows, "结构", "超时提交数", timeout, "");
        addKv(rows, "结构", "二维码进入数", qr, "");
        addKv(rows, "结构", "最近提交", ResearchExportWorkbook.formatDateTime(overview.latestSubmissionAt()), "北京时间");
        return rows;
    }

    private void populateGroupAi(ResearchExportWorkbook.WorkbookData workbook, Long publishId) {
        ResearchAiReportVO report;
        try {
            report = aiReportService.latest(publishId);
        } catch (RuntimeException exception) {
            workbook.groupAiFindings = List.of(new ResearchExportWorkbook.GroupAiFindingRow("说明", null, "读取群体报告失败：" + exception.getMessage()));
            return;
        }
        if (report == null) {
            return;
        }
        List<ResearchExportWorkbook.KvRow> meta = new ArrayList<>();
        addKv(meta, "报告信息", "状态", report.status() == null ? "" : report.status(), report.source() == null ? "" : report.source());
        addKv(meta, "报告信息", "来源", "RULE_FALLBACK".equalsIgnoreCase(report.source()) ? "规则摘要" : report.source() == null ? "处理中" : "模型", report.modelName());
        addKv(meta, "报告信息", "样本量", report.sampleCount(), "绑定统计快照");
        addKv(meta, "报告信息", "完成时间", ResearchExportWorkbook.formatDateTime(report.completedAt()), "");
        addKv(meta, "报告信息", "降级原因", report.fallbackReason() == null ? "" : report.fallbackReason(), "");
        workbook.groupAiMeta = meta;
        ResearchAiReportContentVO content = report.report() != null ? report.report() : report.ruleFallback();
        if (content == null) {
            workbook.groupAiFindings = List.of(new ResearchExportWorkbook.GroupAiFindingRow("说明", null, "报告仍在生成，或尚未写出解读正文。"));
            return;
        }
        List<ResearchExportWorkbook.GroupAiFindingRow> findings = new ArrayList<>();
        if (content.confidence() != null) {
            findings.add(new ResearchExportWorkbook.GroupAiFindingRow("置信度", null, ResearchExportWorkbook.percent(content.confidence())));
        }
        addFinding(findings, "执行摘要", content.executiveSummary() == null ? List.of() : List.of(content.executiveSummary()));
        addFinding(findings, "观察到的模式", content.observedPatterns());
        addFinding(findings, "维度发现", content.dimensionFindings());
        addFinding(findings, "困难题目", content.difficultQuestions());
        addFinding(findings, "干扰项", content.distractorFindings());
        addFinding(findings, "反应时", content.reactionTimeFindings());
        addFinding(findings, "质量限制", content.dataQualityLimitations());
        addFinding(findings, "研究提醒", content.researchCautions());
        addFinding(findings, "建议的下一步分析", content.recommendedNextAnalyses());
        workbook.groupAiFindings = findings;
    }

    private ResearchExportWorkbook.AttemptAiRow toAttemptAiRow(
            AssessmentAttemptEntity attempt,
            AssessmentAiAnalysisEntity analysis,
            Map<Long, String> participantCodes
    ) {
        if (analysis == null) {
            return null;
        }
        AssessmentAiAnalysisVO vo = decodeAttemptAi(analysis);
        String source = analysis.getAnalysisJson() != null && !analysis.getAnalysisJson().isBlank()
                ? (analysis.getModelName() == null ? "模型" : analysis.getModelName())
                : "规则摘要";
        return new ResearchExportWorkbook.AttemptAiRow(
                participantCodes.getOrDefault(attempt.getId(), analyticsService.formatParticipantCode(attempt.getParticipantId())),
                analysis.getStatus(),
                source,
                analysis.getModelName(),
                analysis.getCompletedAt(),
                vo == null ? "" : vo.performanceOverview(),
                vo == null ? "" : ResearchExportWorkbook.joinList(vo.strengths()),
                vo == null ? "" : ResearchExportWorkbook.joinList(vo.risks()),
                vo == null ? "" : vo.contextInterpretation(),
                vo == null ? "" : vo.reactionTimeInterpretation(),
                vo == null ? "" : ResearchExportWorkbook.joinList(vo.recommendations()),
                vo == null ? null : vo.confidence(),
                vo == null ? "" : vo.qualityNotice(),
                analysis.getFallbackReason()
        );
    }

    private AssessmentAiAnalysisVO decodeAttemptAi(AssessmentAiAnalysisEntity analysis) {
        String payload = analysis.getAnalysisJson() == null || analysis.getAnalysisJson().isBlank()
                ? analysis.getRuleFallbackJson()
                : analysis.getAnalysisJson();
        if (payload == null || payload.isBlank()) {
            return null;
        }
        try {
            return jsonCodec.read(payload, AssessmentAiAnalysisVO.class);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private void addFinding(List<ResearchExportWorkbook.GroupAiFindingRow> findings, String section, List<String> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        int order = 1;
        for (String item : items) {
            if (item == null || item.isBlank()) {
                continue;
            }
            findings.add(new ResearchExportWorkbook.GroupAiFindingRow(section, order++, item));
        }
    }

    private void addKv(List<ResearchExportWorkbook.KvRow> rows, String group, String item, Object value, String note) {
        rows.add(new ResearchExportWorkbook.KvRow(group, item, value == null ? "" : String.valueOf(value), note == null ? "" : note));
    }

    private String formatRate(ResearchRateVO rate) {
        if (rate == null) {
            return "";
        }
        String percent = ResearchExportWorkbook.percent(rate.value());
        return (percent.isBlank() ? "—" : percent) + "（" + rate.numerator() + "/" + rate.denominator() + "）";
    }

    private String joinStats(Object q1, Object q3, Object p90) {
        return (q1 == null ? "—" : q1) + " / " + (q3 == null ? "—" : q3) + " / " + (p90 == null ? "—" : p90);
    }

    private String secondsText(Long millis) {
        if (millis == null) {
            return "";
        }
        return String.valueOf(Math.round(millis / 1000d));
    }

    private Long toWholeSeconds(Long millis) {
        return millis == null ? null : Math.round(millis / 1000d);
    }

    private String round(double value) {
        return String.valueOf(Math.round(value * 10d) / 10d);
    }

    private String formatOptions(String primaryJson, String fallbackJson) {
        List<AssessmentOptionPayload> options = jsonCodec.readOptions(primaryJson);
        if (options.isEmpty() && fallbackJson != null) {
            options = jsonCodec.readOptions(fallbackJson);
        }
        return options.stream()
                .map(option -> {
                    String key = option.key() == null ? "" : option.key();
                    String label = option.label() == null ? "" : option.label();
                    return key.isBlank() ? label : key + ". " + label;
                })
                .filter(value -> !value.isBlank())
                .collect(Collectors.joining(" | "));
    }

    private String joinResponses(List<String> responses) {
        if (responses == null || responses.isEmpty()) {
            return "";
        }
        return responses.stream().filter(Objects::nonNull).map(String::trim).filter(value -> !value.isEmpty()).collect(Collectors.joining("、"));
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private String filterSummary(ResearchQueryFilter filter) {
        if (filter == null) {
            return "全部答卷";
        }
        List<String> parts = new ArrayList<>();
        if (filter.status() != null) {
            parts.add("状态=" + filter.status());
        }
        if (filter.entryType() != null) {
            parts.add("进入方式=" + filter.entryType());
        }
        if (filter.qualityFlag() != null) {
            parts.add("质量=" + filter.qualityFlag());
        }
        if (filter.keyword() != null) {
            parts.add("关键词=" + filter.keyword());
        }
        return parts.isEmpty() ? "全部答卷" : String.join("；", parts);
    }

    private ResearchExportJobVO toVo(ResearchExportJobEntity job) {
        String downloadPath = ResearchExportJobStatus.COMPLETED.name().equalsIgnoreCase(job.getStatus())
                ? "/api/teacher/research/exports/" + job.getId() + "/download"
                : null;
        return new ResearchExportJobVO(
                job.getId(),
                job.getJobKey(),
                job.getPublishId(),
                job.getStatus(),
                job.getFormat(),
                job.getScope(),
                Boolean.TRUE.equals(job.getIncludeSensitiveFields()),
                Boolean.TRUE.equals(job.getIncludeAttachmentManifest()),
                job.getFileName(),
                downloadPath,
                job.getErrorMessage(),
                job.getRequestedAt(),
                job.getCompletedAt()
        );
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
