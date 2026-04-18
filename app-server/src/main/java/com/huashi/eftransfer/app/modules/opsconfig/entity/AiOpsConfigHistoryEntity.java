package com.huashi.eftransfer.app.modules.opsconfig.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.huashi.eftransfer.app.common.persistence.BaseAuditEntity;

@TableName("admin_ai_config_history")
public class AiOpsConfigHistoryEntity extends BaseAuditEntity {

    @TableField("config_key")
    private String configKey;

    @TableField("version_number")
    private Long versionNumber;

    @TableField("previous_version_number")
    private Long previousVersionNumber;

    @TableField("encrypted_config")
    private String encryptedConfig;

    @TableField("change_summary_json")
    private String changeSummaryJson;

    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    public Long getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(Long versionNumber) {
        this.versionNumber = versionNumber;
    }

    public Long getPreviousVersionNumber() {
        return previousVersionNumber;
    }

    public void setPreviousVersionNumber(Long previousVersionNumber) {
        this.previousVersionNumber = previousVersionNumber;
    }

    public String getEncryptedConfig() {
        return encryptedConfig;
    }

    public void setEncryptedConfig(String encryptedConfig) {
        this.encryptedConfig = encryptedConfig;
    }

    public String getChangeSummaryJson() {
        return changeSummaryJson;
    }

    public void setChangeSummaryJson(String changeSummaryJson) {
        this.changeSummaryJson = changeSummaryJson;
    }
}
