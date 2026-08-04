package com.huashi.eftransfer.app.modules.assessment.imports.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.huashi.eftransfer.app.common.persistence.BaseAuditEntity;

import java.time.LocalDateTime;

@TableName("assessment_question_bank_import")
public class QuestionBankImportEntity extends BaseAuditEntity {
    @TableField("question_bank_id") private Long questionBankId;
    @TableField("import_key") private String importKey;
    @TableField("source_file_name") private String sourceFileName;
    @TableField("source_format") private String sourceFormat;
    @TableField("source_sha256") private String sourceSha256;
    private String status;
    @TableField("source_payload_json") private String sourcePayloadJson;
    @TableField("preflight_summary_json") private String preflightSummaryJson;
    @TableField("differences_json") private String differencesJson;
    @TableField("errors_json") private String errorsJson;
    @TableField("confirmed_by") private Long confirmedBy;
    @TableField("confirmed_at") private LocalDateTime confirmedAt;
    @TableField("committed_by") private Long committedBy;
    @TableField("committed_at") private LocalDateTime committedAt;

    public Long getQuestionBankId() { return questionBankId; }
    public void setQuestionBankId(Long value) { this.questionBankId = value; }
    public String getImportKey() { return importKey; }
    public void setImportKey(String value) { this.importKey = value; }
    public String getSourceFileName() { return sourceFileName; }
    public void setSourceFileName(String value) { this.sourceFileName = value; }
    public String getSourceFormat() { return sourceFormat; }
    public void setSourceFormat(String value) { this.sourceFormat = value; }
    public String getSourceSha256() { return sourceSha256; }
    public void setSourceSha256(String value) { this.sourceSha256 = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { this.status = value; }
    public String getSourcePayloadJson() { return sourcePayloadJson; }
    public void setSourcePayloadJson(String value) { this.sourcePayloadJson = value; }
    public String getPreflightSummaryJson() { return preflightSummaryJson; }
    public void setPreflightSummaryJson(String value) { this.preflightSummaryJson = value; }
    public String getDifferencesJson() { return differencesJson; }
    public void setDifferencesJson(String value) { this.differencesJson = value; }
    public String getErrorsJson() { return errorsJson; }
    public void setErrorsJson(String value) { this.errorsJson = value; }
    public Long getConfirmedBy() { return confirmedBy; }
    public void setConfirmedBy(Long value) { this.confirmedBy = value; }
    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(LocalDateTime value) { this.confirmedAt = value; }
    public Long getCommittedBy() { return committedBy; }
    public void setCommittedBy(Long value) { this.committedBy = value; }
    public LocalDateTime getCommittedAt() { return committedAt; }
    public void setCommittedAt(LocalDateTime value) { this.committedAt = value; }
}
