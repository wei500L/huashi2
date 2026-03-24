package com.huashi.eftransfer.app.modules.lexicon.imports.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.huashi.eftransfer.app.common.persistence.BaseAuditEntity;

@TableName("lexical_import_row")
public class LexicalImportRowEntity extends BaseAuditEntity {

    @TableField("batch_id")
    private Long batchId;

    @TableField("import_row_number")
    private Integer rowNumber;
    private String rowStatus;
    private String draftJson;

    @TableField("validation_errors_json")
    private String validationErrorsJson;

    @TableField("imported_lexical_pair_id")
    private Long importedLexicalPairId;

    private String importMessage;

    public Long getBatchId() {
        return batchId;
    }

    public void setBatchId(Long batchId) {
        this.batchId = batchId;
    }

    public Integer getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(Integer rowNumber) {
        this.rowNumber = rowNumber;
    }

    public String getRowStatus() {
        return rowStatus;
    }

    public void setRowStatus(String rowStatus) {
        this.rowStatus = rowStatus;
    }

    public String getDraftJson() {
        return draftJson;
    }

    public void setDraftJson(String draftJson) {
        this.draftJson = draftJson;
    }

    public String getValidationErrorsJson() {
        return validationErrorsJson;
    }

    public void setValidationErrorsJson(String validationErrorsJson) {
        this.validationErrorsJson = validationErrorsJson;
    }

    public Long getImportedLexicalPairId() {
        return importedLexicalPairId;
    }

    public void setImportedLexicalPairId(Long importedLexicalPairId) {
        this.importedLexicalPairId = importedLexicalPairId;
    }

    public String getImportMessage() {
        return importMessage;
    }

    public void setImportMessage(String importMessage) {
        this.importMessage = importMessage;
    }
}
