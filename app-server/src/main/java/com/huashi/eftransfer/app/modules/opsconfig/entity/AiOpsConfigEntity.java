package com.huashi.eftransfer.app.modules.opsconfig.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.huashi.eftransfer.shared.model.BaseAuditEntity;

@TableName("admin_ai_config")
public class AiOpsConfigEntity extends BaseAuditEntity {

    private String configKey;

    @TableField("encrypted_config")
    private String encryptedConfig;

    @TableField("version_number")
    private Long versionNumber;

    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    public String getEncryptedConfig() {
        return encryptedConfig;
    }

    public void setEncryptedConfig(String encryptedConfig) {
        this.encryptedConfig = encryptedConfig;
    }

    public Long getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(Long versionNumber) {
        this.versionNumber = versionNumber;
    }
}
