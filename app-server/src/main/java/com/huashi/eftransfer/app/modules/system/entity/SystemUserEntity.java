package com.huashi.eftransfer.app.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.huashi.eftransfer.shared.model.BaseAuditEntity;

@TableName("sys_user")
public class SystemUserEntity extends BaseAuditEntity {

    private String username;
    private String passwordHash;
    private String displayName;
    private String role;
    private Boolean enabled;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
