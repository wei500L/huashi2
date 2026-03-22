package com.huashi.eftransfer.app.modules.lexicon.imports.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.huashi.eftransfer.app.common.persistence.BaseAuditEntity;

import java.time.LocalDateTime;

@TableName("lexical_import_batch")
public class LexicalImportBatchEntity extends BaseAuditEntity {

    @TableField("owner_user_id")
    private Long ownerUserId;

    private String originalFilename;
    private String contentType;

    @TableField("file_size_bytes")
    private Long fileSizeBytes;

    private String sourceFormat;
    private String status;

    @TableField("total_rows")
    private Integer totalRows;

    @TableField("ready_rows")
    private Integer readyRows;

    @TableField("invalid_rows")
    private Integer invalidRows;

    @TableField("skipped_rows")
    private Integer skippedRows;

    @TableField("imported_rows")
    private Integer importedRows;

    private String errorMessage;

    @TableField("parser_job_started_at")
    private LocalDateTime parserJobStartedAt;

    @TableField("parser_job_finished_at")
    private LocalDateTime parserJobFinishedAt;

    @TableField("import_job_started_at")
    private LocalDateTime importJobStartedAt;

    @TableField("import_job_finished_at")
    private LocalDateTime importJobFinishedAt;

    public Long getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(Long ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(Long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    public String getSourceFormat() {
        return sourceFormat;
    }

    public void setSourceFormat(String sourceFormat) {
        this.sourceFormat = sourceFormat;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(Integer totalRows) {
        this.totalRows = totalRows;
    }

    public Integer getReadyRows() {
        return readyRows;
    }

    public void setReadyRows(Integer readyRows) {
        this.readyRows = readyRows;
    }

    public Integer getInvalidRows() {
        return invalidRows;
    }

    public void setInvalidRows(Integer invalidRows) {
        this.invalidRows = invalidRows;
    }

    public Integer getSkippedRows() {
        return skippedRows;
    }

    public void setSkippedRows(Integer skippedRows) {
        this.skippedRows = skippedRows;
    }

    public Integer getImportedRows() {
        return importedRows;
    }

    public void setImportedRows(Integer importedRows) {
        this.importedRows = importedRows;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getParserJobStartedAt() {
        return parserJobStartedAt;
    }

    public void setParserJobStartedAt(LocalDateTime parserJobStartedAt) {
        this.parserJobStartedAt = parserJobStartedAt;
    }

    public LocalDateTime getParserJobFinishedAt() {
        return parserJobFinishedAt;
    }

    public void setParserJobFinishedAt(LocalDateTime parserJobFinishedAt) {
        this.parserJobFinishedAt = parserJobFinishedAt;
    }

    public LocalDateTime getImportJobStartedAt() {
        return importJobStartedAt;
    }

    public void setImportJobStartedAt(LocalDateTime importJobStartedAt) {
        this.importJobStartedAt = importJobStartedAt;
    }

    public LocalDateTime getImportJobFinishedAt() {
        return importJobFinishedAt;
    }

    public void setImportJobFinishedAt(LocalDateTime importJobFinishedAt) {
        this.importJobFinishedAt = importJobFinishedAt;
    }
}
