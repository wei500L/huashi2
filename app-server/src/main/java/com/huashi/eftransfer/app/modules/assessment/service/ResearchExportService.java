package com.huashi.eftransfer.app.modules.assessment.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huashi.eftransfer.app.common.audit.service.AuditLogService;
import com.huashi.eftransfer.app.common.util.SecurityUtils;
import com.huashi.eftransfer.app.modules.assessment.dto.ResearchExportRequest;
import com.huashi.eftransfer.app.modules.assessment.entity.ResearchExportJobEntity;
import com.huashi.eftransfer.app.modules.assessment.mapper.ResearchExportJobMapper;
import com.huashi.eftransfer.app.modules.assessment.support.AssessmentJsonCodec;
import com.huashi.eftransfer.app.modules.assessment.vo.ResearchAttemptSummaryVO;
import com.huashi.eftransfer.app.modules.assessment.vo.ResearchExportJobVO;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.enums.ResearchExportJobStatus;
import com.huashi.eftransfer.shared.exception.BusinessException;
import com.huashi.eftransfer.shared.page.PageResult;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class ResearchExportService {

    private final ResearchAccessService accessService;
    private final ResearchAnalyticsService analyticsService;
    private final ResearchExportJobMapper jobMapper;
    private final AssessmentJsonCodec jsonCodec;
    private final ObjectStorageService objectStorageService;
    private final AuditLogService auditLogService;

    public ResearchExportService(
            ResearchAccessService accessService,
            ResearchAnalyticsService analyticsService,
            ResearchExportJobMapper jobMapper,
            AssessmentJsonCodec jsonCodec,
            ObjectStorageService objectStorageService,
            AuditLogService auditLogService
    ) {
        this.accessService = accessService;
        this.analyticsService = analyticsService;
        this.jobMapper = jobMapper;
        this.jsonCodec = jsonCodec;
        this.objectStorageService = objectStorageService;
        this.auditLogService = auditLogService;
    }

    @Transactional
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
        processJob(job.getId());
        return toVo(jobMapper.selectById(job.getId()));
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
    public ResponseEntity<StreamingResponseBody> download(Long jobId) {
        ResearchExportJobEntity job = jobMapper.selectById(jobId);
        if (job == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Export job was not found", 404);
        }
        accessService.requireAccessibleResearchPublish(job.getPublishId());
        if (!ResearchExportJobStatus.COMPLETED.name().equalsIgnoreCase(job.getStatus()) || job.getObjectKey() == null) {
            throw new BusinessException(ResultCode.CONFLICT, "Export is not ready", 409);
        }
        StreamingResponseBody body = output -> {
            try (var input = objectStorageService.open(job.getObjectKey())) {
                input.transferTo(output);
            }
        };
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + job.getFileName() + "\"")
                .contentType(MediaType.parseMediaType(job.getMimeType()))
                .body(body);
    }

    @Scheduled(fixedDelayString = "PT20S")
    public void processPending() {
        var staleBefore = LocalDateTime.now().minusSeconds(120);
        for (Long id : jobMapper.selectProcessableIds(5, staleBefore)) {
            if (jobMapper.claimForProcessing(id, staleBefore) == 1) {
                processJob(id);
            }
        }
    }

    void processJob(Long jobId) {
        ResearchExportJobEntity job = jobMapper.selectById(jobId);
        if (job == null) {
            return;
        }
        try {
            ResearchQueryFilter filter = jsonCodec.read(job.getFilterJson(), ResearchQueryFilter.class);
            PageResult<ResearchAttemptSummaryVO> page = analyticsService.listAttempts(
                    job.getPublishId(), filter, 1, 10_000, "submittedAt,desc");
            byte[] bytes;
            String fileName;
            String mime;
            if ("XLSX".equalsIgnoreCase(job.getFormat())) {
                bytes = writeXlsx(page.records(), Boolean.TRUE.equals(job.getIncludeAttachmentManifest()));
                fileName = "research-export-" + job.getPublishId() + ".xlsx";
                mime = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            } else {
                bytes = writeCsv(page.records(), Boolean.TRUE.equals(job.getIncludeAttachmentManifest()));
                fileName = "research-export-" + job.getPublishId() + ".csv";
                mime = "text/csv";
            }
            String objectKey = "research-exports/" + job.getPublishId() + "/" + job.getJobKey() + "-" + fileName;
            objectStorageService.put(objectKey, new ByteArrayInputStream(bytes), bytes.length, mime);
            job.setStatus(ResearchExportJobStatus.COMPLETED.name());
            job.setObjectKey(objectKey);
            job.setFileName(fileName);
            job.setMimeType(mime);
            job.setCompletedAt(LocalDateTime.now());
            jobMapper.updateById(job);
        } catch (Exception exception) {
            job.setStatus(ResearchExportJobStatus.FAILED.name());
            job.setErrorMessage(exception.getMessage() == null ? "EXPORT_FAILED" : exception.getMessage());
            jobMapper.updateById(job);
        }
    }

    private byte[] writeCsv(List<ResearchAttemptSummaryVO> rows, boolean includeManifest) {
        StringBuilder builder = new StringBuilder();
        builder.append('\uFEFF');
        builder.append("participantCode,participantType,status,answeredCount,questionCount,percentageScore,effectiveDurationMs,qualityFlags,attachmentCount,aiAnalysisStatus,startedAt,lastSavedAt,submittedAt\n");
        for (ResearchAttemptSummaryVO row : rows) {
            builder.append(csv(row.participantCode())).append(',')
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

    private byte[] writeXlsx(List<ResearchAttemptSummaryVO> rows, boolean includeManifest) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("attempts");
            String[] headers = {
                    "participantCode", "participantType", "status", "answeredCount", "questionCount",
                    "percentageScore", "effectiveDurationMs", "qualityFlags", "attachmentCount",
                    "aiAnalysisStatus", "startedAt", "lastSavedAt", "submittedAt"
            };
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }
            int rowIndex = 1;
            for (ResearchAttemptSummaryVO row : rows) {
                Row excelRow = sheet.createRow(rowIndex++);
                excelRow.createCell(0).setCellValue(nullToEmpty(row.participantCode()));
                excelRow.createCell(1).setCellValue(nullToEmpty(row.participantType()));
                excelRow.createCell(2).setCellValue(nullToEmpty(row.status()));
                excelRow.createCell(3).setCellValue(row.answeredCount() == null ? 0 : row.answeredCount());
                excelRow.createCell(4).setCellValue(row.questionCount() == null ? 0 : row.questionCount());
                if (row.percentageScore() != null) {
                    excelRow.createCell(5).setCellValue(row.percentageScore());
                }
                if (row.effectiveDurationMs() != null) {
                    excelRow.createCell(6).setCellValue(row.effectiveDurationMs());
                }
                excelRow.createCell(7).setCellValue(row.qualityFlags() == null ? "" : String.join("|", row.qualityFlags()));
                excelRow.createCell(8).setCellValue(row.attachmentCount() == null ? 0 : row.attachmentCount());
                excelRow.createCell(9).setCellValue(nullToEmpty(row.aiAnalysisStatus()));
                excelRow.createCell(10).setCellValue(row.startedAt() == null ? "" : row.startedAt().toString());
                excelRow.createCell(11).setCellValue(row.lastSavedAt() == null ? "" : row.lastSavedAt().toString());
                excelRow.createCell(12).setCellValue(row.submittedAt() == null ? "" : row.submittedAt().toString());
            }
            if (includeManifest) {
                XSSFSheet manifest = workbook.createSheet("attachments");
                manifest.createRow(0).createCell(0).setCellValue("downloadPath");
                manifest.createRow(1).createCell(0).setCellValue("/api/teacher/research/files/{fileId}/download");
            }
            workbook.write(output);
            return output.toByteArray();
        }
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
