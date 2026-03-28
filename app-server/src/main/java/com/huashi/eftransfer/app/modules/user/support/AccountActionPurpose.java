package com.huashi.eftransfer.app.modules.user.support;

import java.util.Locale;

public enum AccountActionPurpose {
    INVITE_ACTIVATION,
    PASSWORD_RESET;

    public static AccountActionPurpose fromCode(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("purpose must not be blank");
        }
        return AccountActionPurpose.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
