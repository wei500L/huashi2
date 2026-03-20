package com.huashi.eftransfer.app.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ops.config")
public class AiOpsConfigProperties {

    private String encryptionSecret = "";

    public String getEncryptionSecret() {
        return encryptionSecret;
    }

    public void setEncryptionSecret(String encryptionSecret) {
        this.encryptionSecret = encryptionSecret;
    }
}
